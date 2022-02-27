package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.WhiteListMapper;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 白名单数据
 * @author zhulin
 */
@Slf4j
@Service
public class IWhiteListServiceImpl extends BaseServiceImpl<WhiteListMapper, WhiteList> implements IWhiteListService {

    @Autowired
    private  WhiteListMapper whiteListMapper;

    @Override
    public long countBylistTypeAndlistId(byte listType, String id) {
        Map<String,Object> map = new HashMap<>();
        map.put("linkType",listType);
        map.put("linkId",id);
        return whiteListMapper.countBylistTypeAndlistId(map);
    }

    @Override
    public List<WhiteList> queryByMap(Map<String,Object> map) {
        return whiteListMapper.queryByMap(map);
    }


    @Override
    public long countByAppidAndName(String appid, String name) {
        Map<String,Object> map = new HashMap<>();
        map.put("appid",appid);
        map.put("name",name);
        return whiteListMapper.countByAppidAndName(map);
    }
}
