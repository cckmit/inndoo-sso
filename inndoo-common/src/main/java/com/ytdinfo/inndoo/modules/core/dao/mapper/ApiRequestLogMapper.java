package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;

import java.util.Date;
import java.util.List;

/**
 * 数据处理层
 * @author timmy
 */
public interface ApiRequestLogMapper extends BaseMapper<ApiRequestLog> {

    List<String> find4Delete(Date date);

    int clearAllApiRequestLog();
}