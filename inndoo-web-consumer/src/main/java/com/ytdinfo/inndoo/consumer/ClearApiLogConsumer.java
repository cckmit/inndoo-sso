package com.ytdinfo.inndoo.consumer;

import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.modules.core.service.ApiRequestLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class ClearApiLogConsumer extends BaseConsumer {
    @Autowired
    private ApiRequestLogService apiRequestLogService;

    @Override
    public void onMessage(MQMessage mqMessage) {
        int k = apiRequestLogService.clearAllApiRequestLog();
    }
}
