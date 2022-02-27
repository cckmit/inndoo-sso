package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.ApiRequestLogMapper;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IApiRequestLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 接口实现
 * @author timmy
 */
@Slf4j
@Service
public class IApiRequestLogServiceImpl extends ServiceImpl<ApiRequestLogMapper, ApiRequestLog> implements IApiRequestLogService {

    @Autowired
    private ApiRequestLogMapper apiRequestLogMapper;

    @Override
    public List<String> find4Delete(Date date) {
        return apiRequestLogMapper.find4Delete(date);
    }

    @Override
    public int batchRemove(List<String> ids) {
        return apiRequestLogMapper.deleteBatchIds(ids);
    }
}