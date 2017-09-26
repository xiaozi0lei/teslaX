package cn.yingnote.qa.teslaX.controller;

import cn.yingnote.qa.teslaX.service.Impl.PerformanceServiceImpl;
import cn.yingnote.qa.teslaX.service.PerformanceService;
import cn.yingnote.qa.teslaX.utils.RabbitMQProducer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static io.restassured.RestAssured.get;

@Controller
@RequestMapping("/performance")
public class PerformanceController {

    private PerformanceService performanceService;
    private RabbitMQProducer rabbitMQProducer;

    public PerformanceController(PerformanceServiceImpl performanceService, RabbitMQProducer rabbitMQProducer) {
        this.performanceService = performanceService;
        this.rabbitMQProducer = rabbitMQProducer;
    }

    @PostMapping("/test")
    public String test(@RequestParam("address") String address, Model model) {
        if (!address.startsWith("http")) {
            address = "http://" + address;
        }
        // 1. 先验证测试网址有效
        get(address).then().assertThat().statusCode(200);

        // 2. 调用 Service JMeter Executor 执行压测
        String logFile = performanceService.test(address);

        // 3. 调用 Service JMeter Summary 压测结果分析
        String result = performanceService.analyze(logFile);

        model.addAttribute("result", result);

        // 4. 返回结果页面
        return "performance/result";
    }

    @PostMapping("/test1")
    public String test1(@RequestParam("address1") String address1, Model model) {
        if (!address1.startsWith("http")) {
            address1 = "http://" + address1;
        }
        // 1. 先验证测试网址有效
        get(address1).then().assertThat().statusCode(200);

        // 2. 调用 rabbitMQProducer 生产
        rabbitMQProducer.send(address1);

        model.addAttribute("result", "请查看 Console 窗口");

        // 3. 返回结果页面
        return "performance/result";
    }
}
