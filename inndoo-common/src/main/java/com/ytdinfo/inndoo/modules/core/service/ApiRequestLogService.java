package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * api请求日志接口
 * @author zhuzheng
 */
public interface ApiRequestLogService extends BaseService<ApiRequestLog,String> {

    /**
    * 多条件分页获取
    * @param apiRequestLog
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<ApiRequestLog> findByCondition(ApiRequestLog apiRequestLog, SearchVo searchVo, Pageable pageable);

    void saveBatch(List<ApiRequestLog> logs, int i);


    int  clearAllApiRequestLog();
}