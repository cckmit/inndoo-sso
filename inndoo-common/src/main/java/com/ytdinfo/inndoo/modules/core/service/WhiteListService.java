package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 白名单接口
 * @author Timmy
 */
public interface WhiteListService extends BaseService<WhiteList,String> {

    /**
    * 多条件分页获取
    * @param whiteList
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<WhiteList> findByCondition(WhiteList whiteList, SearchVo searchVo, Pageable pageable);

    List<WhiteList> findByAppid(String appid);
    List<WhiteList> findByAppidAndName(String name, String appid);
    long  countByAppidAndName(String appid, String name);

    List<WhiteList> findList(String appid);

    WhiteList findByName(String name);

    List<WhiteList> findByListTypeAndIsEncryption(Integer ListType,byte IsEncryption);

}