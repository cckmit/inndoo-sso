package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListExtendRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 白名单扩展清单接口
 * @author Timmy
 */
public interface WhiteListExtendRecordService extends BaseService<WhiteListExtendRecord,String> {

    /**
    * 多条件分页获取
    * @param whiteListExtendRecord
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<WhiteListExtendRecord> findByCondition(WhiteListExtendRecord whiteListExtendRecord, SearchVo searchVo, Pageable pageable);

    List<WhiteListExtendRecord> findByListId(String whiteListId);

    WhiteListExtendRecord findByRecordIdAndMetaTitle(String recordId, String metaTitle);

    List<WhiteListExtendRecord> findByListIdAndRecordId(String listId, String recordId);

    List<WhiteListExtendRecord>  findByListIdAndIdentifierIn(String listId,List<String> identifiers);

    void deleteByListIdAndIdentifier(String listId, String identifier);
}