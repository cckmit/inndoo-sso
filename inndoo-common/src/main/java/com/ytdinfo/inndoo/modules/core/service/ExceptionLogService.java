package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.ExceptionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 异常日志接口
 * @author Timmy
 */
public interface ExceptionLogService extends BaseService<ExceptionLog,String> {

    /**
    * 多条件分页获取
    * @param exceptionLog
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<ExceptionLog> findByCondition(ExceptionLog exceptionLog, SearchVo searchVo, Pageable pageable);
}