package cn.yingnote.qa.teslaX.utils;

import cn.yingnote.qa.teslaX.service.Impl.PerformanceServiceImpl;
import cn.yingnote.qa.teslaX.service.PerformanceService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

@Component
@RabbitListener(queues = "QA_Platform", containerFactory = "rabbitListenerContainerFactory")
public class RabbitMQConsumer implements ChannelAwareMessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

    private PerformanceService performanceService;

    RabbitMQConsumer(PerformanceServiceImpl performanceService) {
        this.performanceService = performanceService;
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        logger.debug("Receiver: {}", message.toString());

        // 设置 mq 一次只从队列中取一个 String
        channel.basicQos(1);
        // 按照编码将 bytes 数组转为 String
        String url = new String(message.getBody(), Charset.defaultCharset().name());
        logger.info("接收到的 String 为：{}", url);
        // 执行压测
        String logFile = performanceService.test(url);
        // 分析日志，查看 console 输出
        performanceService.analyze(logFile);
    }
}
