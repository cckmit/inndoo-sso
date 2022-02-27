package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;

import java.util.List;
import java.util.Map;

public interface IWhiteListService extends BaseIService<WhiteList> {
    long countBylistTypeAndlistId(byte listType, String id);

    List<WhiteList>  queryByMap(Map<String,Object> map);

    long countByAppidAndName(String appid, String name);
}
