package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListExtendRecord;

import java.util.List;

/**
 * 白名单扩展清单数据处理层
 * @author Timmy
 */
public interface WhiteListExtendRecordDao extends BaseDao<WhiteListExtendRecord,String> {

    List<WhiteListExtendRecord> findByListId(String whiteListId);

    WhiteListExtendRecord findByRecordIdAndMetaTitle(String recordId, String metaTitle);

    List<WhiteListExtendRecord> findByListIdAndRecordId(String listId, String recordId);

    List<WhiteListExtendRecord> findByIdentifierInAndListId(String listId,List<String> identifiers);

    List<WhiteListExtendRecord> findByListIdAndIdentifierIn(String listId,List<String> identifiers);

    void removeByListId(String listId);

    void deleteByListIdAndIdentifier(String listId,String identifier);
}