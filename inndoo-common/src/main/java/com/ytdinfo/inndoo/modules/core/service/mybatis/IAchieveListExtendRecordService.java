package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListExtendRecord;

import java.util.List;
import java.util.Map;

public interface IAchieveListExtendRecordService extends BaseIService<AchieveListExtendRecord> {
    int deleteBatchByIdentifersAndListId(List<String> deleteIdentifers,String listId, int i);

    List<Map<String, Object>> findTransformDate(Map<String, Object> findMap);
}
