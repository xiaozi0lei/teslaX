package cn.yingnote.qa.teslaX.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段含义如下：
 * cnt：number of requests
 * avg t：average time per request
 * max t：longest request
 * min t：shortest request
 * result codes：http result code and number of times received
 * failures：number of failures
 * cnt by time：a break down of how many requests returned in the specified time range
 * avg conn：average time doing connection overhead (time ms - latency ms = conn ms)
 * max conn：max connection overhead
 * min conn：min connection overhead
 * elapsed seconds：total elapsed time of test (last time stamp - first time stamp)
 * cnt per second：throughput (number of requests / elapsed seconds)
 */
public class JMeterSummary {
    private final static Logger logger = LoggerFactory.getLogger(JMeterSummary.class);

    private static final String REG_EX =
            "<httpSample\\s*" + // Start element
                    "t=\"([^\"]*)\"\\s*" + // GROUP_T
                    "it=\"([^\"]*)\"\\s*" + // GROUP_IT
                    "lt=\"([^\"]*)\"\\s*" + // GROUP_LT
                    "ct=\"([^\"]*)\"\\s*" + // GROUP_CT
                    "ts=\"([^\"]*)\"\\s*" + // GROUP_TS
                    "s=\"([^\"]*)\"\\s*" + // GROUP_S
                    "lb=\"([^\"]*)\"\\s*" + // GROUP_LB
                    "rc=\"([^\"]*)\"\\s*" + // GROUP_RC
                    "rm=\"([^\"]*)\"\\s*" + // GROUP_RM
                    "tn=\"([^\"]*)\"\\s*" + // GROUP_TN
                    "dt=\"([^\"]*)\"\\s*" + // GROUP_DT
                    "by=\"([^\"]*)\"\\s*" + // GROUP_BY
                    "sby=\"([^\"]*)\"\\s*" + // GROUP_SBY
                    "ng=\"([^\"]*)\"\\s*" + // GROUP_NG
                    "na=\"([^\"]*)\"\\s*" + // GROUP_NA
                    "/>"; // Finish element

    private static final int GROUP_ALL = 0;
    private static final int GROUP_T = 1;
    private static final int GROUP_IT = 2;
    private static final int GROUP_LT = 3;
    private static final int GROUP_CT = 4;
    private static final int GROUP_TS = 5;
    private static final int GROUP_S = 6;
    private static final int GROUP_LB = 7;
    private static final int GROUP_RC = 8;
    private static final int GROUP_RM = 9;
    private static final int GROUP_TN = 10;
    private static final int GROUP_DT = 11;
    private static final int GROUP_BY = 12;
    private static final int GROUP_SBY = 13;
    private static final int GROUP_NG = 14;
    private static final int GROUP_NA = 15;

    private static final int DEFAULT_MILLIS_BUCKET = 500;
    private static File _jmeterOutput = null;
    private static int _millisPerBucket;

