package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.DateUtils;
import com.ytdinfo.inndoo.common.vo.consumer.TagUserVo;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListRecord;
import com.ytdinfo.inndoo.modules.core.service.WhiteListRecordService;
import com.ytdinfo.inndoo.modules.core.service.WhiteListService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Scope("prototype")
public class TagUserPushConsumer extends BaseConsumer {

    @Autowired
    private WhiteListService whiteListService;
    @Autowired
    private WhiteListRecordService whiteListRecordService;
    @Autowired
    private IWhiteListRecordService iWhiteListRecordService;

    @Override
    public void onMessage(MQMessage mqMessage) {
        if(mqMessage.getMessage().getBody() != null){
            TagUserVo tagUserVo = JSONUtil.toBean(new String(mqMessage.getMessage().getBody()), TagUserVo.class);
            if(tagUserVo != null){
                DynamicDataSourceContextHolder.setDataSourceType(tagUserVo.getTenantId());
                UserContext.setTenantId(tagUserVo.getTenantId());
                UserContext.setWxAppId(tagUserVo.getAppId());
                Long size = whiteListRecordService.countByListId(tagUserVo.getReqId());
                if(size > 0){
                    whiteListRecordService.deleteByListId(tagUserVo.getReqId());
                }
                /*WhiteList whiteList = whiteListService.get(tagUserVo.getReqId());
                if(whiteList == null){
                    WhiteList whiteListNew = new WhiteList();
                    whiteListNew.setName("白名单推送");
                    whiteListNew.setListType(1);
                    whiteListNew.setExpireDate(DateUtils.getAfterTime(30));
                    whiteListNew.setFormId("");
                    whiteListNew.setCreateBy("");
                    whiteListNew.setUpdateBy("");
                    whiteListNew.setId(tagUserVo.getReqId());
                    whiteListService.save(whiteListNew);
                }*/
                String data = tagUserVo.getData();
                if(StrUtil.isNotEmpty(data)){
                    JSONArray list = JSONUtil.parseArray(data);
                    List<String> pathList = list.toList(String.class);
                    List<WhiteListRecord> listRecords = new ArrayList<>();
                    for(String record : pathList){
                        WhiteListRecord whiteListRecord = new WhiteListRecord();
                        whiteListRecord.setIdentifier(record);
                        whiteListRecord.setListId(tagUserVo.getReqId());
                        listRecords.add(whiteListRecord);
                    }
                    iWhiteListRecordService.saveBatchWithIgnore(listRecords,500);
                }
            }
        }
    }
}
