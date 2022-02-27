package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;

import java.util.List;

/**
 * 达标清单数据处理层
 * @author Timmy
 */
public interface AchieveListDao extends BaseDao<AchieveList,String> {
    List<AchieveList> findByValidateFieldsContains(String validateFields);

    List<AchieveList> findByAppid(String appid);

    List<AchieveList> findByAppidAndName(String appid, String name );

    long countByAppidAndName(String appid, String name);

    List<AchieveList> findByListTypeAndIsEncryption(Integer ListType, byte IsEncryption);
}