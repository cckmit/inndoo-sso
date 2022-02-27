package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.AchieveListRecordDataVo;
import com.ytdinfo.inndoo.common.vo.NameListValidateResultVo;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.common.vo.WhiteListResultVo;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 达标名单清单接口
 * @author Timmy
 */
public interface AchieveListRecordService extends BaseService<AchieveListRecord,String> {

    /**
    * 多条件分页获取
    * @param achieveListRecord
    * @param searchVo
    * @param pageable
    * @param id
    * @return
    */
    Page<AchieveListRecord> findByCondition(AchieveListRecord achieveListRecord, SearchVo searchVo, Pageable pageable, String id);

    NameListValidateResultVo verify(String listId, String recordIdentifier, String openId);
    NameListValidateResultVo validateByCache(AchieveList achieveList, String identifier);
    void updateCacheTime(AchieveList achieveList);

    void loadCache(String listId);

    List<AchieveListRecord> findByListIdAndIsDeleted(String achieveListId, Boolean i);

    String getMd5identifier(AchieveList achieveList, AchieveListRecord record);

    void removeCache(String whilteid, List<String> md5);

    List<List<String>> toWrite(String id);

    long countByListId(String id);

    WhiteListResultVo findByAchieveListAndNextId(AchieveList linkAchieveList, String nextId);

    void deleteByListId(String listId);

    void achieveListPushAct(AchieveList achieveList);

    void achieveListRecordPushAct(AchieveListRecord achieveListRecord);

    void loadSingleCache(String listId,AchieveListRecord record);

    boolean existsByListIdAndIdentifier(String listId, String recordIdentifier);

    AchieveListRecord findByListIdAndId(String listId,String id);

    AchieveListRecord findByListIdAndIdentifier(String listId, String identifier);

    void handlePushAchieveTimes(String listId, String record, String times);

    List<AchieveListRecordDataVo> findByAchieveListIdAndNextId(String achieveListId, String nextId);

    void handlePushAchieve(String listId, String record);
}