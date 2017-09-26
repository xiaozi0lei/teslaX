package cn.yingnote.qa.teslaX.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQProducer {
    private final static Logger logger = LoggerFactory.getLogger(RabbitMQProducer.class);
    private final AmqpTemplate rabbitTemplate;

    @Autowired
    public RabbitMQProducer(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String url) {
        logger.info("生产的 String 为：{}", url);
        rabbitTemplate.convertAndSend("QA_Platform_Exchange", "QA_Platform_Route", url);
    }
}
