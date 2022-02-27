package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCodeDetail;

/**
 * 动态接口详情数据处理层
 * @author zhuzheng
 */
public interface DynamicCodeDetailDao extends BaseDao<DynamicCodeDetail,String> {

    DynamicCodeDetail findByDynamicCodeIdAndVersion(String dynamicCodeId, String version);
}