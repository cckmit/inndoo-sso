package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 动态代码接口
 * @author zhuzheng
 */
public interface DynamicCodeService extends BaseService<DynamicCode,String> {

    /**
    * 多条件分页获取
    * @param dynamicCode
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<DynamicCode> findByCondition(DynamicCode dynamicCode, SearchVo searchVo, Pageable pageable);

    List<DynamicCode> findByAppid(String appid);
}