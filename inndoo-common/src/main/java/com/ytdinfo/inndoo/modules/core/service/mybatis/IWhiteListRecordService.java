package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListRecord;

import java.util.List;
import java.util.Map;

public interface IWhiteListRecordService extends BaseIService<WhiteListRecord> {
    int deleteBatchByIdentifersAndListId(List<String> deleteIdentifers, int i,String listId);

    long countByListId(String id);

    Integer aesDataSwitchPassword(Map<String,Object> map);
}
