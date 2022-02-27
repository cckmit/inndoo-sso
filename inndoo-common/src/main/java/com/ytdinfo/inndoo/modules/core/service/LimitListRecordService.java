package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.LimitListResultVo;
import com.ytdinfo.inndoo.common.vo.NameListValidateResultVo;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.common.vo.WhiteListResultVo;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import com.ytdinfo.inndoo.modules.core.entity.LimitListRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 受限名单清单接口
 * @author Timmy
 */
public interface LimitListRecordService extends BaseService<LimitListRecord,String> {

    /**
    * 多条件分页获取
    * @param limitListRecord
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<LimitListRecord> findByCondition(LimitListRecord limitListRecord, SearchVo searchVo, Pageable pageable, String limitListId);

    List<LimitListRecord> findByIdentifiers(List<String> identifiers);

    NameListValidateResultVo verify(String listId, String recordIdentifier, String openId);
    NameListValidateResultVo validateByCache(LimitList limitList, String identifier);
    void updateCacheTime(LimitList limitList);

    void loadCache(String listId);

    List<LimitListRecord> findByListIdAndIsDeleted(String limitListId, boolean b);

    String getMd5identifier(LimitList limitList, LimitListRecord record);

    void removeCache(String limitid, List<String> md5);

    List<List<String>> toWrite(String id);

    long countByListId(String id);

    WhiteListResultVo findByLimitListAndNextId(LimitList linkLimitList, String nextId);

    void deleteByListId(String listId);

    void loadSingleCache(String listId,LimitListRecord record);

    boolean existsByListIdAndIdentifier(String listId, String recordIdentifier);

    LimitListRecord findByListIdAndId(String listId,String id);

    LimitListRecord findByListIdAndIdentifier(String listId, String identifier);

    LimitListResultVo findByLimitListAndNextId2(LimitList limitList, String nextId);
}
