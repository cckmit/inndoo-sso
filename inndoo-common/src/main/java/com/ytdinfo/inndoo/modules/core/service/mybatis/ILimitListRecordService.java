package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.LimitListRecord;

import java.util.List;
import java.util.Map;

public interface ILimitListRecordService extends BaseIService<LimitListRecord> {
    int deleteBatchByIdentifersAndListId(List<String> deleteIdentifers, int i,String listId);

    long countByListId(String id);

    Integer aesDataSwitchPassword(Map<String,Object> map);

}
