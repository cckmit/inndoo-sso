package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AchieveListMapper;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IAchieveListServiceImpl   extends BaseServiceImpl<AchieveListMapper, AchieveList> implements IAchieveListService {
    @Autowired
    private AchieveListMapper achieveListMapper;

    @Override
    public List<AchieveList> queryByMap(Map<String, Object> map) {
        return achieveListMapper.queryByMap(map);
    }

    @Override
    public long countByAppidAndName(String appid, String name) {
        Map<String,Object> map = new HashMap<>();
        map.put("appid",appid);
        map.put("name",name);
        return achieveListMapper.countByAppidAndName(map);
    }
}
