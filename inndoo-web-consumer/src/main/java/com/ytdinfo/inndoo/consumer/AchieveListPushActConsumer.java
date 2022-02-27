package com.ytdinfo.inndoo.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.entity.ExceptionLog;
import com.ytdinfo.inndoo.modules.core.service.AchieveListRecordService;
import com.ytdinfo.inndoo.modules.core.service.ExceptionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Scope("prototype")
public class AchieveListPushActConsumer extends BaseConsumer {
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private ExceptionLogService exceptionLogService;
    @Override
    public void onMessage(MQMessage mqMessage) {
        AchieveList u = JSONUtil.toBean((JSONObject) mqMessage.getContent(), AchieveList.class);
        achieveListRecordService.achieveListPushAct(u);
    }
}
