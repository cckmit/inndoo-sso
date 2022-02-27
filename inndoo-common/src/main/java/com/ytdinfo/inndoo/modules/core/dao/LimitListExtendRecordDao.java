package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.LimitListExtendRecord;

import java.util.List;

/**
 * 受限名单扩展信息清单数据处理层
 * @author Timmy
 */
public interface LimitListExtendRecordDao extends BaseDao<LimitListExtendRecord,String> {

    List<LimitListExtendRecord> findByListIdAndRecordId(String listId, String recordId);

    LimitListExtendRecord findByRecordIdAndMetaTitle(String recordId,String metaTitle);

    List<LimitListExtendRecord> findByListIdAndIdentifierIn(String id, List<String> identifiers);

    List<LimitListExtendRecord> findByListId(String listId);

    void deleteByListIdAndIdentifier(String listId,String identifier);
}