package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 达标清单接口
 * @author Timmy
 */
public interface AchieveListService extends BaseService<AchieveList,String> {

    /**
    * 多条件分页获取
    * @param achieveList
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<AchieveList> findByCondition(AchieveList achieveList, SearchVo searchVo, Pageable pageable);

    List<AchieveList> findByLikeValidateFields(String validateFields);

    List<AchieveList> findByAppid(String appid);

    long countByAppidAndName(String appid, String name);

    List<AchieveList> findByAppidAndName(String name, String appid);

    List<AchieveList> findList(String appid);

    List<AchieveList> findByListTypeAndIsEncryption(Integer ListType, byte IsEncryption);
}