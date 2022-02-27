package com.ytdinfo.inndoo.common.rabbit.config;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.consumer.TenantConsumer;
import com.ytdinfo.inndoo.common.utils.ServerUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * 租户交换机申明
 *
 * @author yaochangning
 * @date 2019/8/22
 */
@Configuration
public class TenantMQConfig {
    @Autowired
    private RabbitUtil rabbitUtil;
    @Autowired
    private ServerUtil serverUtil;

    /**
     * 声明队列和交换机
     * @return
     */
    @Bean
    public FanoutExchange tenantEventMsgExchanges(){
        String prefix = serverUtil.getServerIPPort();
        FanoutExchange exchange = rabbitUtil.createFanoutExchange(StrUtil.EMPTY, prefix, QueueEnum.QUEUE_WXOPEN_EVENT_TENANT);
        return exchange;
    }

    /**
     * 绑定队列消费者
     */
    @Bean
    public Queue tenantEventQueues() throws SocketException, UnknownHostException {
        String prefix = serverUtil.getServerIPPort();
        TenantConsumer consumer = SpringContextUtil.getBean(TenantConsumer.class);
        return rabbitUtil.bindListener(prefix, QueueEnum.QUEUE_WXOPEN_EVENT_TENANT, consumer);
    }
}
