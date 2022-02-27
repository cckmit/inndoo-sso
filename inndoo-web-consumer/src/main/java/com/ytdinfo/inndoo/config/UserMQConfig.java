package com.ytdinfo.inndoo.config;


import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.ITenantMQConfig;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.common.vo.Tenant;
import com.ytdinfo.inndoo.consumer.UserConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 用戶操作队列说明
 *
 * @author yaochangning
 * @date 2019/8/9
 */
@Configuration
public class UserMQConfig implements ITenantMQConfig {


    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private MatrixApiUtil apiUtil;

    @Bean
    FanoutExchange userEventMsgExchange() {
//        List<Tenant> tenantList = apiUtil.getTenantList();
//        List<FanoutExchange> list = new ArrayList<>();
//        for (int i = 0; i < tenantList.size(); i++) {
//            String tenantId = tenantList.get(i).getId();
//            list.add(rabbitUtil.createFanoutExchange(StrUtil.EMPTY, tenantId, QueueEnum.QUEUE_USER_EVENT_MSG));
//        }
//        FanoutExchange[] fanoutExchanges = new FanoutExchange[list.size()];
//        return list.toArray(fanoutExchanges);
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_USER_EVENT_MSG);
    }

    /**
     * 绑定队列消费者
     */
    @Bean
    public Queue userEventQueue() {
//        List<Queue> list = new ArrayList<>();
//        List<Tenant> tenantList = apiUtil.getTenantList();
//        for (int i = 0; i < tenantList.size(); i++) {
//            String tenantId = tenantList.get(i).getId();
//            UserConsumer consumer = SpringContextUtil.getBean(UserConsumer.class);
//            consumer.setTenantId(tenantId);
//            rabbitUtil.bindListener(tenantId, QueueEnum.QUEUE_USER_EVENT_MSG, consumer);
//        }
//        Queue[] queues = new Queue[list.size()];
//        list.toArray(queues);
//        return queues;
        UserConsumer consumer = SpringContextUtil.getBean(UserConsumer.class);
        Queue queue = rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_USER_EVENT_MSG, consumer);
        return queue;
    }

    @Override
    public void dynamicInit(String prefix) {
//        rabbitUtil.createFanoutExchange(StrUtil.EMPTY, prefix, QueueEnum.QUEUE_USER_EVENT_MSG);
//        UserConsumer consumer = SpringContextUtil.getBean(UserConsumer.class);
//        consumer.setTenantId(prefix);
//        rabbitUtil.bindListener(prefix, QueueEnum.QUEUE_USER_EVENT_MSG, consumer, true);
    }
}
