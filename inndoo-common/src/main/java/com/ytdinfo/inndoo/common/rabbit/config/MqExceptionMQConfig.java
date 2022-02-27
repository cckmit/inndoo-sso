package com.ytdinfo.inndoo.common.rabbit.config;

import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ错误交换机申明
 * @author yaochangning
 * @Data 2019/11/8
 */
@Configuration
public class MqExceptionMQConfig {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Bean
    FanoutExchange tenantExchanges() {
        //创建交换机
        FanoutExchange exchange = (FanoutExchange) ExchangeBuilder.fanoutExchange(QueueEnum.QUEUE_MQEXCEPTION_EVENT_MSG.getExchange()).durable(true).build();
        rabbitAdmin.declareExchange(exchange);
        return exchange;
    }

}
