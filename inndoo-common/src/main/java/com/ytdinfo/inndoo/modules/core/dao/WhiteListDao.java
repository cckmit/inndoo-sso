package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;

import java.util.List;

/**
 * 白名单数据处理层
 * @author Timmy
 */
public interface WhiteListDao extends BaseDao<WhiteList,String> {

    List<WhiteList> findByAppid(String Appid);

    List<WhiteList> findByAppidAndName(String appid, String name );

    long countByAppidAndName(String appid, String name);

    WhiteList findByName(String name);

    List<WhiteList> findByListTypeAndIsEncryption(Integer ListType,byte IsEncryption);
}