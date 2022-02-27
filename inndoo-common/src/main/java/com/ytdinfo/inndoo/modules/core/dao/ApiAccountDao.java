package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.ApiAccount;

/**
 * API用户帐号管理数据处理层
 * @author Timmy
 */
public interface ApiAccountDao extends BaseDao<ApiAccount,String> {

    /**
     * 根据appkey获取apiaccount对象
     * @param appkey
     * @return
     */
    ApiAccount findByAppkey(String appkey);
}