package com.ytdinfo.inndoo.common.rabbit.consumer;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.ThreadPoolUtil;
import com.ytdinfo.inndoo.modules.core.entity.MqException;
import com.ytdinfo.inndoo.modules.core.mqutil.MqExceptionUtil;
import com.ytdinfo.inndoo.modules.core.service.MqExceptionService;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author timmy
 */
public abstract class BaseThreadPoolConsumer extends BaseConsumer {

    @Autowired
    private RabbitUtil rabbitUtil;
    @Autowired
    private MqExceptionService mqExceptionService;
    @Autowired
    private MqExceptionUtil mqExceptionUtil;

    private Object lock = new Object();

    @Override
    public void onMessage(Message message, Channel channel) {
        ThreadPoolUtil.getPool().execute(() -> {
            try {
                MQMessage mqMessage = prepare(message);
                onMessage(mqMessage);
                synchronized (lock){
                    rabbitUtil.ack(message, channel);
                }
            } catch (Exception ex) {
                synchronized (lock){
                    rabbitUtil.reject(message, channel);
                }
                String error = ExceptionUtil.stacktraceToString(ex);
                String exception = ex.getMessage();
                String body = JSONUtil.toJsonStr(message);
                String queueName = message.getMessageProperties().getConsumerQueue();
                MqException mqException = new MqException();
                if (StrUtil.isEmpty(mqException.getAppid())) {
                    mqException.setAppid("");
                }
                mqException.setMsgBody(body);
                mqException.setQueueName(queueName);
                mqException.setException(exception+ "\n"+ error);
                mqException.setCreateTime(new Date());
                mqException.setUpdateTime(new Date());
                mqExceptionService.save(mqException);
                //向matrix项目发送MQ信息
                mqExceptionUtil.sendMessage(mqException);
            }
            finally {
                super.dispose();
            }
        });


    }
}
