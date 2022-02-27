package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;

import java.util.List;
import java.util.Map;

public interface IAchieveListService extends BaseIService<AchieveList> {

    List<AchieveList> queryByMap(Map<String,Object> map);

    long countByAppidAndName(String appid, String name);
}
