package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.ApiCheck;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 动态接口接口
 * @author zhuzheng
 */
public interface DynamicApiService extends BaseService<DynamicApi,String> {

    /**
    * 多条件分页获取
    * @param dynamicApi
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<DynamicApi> findByCondition(DynamicApi dynamicApi, SearchVo searchVo, Pageable pageable);

    Result<Object> getValue(ApiCheck apiCheck, String actAccountId, String coreAccountId, String openId,Byte accountType);

    List<DynamicApi> findByAppid(String appid);

    List<DynamicApi> findByDynamicCodeIdsLike(String dynamicCodeId);
}