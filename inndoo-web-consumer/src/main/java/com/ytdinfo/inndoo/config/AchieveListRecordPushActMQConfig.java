package com.ytdinfo.inndoo.config;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.AchieveListPushActConsumer;
import com.ytdinfo.inndoo.consumer.AchieveListRecordPushActConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 达标用户导入后推送到act用户
 *
 * @author yaochangning
 * @date 2020/9/16
 */
@Configuration
public class AchieveListRecordPushActMQConfig implements IWxMQConfig {
    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private MatrixApiUtil apiUtil;

    @Bean
    FanoutExchange achieveListRecordPushActMsgExchanges() {

//        List<FanoutExchange> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG);
//            list.add(rabbitUtil.createFanoutExchange(appid, appid, QueueEnum.QUEUE_ACHIEVELISTRECORD_PUSHACT_MSG));
//        }
//        FanoutExchange[] fanoutExchanges = new FanoutExchange[list.size()];
//        return list.toArray(fanoutExchanges);
    }

    /**
     * 绑定实际消费队列
     */
    @Bean
    public Queue achieveListRecordPushActEventQueues() {
//        List<Queue> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();

        AchieveListRecordPushActConsumer consumer = SpringContextUtil.getBean(AchieveListRecordPushActConsumer.class);
       return rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG, consumer);
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
