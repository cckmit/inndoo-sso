package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCodeDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

/**
 * 动态接口详情接口
 * @author zhuzheng
 */
public interface DynamicCodeDetailService extends BaseService<DynamicCodeDetail,String> {

    /**
    * 多条件分页获取
    * @param dynamicCodeDetail
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<DynamicCodeDetail> findByCondition(DynamicCodeDetail dynamicCodeDetail, SearchVo searchVo, Pageable pageable);

    DynamicCodeDetail findByDynamicCodeIdAndVersion(String dynamicCodeId, String version);
}