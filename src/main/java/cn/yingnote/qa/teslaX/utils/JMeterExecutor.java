package cn.yingnote.qa.teslaX.utils;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
public class JMeterExecutor {

    private final static Logger logger = LoggerFactory.getLogger(JMeterExecutor.class);

    @Value("${jmeter.properties}")
    private String JMETER_PROPERTIES;
    @Value("${jmeter.home}")
    private String JMETER_HOME;
    @Value("${jmeter.log}")
    private String JMETER_LOG;
    @Value("${jmeter.thread-number}")
    private int JMETER_THREAD_NUM;
    @Value("${jmeter.duration}")
    private int JMETER_DURATION;

    public String jmeterRun(URL urlParse) {
        //JMeter Engine
        StandardJMeterEngine jmeter = new StandardJMeterEngine();
        // 本地 JMeter 的配置文件地址 JMeter initialization (properties, log levels, locale, etc)
        JMeterUtils.loadJMeterProperties(JMETER_PROPERTIES);
        // 本地 JMeter 目录
        JMeterUtils.setJMeterHome(JMETER_HOME);
        JMeterUtils.initLocale();
        // JMeter 报告的地址
        String jtlLogPath = JMETER_LOG;

        // 支持实时压测监控
        /*BackendListener backendListener = new BackendListener();
        backendListener.setClassname("org.apache.jmeter.visualizers.backend.graphite.GraphiteBackendListenerClient");
        Arguments args = new Arguments();
        args.addArgument("graphiteMetricsSender", "org.apache.jmeter.visualizers.backend.graphite.TextGraphiteMetricsSender");
        // InfluxDB 服务器地址
        args.addArgument("graphiteHost", "192.168.x.x");
        args.addArgument("graphitePort", "2003");
        args.addArgument("rootMetricsPrefix", "jmeter.");
        args.addArgument("summaryOnly", "false");
        args.addArgument("samplersList", ".*");
        args.addArgument("percentiles", "90;95;99");
        args.addArgument("useRegexpForSamplersList", "true");
        backendListener.setArguments(args);*/

        // Result collector
        ResultCollector resultCollector = new ResultCollector();
        resultCollector.setFilename(jtlLogPath);

        SampleSaveConfiguration saveConfiguration = new SampleSaveConfiguration();
        // 设置压测结果为 xml 格式，csv 的话就注释掉
        saveConfiguration.setAsXml(true);
        saveConfiguration.setCode(true);
        saveConfiguration.setLatency(true);
        resultCollector.setSaveConfig(saveConfiguration);

        // HTTP Sampler
        HTTPSampler httpSampler = new HTTPSampler();
        httpSampler.setDomain(urlParse.getHost());
        httpSampler.setPort(urlParse.getPort());
        httpSampler.setPath(urlParse.getPath());

        // Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        // 线程数
        threadGroup.setNumThreads(JMETER_THREAD_NUM);
        threadGroup.setRampUp(1);
        // 压测时间
        threadGroup.setDuration((long) JMETER_DURATION);
        threadGroup.setScheduler(true);
        threadGroup.setName("Group1");

        // Loop Controller
        LoopController loopController = new LoopController();
        // 循环次数设置为 -1，才可以通过 setDuration 来指定运行时间
        loopController.setLoops(-1);
        threadGroup.setSamplerController(loopController);
        HashTree threadGroupTree = new HashTree();
        threadGroupTree.add(httpSampler);

        // Test Plan
        TestPlan testPlan = new TestPlan("Create JMeter Script From Java Code");

        // JMeter Test Plan, basic all u JOrphan HashTree
        HashTree testPlanTree = new HashTree();
        // Construct Test Plan from previously initialized elements
        testPlanTree.add(threadGroup, threadGroupTree);
        testPlanTree.add(resultCollector);
        // 添加 backendListener，支持压测实时监控
//        testPlanTree.add(backendListener);
        HashTree hashTree = new HashTree();
        hashTree.add(testPlan, testPlanTree);

        // Run Test Plan
        jmeter.configure(hashTree);

        logger.debug("JMeter 压测开始...");
        jmeter.run();
        logger.debug("JMeter 压测结束...");

        return jtlLogPath;
    }
}
