package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.LimitListExtendRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 受限名单扩展信息清单接口
 * @author Timmy
 */
public interface LimitListExtendRecordService extends BaseService<LimitListExtendRecord,String> {

    /**
    * 多条件分页获取
    * @param limitListExtendRecord
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<LimitListExtendRecord> findByCondition(LimitListExtendRecord limitListExtendRecord, SearchVo searchVo, Pageable pageable);

    List<LimitListExtendRecord> findByListIdAndRecordId(String id, String id1);

    LimitListExtendRecord findByRecordIdAndMetaTitle(String id, String metaTitle);

    List<LimitListExtendRecord> findByListIdAndIdentifierIn(String id, List<String> identifiers);

    List<LimitListExtendRecord> findByListId(String listId);

    void deleteByListIdAndIdentifier(String listId,String identifier);
}