package com.ytdinfo.inndoo.modules.core.mqutil;

import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;

import com.ytdinfo.inndoo.common.vo.MqExceptionVo;
import com.ytdinfo.inndoo.modules.core.entity.MqException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MqExceptionUtil {
    private static final String MQEXCEPTION_ACTION_NAME = "save";

    @Autowired
    public RabbitUtil rabbitUtil;

    public void sendMessage(MqException mqException){
        //修改统计信息
        MQMessage<MqExceptionVo> mqMessage = new MQMessage<MqExceptionVo>();
        MqExceptionVo mqExceptionVo = new MqExceptionVo();
        BeanUtils.copyProperties(mqException,mqExceptionVo);
        //设置项目名
        mqExceptionVo.setProjectName("core");
        //设置租户id
        mqExceptionVo.setTenantId(UserContext.getTenantId());
        mqMessage.setContent(mqExceptionVo);
        rabbitUtil.sendToExchange(QueueEnum.QUEUE_MQEXCEPTION_EVENT_MSG.getExchange() , "", mqMessage, MQEXCEPTION_ACTION_NAME);
    }
}
