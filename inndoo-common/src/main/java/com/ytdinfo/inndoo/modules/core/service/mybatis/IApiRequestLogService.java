package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;

import java.util.Date;
import java.util.List;

/**
 * 接口
 * @author timmy
 */
public interface IApiRequestLogService extends IService<ApiRequestLog> {
    List<String> find4Delete(Date date);
    int batchRemove(List<String> ids);
}