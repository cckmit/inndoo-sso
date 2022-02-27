package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.ApiCheck;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 受限名单接口
 * @author Timmy
 */
public interface ApiCheckService extends BaseService<ApiCheck,String> {

    Page<ApiCheck> findByCondition(ApiCheck apiCheck, SearchVo searchVo, Pageable pageable);

    List<ApiCheck> findByDynamicApiIdAndIsDeleted(String dynamicApiId, boolean isDeleted);

    List<ApiCheck> findByAppid(String appid);
}