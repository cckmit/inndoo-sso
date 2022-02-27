package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.SmsCaptchaLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 手机短信验证码日志接口
 * @author Nolan
 */
public interface SmsCaptchaLogService extends BaseService<SmsCaptchaLog,String> {

    /**
    * 多条件分页获取
    * @param smsCaptchaLog
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<SmsCaptchaLog> findByCondition(SmsCaptchaLog smsCaptchaLog, SearchVo searchVo, Pageable pageable);
}