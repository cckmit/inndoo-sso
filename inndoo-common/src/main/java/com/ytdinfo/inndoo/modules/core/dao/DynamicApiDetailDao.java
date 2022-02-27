package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApiDetail;

import java.util.List;

/**
 * 动态接口详情数据处理层
 * @author zhuzheng
 */
public interface DynamicApiDetailDao extends BaseDao<DynamicApiDetail,String> {

    DynamicApiDetail findByDynamicApiIdAndVersion(String dynamicApiId, String version);
}