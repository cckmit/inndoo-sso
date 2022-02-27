package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.LimitListExtendRecord;

import java.util.List;
import java.util.Map;

public interface LimitListExtendRecordMapper extends BaseInndooMapper<LimitListExtendRecord> {
    List<Map<String, Object>> findTransformDate(Map<String, Object> findMap);

    int batchDeleteByIdentifiersAndListId(Map<String,Object> map);

    int batchDeleteByListId(String listId);
}
