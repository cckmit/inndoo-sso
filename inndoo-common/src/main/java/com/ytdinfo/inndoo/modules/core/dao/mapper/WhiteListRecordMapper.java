package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface WhiteListRecordMapper extends BaseInndooMapper<WhiteListRecord> {

    int batchDeleteByIdentifierAndListId(Map<String,Object> map);

    int batchDeleteByListId(String listId);

    long countByListId(String id);

    Integer aesDataSwitchPassword(Map<String,Object> map);
}
