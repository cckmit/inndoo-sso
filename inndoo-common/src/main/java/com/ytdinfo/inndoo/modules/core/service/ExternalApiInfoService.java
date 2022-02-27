package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.ExternalAPIResultVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.ExternalApiInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

/**
 * 外部接口调用定义表接口
 *
 * @author yaochangning
 */
public interface ExternalApiInfoService extends BaseService<ExternalApiInfo, String> {

    /**
     * 多条件分页获取
     *
     * @param externalApiInfo
     * @param searchVo
     * @param pageable
     * @return
     */
    Page<ExternalApiInfo> findByCondition(ExternalApiInfo externalApiInfo, SearchVo searchVo, Pageable pageable);

    Result<Object> execute(String id, String accountId, String ext, boolean obj);

    ExternalAPIResultVo getRes(String id, String accountId, String ext);

    boolean vertify(String id, String accountId, String ext);
}