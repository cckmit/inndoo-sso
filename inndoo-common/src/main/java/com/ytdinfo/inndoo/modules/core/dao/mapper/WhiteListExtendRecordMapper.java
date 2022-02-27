package com.ytdinfo.inndoo.modules.core.dao.mapper;
import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListExtendRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface WhiteListExtendRecordMapper extends BaseInndooMapper<WhiteListExtendRecord> {

    List<Map<String, Object>> findTransformDate(Map<String, Object> findMap);

    int batchDeleteByIdentifiersAndListId(Map<String,Object> map);

    int batchDeleteByListId(String listId);
}
