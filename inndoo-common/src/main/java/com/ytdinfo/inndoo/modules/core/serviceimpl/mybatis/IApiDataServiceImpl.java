package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.modules.core.dao.mapper.ApiDataMapper;
import com.ytdinfo.inndoo.modules.core.entity.ApiData;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IApiDataService;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 接口数据记录接口实现
 * @author haiqing
 */
@Slf4j
@Service
public class IApiDataServiceImpl extends BaseServiceImpl<ApiDataMapper, ApiData> implements IApiDataService {

    @Autowired
    private ApiDataMapper apiDataMapper;
}