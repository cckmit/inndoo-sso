package com.ytdinfo.inndoo.config;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.PhoneLocationConsumer;
import com.ytdinfo.inndoo.modules.base.entity.WxAuthorizer;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author timmy
 * @date 2019/10/17
 */
@Configuration
public class PhoneLocationMQConfig implements IWxMQConfig {
    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private MatrixApiUtil apiUtil;

    /**
     * 绑定实际消费队列
     */
    @Bean
    public Queue phoneLocationQueues() {
//        List<Queue> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
            PhoneLocationConsumer consumer = SpringContextUtil.getBean(PhoneLocationConsumer.class);
            rabbitUtil.createQueue(StrUtil.EMPTY, QueueEnum.QUEUE_PHONE_LOCATION_EVENT_MSG);
            return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_PHONE_LOCATION_EVENT_MSG, consumer);
//            list.add(queue);
//        }
//        Queue[] queues = new Queue[list.size()];
//        list.toArray(queues);
//        return queues;
    }

    @Override
    public void dynamicInit(String prefix) {
//        PhoneLocationConsumer consumer = SpringContextUtil.getBean(PhoneLocationConsumer.class);
//        rabbitUtil.createQueue(prefix, QueueEnum.QUEUE_PHONE_LOCATION_EVENT_MSG);
//        rabbitUtil.bindListener(prefix, QueueEnum.QUEUE_PHONE_LOCATION_EVENT_MSG, consumer, true);
    }
}