package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.MqException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * mq执行异常接口
 * @author yaochangning
 */
public interface MqExceptionService extends BaseService<MqException,String> {

    /**
    * 多条件分页获取
    * @param mqException
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<MqException> findByCondition(MqException mqException, SearchVo searchVo, Pageable pageable);
}