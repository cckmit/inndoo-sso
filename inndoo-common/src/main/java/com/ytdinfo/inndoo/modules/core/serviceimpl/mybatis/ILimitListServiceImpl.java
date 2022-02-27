package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.LimitListMapper;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILimitListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ILimitListServiceImpl extends BaseServiceImpl<LimitListMapper, LimitList> implements ILimitListService {
    @Autowired
    private LimitListMapper limitListMapper;

    @Override
    public List<LimitList> queryByMap(Map<String, Object> map) {
        return limitListMapper.queryByMap(map);
    }

    @Override
    public long countByAppidAndName(String appid, String name) {
        Map<String,Object> map = new HashMap<>();
        map.put("appid",appid);
        map.put("name",name);
        return limitListMapper.countByAppidAndName(map);
    }
}
