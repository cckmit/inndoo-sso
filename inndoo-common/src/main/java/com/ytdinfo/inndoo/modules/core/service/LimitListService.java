package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 受限名单接口
 * @author Timmy
 */
public interface LimitListService extends BaseService<LimitList,String> {

    /**
    * 多条件分页获取
    * @param limitList
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<LimitList> findByCondition(LimitList limitList, SearchVo searchVo, Pageable pageable);

    List<LimitList> findByAppid(String appid);

    long countByAppidAndName(String appid, String name);

    List<LimitList> findByAppidAndName(String name, String appid);

    List<LimitList> findList(String appid);

    List<LimitList> findByListTypeAndIsEncryption(Integer ListType, byte IsEncryption);
}