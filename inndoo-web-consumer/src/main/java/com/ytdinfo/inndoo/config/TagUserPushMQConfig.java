package com.ytdinfo.inndoo.config;


import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.consumer.AchieveListPushActConsumer;
import com.ytdinfo.inndoo.consumer.TagUserPushConsumer;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 达标用户导入后推送到act用户
 *
 * @author yaochangning
 * @date 2019/8/9
 */
@Configuration
public class TagUserPushMQConfig implements IWxMQConfig {


    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private MatrixApiUtil apiUtil;

    @Bean
    FanoutExchange tagUserPushMsgExchanges() {

//        List<FanoutExchange> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
        return rabbitUtil.createFanoutExchange(StrUtil.EMPTY, StrUtil.EMPTY, QueueEnum.QUEUE_PUSH_TAG_MSG);
//            list.add(rabbitUtil.createFanoutExchange(appid, appid, QueueEnum.QUEUE_ACHIEVELISTRECORD_PUSHACT_MSG));
//        }
//        FanoutExchange[] fanoutExchanges = new FanoutExchange[list.size()];
//        return list.toArray(fanoutExchanges);
    }

    /**
     * 绑定实际消费队列
     */
    @Bean
    public Queue tagUserPushEventQueues() {
//        List<Queue> list = new ArrayList<>();
//        List<WxAuthorizer> wxAuthorizers = apiUtil.getWxAuthorizerList();
//        for (int i = 0; i < wxAuthorizers.size(); i++) {
//            String appid = wxAuthorizers.get(i).getAppid();
        TagUserPushConsumer consumer = SpringContextUtil.getBean(TagUserPushConsumer.class);
            return  rabbitUtil.bindListener(StrUtil.EMPTY, QueueEnum.QUEUE_PUSH_TAG_MSG, consumer);
//            list.add(queue);
//        }
//        Queue[] queues = new Queue[list.size()];
//        list.toArray(queues);
//        return queues;
    }


    @Override
    public void dynamicInit(String prefix) {
//        rabbitUtil.createFanoutExchange(prefix, prefix, QueueEnum.QUEUE_ACHIEVELISTRECORD_PUSHACT_MSG);
//        AchieveListPushActConsumer consumer = SpringContextUtil.getBean(AchieveListPushActConsumer.class);
//        rabbitUtil.bindListener(prefix, QueueEnum.QUEUE_ACHIEVELISTRECORD_PUSHACT_MSG, consumer, true);
    }
}
