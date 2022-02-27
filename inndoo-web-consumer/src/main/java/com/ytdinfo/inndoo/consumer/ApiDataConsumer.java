package com.ytdinfo.inndoo.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseThreadPoolConsumer;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.modules.core.entity.ApiData;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IApiDataService;
import com.ytdinfo.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Scope("prototype")
public class ApiDataConsumer extends BaseThreadPoolConsumer {

    @Autowired
    private IApiDataService iApiDataService;

    @Override
    public void onMessage(MQMessage mqMessage) {
        ApiData vo = JSONUtil.toBean((JSONObject) mqMessage.getContent(), ApiData.class);
        vo.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
        if ("phone".equals(vo.getIdentifier_type())) {
            vo.setIdentifier(MD5Util.md5(vo.getIdentifier()));
        }
        vo.setCreateTime(new Date());
        vo.setUpdateTime(new Date());
        vo.setIsDeleted(false);
        List<ApiData> list = new ArrayList<>();
        list.add(vo);
        iApiDataService.saveBatchWithIgnore(list, 10);
    }
}
