package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListExtendRecord;

import java.util.List;

/**
 * 达标名单扩展清单数据处理层
 * @author Timmy
 */
public interface AchieveListExtendRecordDao extends BaseDao<AchieveListExtendRecord,String> {

    List<AchieveListExtendRecord> findByListIdAndRecordId(String listId, String recordId);

    AchieveListExtendRecord findByRecordIdAndMetaTitle(String recordId, String metaTitle);

    List<AchieveListExtendRecord> findByListIdAndIdentifierIn(String id, List<String> identifiers);

    List<AchieveListExtendRecord> findByListId(String listId);

    void deleteByListIdAndIdentifier(String listId, String identifier);
}