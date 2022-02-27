package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListExtendRecord;

import java.util.List;
import java.util.Map;

public interface AchieveListExtendRecordMapper extends BaseInndooMapper<AchieveListExtendRecord> {
    int batchDeleteByIdentifiersAndListId(Map<String,Object> map);

    List<Map<String, Object>> findTransformDate(Map<String, Object> findMap);

    int batchDeleteByListId(String listId);
}
