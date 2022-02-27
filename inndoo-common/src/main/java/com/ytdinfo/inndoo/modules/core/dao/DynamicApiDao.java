package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApi;

import java.util.List;

/**
 * 动态接口数据处理层
 * @author zhuzheng
 */
public interface DynamicApiDao extends BaseDao<DynamicApi,String> {

    List<DynamicApi> findByAppid(String appid);

    List<DynamicApi> findByAppidAndDynamicCodeIdsLike(String appid, String dynamicCodeId);
}