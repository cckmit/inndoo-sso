package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IAchieveListRecordService extends BaseIService<AchieveListRecord> {
    int deleteBatchByIdentifersAndListId(List<String> deleteIdentifers, int i,String listId);

     long countByListId(String id);

     Integer stockByListId(String listId);

     BigDecimal getredpackamount(String listId);

     void batchInsert(List<AchieveListRecord> list);

    Integer aesDataSwitchPassword(Map<String,Object> map);


}
