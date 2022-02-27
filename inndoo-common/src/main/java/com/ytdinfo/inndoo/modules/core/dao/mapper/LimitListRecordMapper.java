package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.LimitListRecord;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface LimitListRecordMapper extends BaseInndooMapper<LimitListRecord> {
    int batchDeleteByIdentifierAndListId(Map<String,Object> map);

    long countByListId(String id);

    int batchDeleteByListId(String listId);

    Integer aesDataSwitchPassword(Map<String,Object> map);
}
