package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.NameListValidateResultVo;
import com.ytdinfo.inndoo.common.vo.WhiteListResultVo;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 白名单清单接口
 * @author Timmy
 */
public interface WhiteListRecordService extends BaseService<WhiteListRecord,String> {

    /**
    * 多条件分页获取
    * @param whiteListRecord
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<WhiteListRecord> findByCondition(WhiteListRecord whiteListRecord, SearchVo searchVo, Pageable pageable,String whiteListId);

    List<WhiteListRecord> findByIdentifiers(List<String> identifiers);

    NameListValidateResultVo verify(String listId, String recordIdentifier, String openId);
    NameListValidateResultVo validateByCache(WhiteList whiteList, String identifier);
    void updateCacheTime(WhiteList whiteList);

    void loadCache(String listId);

    void removeCache(String listId ,List<String> removeIds);

    List<WhiteListRecord> findByListIdAndIsDeleted(String whiteListId, boolean b);

    String getMd5identifier(WhiteList whiteList, WhiteListRecord WhiteListRecord);

    long countByListId(String listId);

    List<List<String>> toWrite(String listId);

    WhiteListResultVo findByWhiteListAndNextId(WhiteList whiteList, String nextId);

    void deleteByListId(String listId);

    void loadSingleCache(String listId,WhiteListRecord record);

    boolean existsByListIdAndIdentifier(String listId, String recordIdentifier);

    WhiteListRecord findByListIdAndId(String listId,String id);

    WhiteListRecord findByListIdAndIdentifier(String listId,String identifier);

    void handlePushListRecord(String listId, String record, String times);
    void AddUpTimesPushListRecord(String listId, String record, String times);
}
