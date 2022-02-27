package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.LimitListRecord;

import java.util.List;

/**
 * 受限名单清单数据处理层
 * @author Timmy
 */
public interface LimitListRecordDao extends BaseDao<LimitListRecord,String> {

    List<LimitListRecord> findByIdentifierIn(List<String> identifiers);

    boolean existsByListIdAndIdentifier(String listId,String identifier);

    List<LimitListRecord> findByListIdAndIsDeleted(String limitListId, Boolean i);

    long countByListId(String listId);

    LimitListRecord findByListIdAndId(String listId,String id);

    LimitListRecord findByListIdAndIdentifier(String listId,String identifier);

}
