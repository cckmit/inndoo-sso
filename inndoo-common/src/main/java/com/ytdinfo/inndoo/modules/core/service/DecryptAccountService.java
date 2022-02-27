package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.DecryptAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 账户解密信息接口
 * @author cnyao
 */
public interface DecryptAccountService extends BaseService<DecryptAccount,String> {

    /**
    * 多条件分页获取
    * @param decryptAccount
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<DecryptAccount> findByCondition(DecryptAccount decryptAccount, SearchVo searchVo, Pageable pageable);

    DecryptAccount findByCoreAccountId(String coreAccountId);
}