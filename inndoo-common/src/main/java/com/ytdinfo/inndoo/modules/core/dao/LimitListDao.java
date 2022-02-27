package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;

import java.util.List;

/**
 * 受限名单数据处理层
 * @author Timmy
 */
public interface LimitListDao extends BaseDao<LimitList,String> {

    List<LimitList> findByAppid(String appid);

    List<LimitList> findByAppidAndName(String appid, String name );

    long countByAppidAndName(String appid, String name);

    List<LimitList> findByListTypeAndIsEncryption(Integer ListType, byte IsEncryption);
}