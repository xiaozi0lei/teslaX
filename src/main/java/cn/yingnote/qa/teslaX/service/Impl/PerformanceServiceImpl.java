package cn.yingnote.qa.teslaX.service.Impl;

import cn.yingnote.qa.teslaX.service.PerformanceService;
import cn.yingnote.qa.teslaX.utils.JMeterExecutor;
import cn.yingnote.qa.teslaX.utils.JMeterSummary;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;

@Service
public class PerformanceServiceImpl implements PerformanceService {

    private JMeterExecutor jMeterExecutor;

    public PerformanceServiceImpl(JMeterExecutor jMeterExecutor) {
        this.jMeterExecutor = jMeterExecutor;
    }

    @Override
    public String test(String address) {
        // 分析 URL 获取 host, port, path
        URL urlParse = null;
        try {
            urlParse = new URL(address);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // 调用 JMeter API 执行性能测试
        return jMeterExecutor.jmeterRun(urlParse);
    }

    @Override
    public String analyze(String logFile) {
        JMeterSummary jMeterSummary = new JMeterSummary();
        return jMeterSummary.runJMeterSummary(new String[]{logFile});
    }
}
