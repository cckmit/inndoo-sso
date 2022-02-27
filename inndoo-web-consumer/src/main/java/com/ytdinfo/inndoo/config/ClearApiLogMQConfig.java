package com.ytdinfo.inndoo.config;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.ClearApiLogConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClearApiLogMQConfig implements IWxMQConfig {
    @Autowired
    private RabbitUtil rabbitUtil;

    @Bean
    FanoutExchange sendClearApiExceptionLogExchanges() {
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_CLEAR_API_LOG_MSG);
    }

    @Bean
    public Queue sendClearApiExceptionLogQueues() {
        ClearApiLogConsumer consumer = SpringContextUtil.getBean(ClearApiLogConsumer.class);
        return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_CLEAR_API_LOG_MSG, consumer);
    }

    @Override
    public void dynamicInit(String prefix) {

    }
}
