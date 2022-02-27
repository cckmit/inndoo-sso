package com.ytdinfo.inndoo.common.rabbit;

import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 *
 * @author timmy
 * @date 2019/8/2
 */
@Data
public class MQMessage<T> {
    private String tenantId;
    private String appid;
    private String componentAppid;
    private T content;
    private Message message;
    public MessageProperties getMessageProperties(){
        if(message != null){
            return message.getMessageProperties();
        }
        return null;
    }
}