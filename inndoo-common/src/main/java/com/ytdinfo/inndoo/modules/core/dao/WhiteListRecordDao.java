package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListRecord;

import java.util.List;

/**
 * 白名单清单数据处理层
 * @author Timmy
 */
public interface WhiteListRecordDao extends BaseDao<WhiteListRecord,String> {

    List<WhiteListRecord> findByIdentifierIn(List<String> identifiers);

    boolean existsByListIdAndIdentifier(String listId, String recordIdentifier);

    List<WhiteListRecord> findByListIdAndIsDeleted(String whiteListId, boolean b);

    long countByListId(String listId);

    void removeByListId(String listId);

    WhiteListRecord findByListIdAndId(String listId,String id);

    WhiteListRecord findByListIdAndIdentifier(String listId,String identifier);
}
