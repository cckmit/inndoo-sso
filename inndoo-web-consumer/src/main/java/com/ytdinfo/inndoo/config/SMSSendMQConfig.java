package com.ytdinfo.inndoo.config;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.SMSSendConsumer;
import com.ytdinfo.inndoo.modules.base.entity.WxAuthorizer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SMSSendMQConfig implements IWxMQConfig {

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private MatrixApiUtil apiUtil;

    @Bean
    FanoutExchange sendSMSEventMsgExchanges() {
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        List<FanoutExchange> list = new ArrayList<>();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
            return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_SEND_SMS_EVENT_MSG);
//            list.add(fanoutExchange);
//        }
//        FanoutExchange[] fanoutExchanges = new FanoutExchange[list.size()];
//        return list.toArray(fanoutExchanges);
    }

    /**
     * 绑定实际消费队列
     */
    @Bean
    public Queue sendSMSEventQueues() {
//        List<Queue> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
            SMSSendConsumer consumer = SpringContextUtil.getBean(SMSSendConsumer.class);
            return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_SEND_SMS_EVENT_MSG, consumer);
//            list.add(queue);
//        }
//        Queue[] queues = new Queue[list.size()];
//        list.toArray(queues);
//        return queues;
    }

    @Override
    public void dynamicInit(String prefix) {
//        rabbitUtil.createFanoutExchange(prefix, prefix, QueueEnum.QUEUE_SEND_SMS_EVENT_MSG);
//        SMSSendConsumer consumer = SpringContextUtil.getBean(SMSSendConsumer.class);
//        rabbitUtil.bindListener(prefix, QueueEnum.QUEUE_SEND_SMS_EVENT_MSG, consumer, true);
    }
}
