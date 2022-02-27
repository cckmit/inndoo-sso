package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApiDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 动态接口详情接口
 * @author zhuzheng
 */
public interface DynamicApiDetailService extends BaseService<DynamicApiDetail,String> {

    /**
    * 多条件分页获取
    * @param dynamicApiDetail
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<DynamicApiDetail> findByCondition(DynamicApiDetail dynamicApiDetail, SearchVo searchVo, Pageable pageable);

    DynamicApiDetail findByDynamicApiIdAndVersion(String dynamicApiId, String version);
}