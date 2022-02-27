package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListRecord;

import java.util.List;

/**
 * 达标名单清单数据处理层
 *
 * @author Timmy
 */
//@Mapper
public interface AchieveListRecordDao extends BaseDao<AchieveListRecord, String> {

    boolean existsByListIdAndIdentifier(String listId, String recordIdentifier);

    List<AchieveListRecord> findByListIdAndIsDeleted(String achieveListId,Boolean isDeleted);

    long countByListId(String listId);

    AchieveListRecord findByListIdAndId(String listId,String id);

    AchieveListRecord findByListIdAndIdentifier(String listId, String identifier);
}