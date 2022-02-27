package com.ytdinfo.inndoo.common.rabbit.consumer;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.modules.core.entity.MqException;
import com.ytdinfo.inndoo.modules.core.mqutil.MqExceptionUtil;
import com.ytdinfo.inndoo.modules.core.service.MqExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class BaseConsumer implements ChannelAwareMessageListener {

    @Autowired
    private RabbitUtil rabbitUtil;
    @Autowired
    private MqExceptionService mqExceptionService;
    @Autowired
    private MqExceptionUtil mqExceptionUtil;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    private String tenantId;


    protected MQMessage prepare(Message message){
        MQMessage mqMessage = rabbitUtil.parseMessage(message);
        String tenantIdFromMq = mqMessage.getTenantId();
        if(StrUtil.isNotEmpty(tenantIdFromMq)) {
            setTenantId(tenantIdFromMq);
        }
        DynamicDataSourceContextHolder.setDataSourceType(this.tenantId);
        UserContext.setTenantId(this.tenantId);
        UserContext.setWxAppId(mqMessage.getAppid());
        return mqMessage;
    }

    @Override
    public void onMessage(Message message, Channel channel){
        try {
            MQMessage mqMessage = prepare(message);
            onMessage(mqMessage);
            rabbitUtil.ack(message, channel);
        }catch (Exception ex){
            try {
                String error = ExceptionUtil.stacktraceToString(ex);
                String exception = ex.getMessage();
                String bodyContent = new String(message.getBody(), CharsetUtil.CHARSET_UTF_8);
                String body = JSONUtil.toJsonStr(message) + "\n" + bodyContent;
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
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
            rabbitUtil.reject(message, channel);
        }finally {
//            try {
//                channel.close();
//            } catch (IOException e) {
//                log.error(e.toString());
//            } catch (TimeoutException e) {
//                log.error(e.toString());
//            }
            dispose();
        }

    }
    protected void dispose() {
        UserContext.remove();
    }

    public abstract void onMessage(MQMessage mqMessage);
}
