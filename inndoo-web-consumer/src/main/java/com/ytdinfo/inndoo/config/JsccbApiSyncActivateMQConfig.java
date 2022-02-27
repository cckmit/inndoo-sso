package com.ytdinfo.inndoo.config;


import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.JsccbApiSyncActivateConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JsccbApiSyncActivateMQConfig implements IWxMQConfig {

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private MatrixApiUtil apiUtil;

    @Bean
    FanoutExchange jsccbApiSyncActivateMsgExchanges() {
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_JSCCB_API_SYNC_ACTIVATE_MSG);
    }

    /**
     * 绑定实际消费队列
     */
    @Bean
    public Queue jsccbApiSyncActivateEventQueues() {
        JsccbApiSyncActivateConsumer consumer = SpringContextUtil.getBean(JsccbApiSyncActivateConsumer.class);
        return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_JSCCB_API_SYNC_ACTIVATE_MSG, consumer);
    }

    @Override
    public void dynamicInit(String prefix) {
    }
}
