package com.ytdinfo.inndoo.config;


import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.JsccbApiSyncOldDataConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JsccbApiSyncOldDataMQConfig implements IWxMQConfig {

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private MatrixApiUtil apiUtil;

    @Bean
    FanoutExchange jsccbApiSyncOldDataMsgExchanges() {
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_JSCCB_API_SYNC_OLDDATA_MSG);
    }

    /**
     * 绑定实际消费队列
     */
    @Bean
    public Queue jsccbApiSyncOldDataEventQueues() {
        JsccbApiSyncOldDataConsumer consumer = SpringContextUtil.getBean(JsccbApiSyncOldDataConsumer.class);
        return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_JSCCB_API_SYNC_OLDDATA_MSG, consumer);
    }

    @Override
    public void dynamicInit(String prefix) {
    }
}
