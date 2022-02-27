package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.ApiAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * API用户帐号管理接口
 * @author Timmy
 */
public interface ApiAccountService extends BaseService<ApiAccount,String> {

    /**
    * 多条件分页获取
    * @param apiAccount
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<ApiAccount> findByCondition(ApiAccount apiAccount, SearchVo searchVo, Pageable pageable);

    /**
     * 根据appkey获取apiaccount对象
     * @param requestAppkey
     * @return
     */
    ApiAccount findByAppkey(String requestAppkey);
}