package com.ytdinfo.inndoo.modules.core.mqutil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.vo.ExceptionLogVo;
import com.ytdinfo.inndoo.modules.core.entity.ExceptionLog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExceptionLogUtil {
    private static final String EXCEPTIONLOG_ACTION_NAME = "saveException";

    @Autowired
    public RabbitUtil rabbitUtil;

    public void sendMessage(ExceptionLog Exception){
        //修改统计信息
        MQMessage<ExceptionLogVo> mqMessage = new MQMessage<ExceptionLogVo>();
        ExceptionLogVo ExceptionVo = new ExceptionLogVo();
        BeanUtils.copyProperties(Exception,ExceptionVo);
        //设置项目名
        ExceptionVo.setProjectName("core");
        //设置租户id
        ExceptionVo.setTenantId(UserContext.getTenantId());
        mqMessage.setContent(ExceptionVo);
        rabbitUtil.sendToExchange(QueueEnum.QUEUE_EXCEPTIONLOG_EVENT_MSG.getExchange() , "", mqMessage, EXCEPTIONLOG_ACTION_NAME);
    }
}
