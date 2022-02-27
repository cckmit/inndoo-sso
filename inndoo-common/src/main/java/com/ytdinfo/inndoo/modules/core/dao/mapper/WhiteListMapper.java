package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;

import java.util.List;
import java.util.Map;

public interface WhiteListMapper extends BaseInndooMapper<WhiteList> {

    long countBylistTypeAndlistId(Map<String, Object> map);

    List<WhiteList> queryByMap(Map<String, Object> map);

    long countByAppidAndName(Map<String, Object> map);
}
