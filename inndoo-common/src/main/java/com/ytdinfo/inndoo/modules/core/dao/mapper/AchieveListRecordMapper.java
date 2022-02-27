package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListRecord;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AchieveListRecordMapper extends BaseInndooMapper<AchieveListRecord> {
    int batchDeleteByIdentifierAndListId(Map<String,Object> map);

    long countByListId(String id);

    int batchDeleteByListId(String listId);

    Integer stockByListId(String id);

    BigDecimal getredpackamount(String id);


    void batchInsert(List<AchieveListRecord> list);

    Integer aesDataSwitchPassword(Map<String,Object> map);

}
