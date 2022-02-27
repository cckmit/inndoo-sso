package com.ytdinfo.inndoo.config;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.JsccbApiSyncConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsccbApiSyncMQConfig implements IWxMQConfig {

    @Autowired
    private RabbitUtil rabbitUtil;

    @Bean
    FanoutExchange jsccbApiSyncEvenMsgExchanges() {
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_JSCCB_API_SYNC);
    }

    /**
     * 实际消费队列
     */
    @Bean
    public Queue jsccbApiSyncEventQueues() {
        JsccbApiSyncConsumer consumer = SpringContextUtil.getBean(JsccbApiSyncConsumer.class);
        return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_JSCCB_API_SYNC, consumer);
    }

    @Override
    public void dynamicInit(String prefix) {
    }
}
