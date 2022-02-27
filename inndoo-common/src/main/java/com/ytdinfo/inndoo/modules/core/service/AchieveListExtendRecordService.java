package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListExtendRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 达标名单扩展清单接口
 * @author Timmy
 */
public interface AchieveListExtendRecordService extends BaseService<AchieveListExtendRecord,String> {

    /**
    * 多条件分页获取
    * @param achieveListExtendRecord
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<AchieveListExtendRecord> findByCondition(AchieveListExtendRecord achieveListExtendRecord, SearchVo searchVo, Pageable pageable);

    List<AchieveListExtendRecord> findByListIdAndRecordId(String achieveListId, String achieveListRecordId);

    AchieveListExtendRecord findByRecordIdAndMetaTitle(String id, String metaTitle);

    List<AchieveListExtendRecord> findByListIdAndIdentifierIn(String id, List<String> identifiers);

    List<AchieveListExtendRecord> findByListId(String listId);

    void deleteByListIdAndIdentifier(String listId,String identifier);
}