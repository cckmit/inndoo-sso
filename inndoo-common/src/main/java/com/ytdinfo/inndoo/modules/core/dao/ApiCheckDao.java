package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.ApiCheck;

import java.util.List;

public interface ApiCheckDao extends BaseDao<ApiCheck,String> {

    List<ApiCheck> findByDynamicApiIdAndIsDeleted(String dynamicApiId, boolean isDeleted);

    List<ApiCheck> findByAppid(String appid);
}