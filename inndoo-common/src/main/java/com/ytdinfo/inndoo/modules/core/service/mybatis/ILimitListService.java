package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;


import java.util.List;
import java.util.Map;

public interface ILimitListService extends BaseIService<LimitList> {

    List<LimitList> queryByMap(Map<String, Object> map);

    long countByAppidAndName(String appid, String name);
}
