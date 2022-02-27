package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;

import java.util.List;
import java.util.Map;

public interface AchieveListMapper extends BaseInndooMapper<AchieveList> {

    List<AchieveList> queryByMap(Map<String, Object> map);

    long countByAppidAndName(Map<String, Object> map);
}
