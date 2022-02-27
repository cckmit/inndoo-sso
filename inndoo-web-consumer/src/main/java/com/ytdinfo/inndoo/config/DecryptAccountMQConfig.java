package com.ytdinfo.inndoo.config;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.AccountInputConsumer;
import com.ytdinfo.inndoo.consumer.DecryptAccountConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 解密账户信息
 */
@Configuration
public class DecryptAccountMQConfig implements IWxMQConfig {

    @Autowired
    private RabbitUtil rabbitUtil;

    @Bean
    FanoutExchange decryptAccountMsgExchanges() {

//        List<FanoutExchange> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
//            list.add(rabbitUtil.createFanoutExchange(appid, appid, QueueEnum.QUEUE_ACCOUNT_INPUT_MSG));
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_DECRYPTACCOUNT_MSG);
//        }
//        FanoutExchange[] fanoutExchanges = new FanoutExchange[list.size()];
//        return list.toArray(fanoutExchanges);
    }

    /**
     * 绑定实际消费队列
     */
    @Bean
    public Queue decryptAccountEventQueues() {
//        List<Queue> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
        DecryptAccountConsumer consumer = SpringContextUtil.getBean(DecryptAccountConsumer.class);
        return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_DECRYPTACCOUNT_MSG, consumer);
//            list.add(queue);
//        }
//        Queue[] queues = new Queue[list.size()];
//        list.toArray(queues);
//        return queues;
    }
    @Override
    public void dynamicInit(String prefix) {

    }
}
