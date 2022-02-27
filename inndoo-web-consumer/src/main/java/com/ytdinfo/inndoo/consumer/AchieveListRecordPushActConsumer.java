package com.ytdinfo.inndoo.consumer;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListRecord;
import com.ytdinfo.inndoo.modules.core.service.AchieveListRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class AchieveListRecordPushActConsumer extends BaseConsumer {
    @Autowired
    private AchieveListRecordService achieveListRecordService;

    @Override
    public void onMessage(MQMessage mqMessage) {
        AchieveListRecord u = JSONUtil.toBean((JSONObject) mqMessage.getContent(), AchieveListRecord.class);
        achieveListRecordService.achieveListRecordPushAct(u);
    }
}
