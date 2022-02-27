package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 注册页面ui资源管理接口
 * @author Timmy
 */
public interface AccountFormResourceService extends BaseService<AccountFormResource,String> {

    /**
    * 多条件分页获取
    * @param accountFormResource
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<AccountFormResource> findByCondition(AccountFormResource accountFormResource, SearchVo searchVo, Pageable pageable);

    void deleteByAccountFormId(String accountFormId);
}