    /**
     * 执行入口，参数为jtl文件的数组
     */
    public String runJMeterSummary(String args[]) {
        try {
            int millisPerBucket;
            int argIndex = 0;

            if (args.length < 1) {
                printUsage();
                throw new IllegalArgumentException("Must provide a JMeter output file as an argument.");
            }

            String arg0 = args[argIndex++];
            if (arg0.contains("help")) {
                printUsage();
                return null;
            }

            File outputFile = new File(arg0);
            if (!outputFile.exists()) {
                throw new FileNotFoundException("File '" + outputFile + "' does not exist.");
            }

            if (args.length > argIndex) {
                millisPerBucket = Integer.parseInt(args[argIndex++]);
            } else {
                millisPerBucket = DEFAULT_MILLIS_BUCKET;
            }

            _millisPerBucket = millisPerBucket;
            _jmeterOutput = outputFile;

            return this.run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    // end [main(String[])]

    private static void printUsage() {
        logger.debug("Usage: " + JMeterSummary.class.getName() + " <JMeter Ouput File> [Millis Per Bucket]");
        logger.debug("  (By default hits are grouped in " + DEFAULT_MILLIS_BUCKET + " millis/bucket.)");
    }

    private String run() throws IOException {
        Totals totalAll = new Totals();
        // key = url, value = total
        Map<String, Totals> totalUrlMap = new HashMap<>();
        Pattern p = Pattern.compile(REG_EX);

        BufferedReader inStream = new BufferedReader(new FileReader(_jmeterOutput));
        try {
            String line = inStream.readLine();
            while (line != null) {
                Matcher m = p.matcher(line);

                if (m.find()) {
                    add(m, totalAll);

                    String url = m.group(GROUP_LB);
                    Totals urlTotals = totalUrlMap.get(url);
                    if (urlTotals == null) {
                        urlTotals = new Totals();
                        totalUrlMap.put(url, urlTotals);
                    }
                    add(m, urlTotals);
                }
                line = inStream.readLine();
            }
        } finally {
            inStream.close();
        }

        if (totalAll.count == 0) {
            logger.debug("No results found!");
            return null;
        }

        String basicString = totalAll.toBasicString();
        String advancedString = totalAll.toAdvancedString();
        logger.debug("All Urls:");
        logger.debug(basicString);
        logger.debug(advancedString);

        String result = "All Urls:" + basicString + advancedString;

        return result;
    }
    // end [run()]

    private void add(Matcher inM, Totals inTotal) {
        inTotal.count++;
        long timeStamp = Long.parseLong(inM.group(GROUP_TS));
        inTotal.last_ts = Math.max(inTotal.last_ts, timeStamp);
        inTotal.first_ts = Math.min(inTotal.first_ts, timeStamp);

        int time = Integer.parseInt(inM.group(GROUP_T));
        inTotal.total_t += time;
        inTotal.max_t = Math.max(inTotal.max_t, time);
        inTotal.min_t = Math.min(inTotal.min_t, time);

        int conn = time - Integer.parseInt(inM.group(GROUP_LT));
        inTotal.total_conn += conn;
        inTotal.max_conn = Math.max(inTotal.max_conn, conn);
        inTotal.min_conn = Math.min(inTotal.min_conn, conn);

        String rc = inM.group(GROUP_RC);
        Integer count = inTotal.rcMap.get(rc);
        if (count == null) {
            count = 0;
        }
        inTotal.rcMap.put(rc, count + 1);

        Integer bucket = time / _millisPerBucket;
        count = inTotal.millisMap.get(bucket);
        if (count == null) {
            count = 0;
        }
        inTotal.millisMap.put(bucket, count + 1);

        if (!inM.group(GROUP_S).equalsIgnoreCase("true")) {
            inTotal.failures++;
        }
    }
    // end [add(Matcher, Totals)]

    /**
     * 统计功能
     */
    private class Totals {
        private static final String DECIMAL_PATTERN = "#,##0.0##";
        private static final double MILLIS_PER_SECOND = 1000.0;

        int count = 0;
        private int total_t = 0;
        // will choose largest
        int max_t = 0;
        // will choose smallest
        int min_t = Integer.MAX_VALUE;
        int total_conn = 0;
        // will choose largest
        int max_conn = 0;
        // will choose smallest
        int min_conn = Integer.MAX_VALUE;
        private int failures = 0;
        // will choose smallest
        long first_ts = Long.MAX_VALUE;
        // will choose largest
        long last_ts = 0;
        // key rc, value count
        Map<String, Integer> rcMap = new HashMap<>();
        // key bucket Integer, value count
        Map<Integer, Integer> millisMap = new TreeMap<>();

        Totals() {
        }

        String toBasicString() {

            DecimalFormat df = new DecimalFormat(DECIMAL_PATTERN);
            List<String> millisStr = new LinkedList<>();
            Iterator iter = millisMap.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry millisEntry = (Map.Entry) iter.next();
                Integer bucket = (Integer) millisEntry.getKey();
                Integer bucketCount = (Integer) millisEntry.getValue();

                int minMillis = bucket * _millisPerBucket;
                int maxMillis = (bucket + 1) * _millisPerBucket;

                millisStr.add(
                        df.format(minMillis / MILLIS_PER_SECOND) + " s " +
                                "- " +
                                df.format(maxMillis / MILLIS_PER_SECOND) + " s " +
                                "= " + bucketCount);
            }

            double secondsElaspsed = (last_ts - first_ts) / MILLIS_PER_SECOND;
            long tps = Math.round(count / secondsElaspsed);
            int errorRate = Math.round(failures / count);

            return
                    "cnt: " + count + ", " +
                            "avg t: " + (total_t / count) + " ms, " +
                            "max t: " + max_t + " ms, " +
                            "min t: " + min_t + " ms, " +
                            "result codes: " + rcMap + ", " +
                            "failures: " + failures + ", " +
                            "cnt by time: " + millisStr + "";
        }

        String toAdvancedString() {
            double secondsElaspsed = (last_ts - first_ts) / MILLIS_PER_SECOND;
            long countPerSecond = Math.round(count / secondsElaspsed);

            return
                    "avg conn: " + (total_conn / count) + " ms, " +
                            "max conn: " + max_conn + " ms, " +
                            "min conn: " + min_conn + " ms, " +
                            "elapsed seconds: " + Math.round(secondsElaspsed) + " s, " +
                            "cnt per second: " + countPerSecond;
        }
    }
}
