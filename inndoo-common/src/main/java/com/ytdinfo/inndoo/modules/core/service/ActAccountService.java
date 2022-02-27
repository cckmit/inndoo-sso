package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.dto.SimulationStaffRegistrationDto;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 活动平台Account关联表接口
 * @author Timmy
 */
public interface ActAccountService extends BaseService<ActAccount,String> {


    /**
    * 多条件分页获取
    * @param actAccount
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<ActAccount> findByCondition(ActAccount actAccount, SearchVo searchVo, Pageable pageable);

    ActAccount findByActAccountId(String actAccountId);

    List<ActAccount> findByCoreAccountIds(List<String> coreAccountIds);

    List<ActAccount> findByCoreAccountId(String coreAccountId);
    //大数据量批量查询账户信息
    List<ActAccount> findBatchByfindByActAccountIds(List<String> actAccountIds,int num);
    /**
     * 账户导入
     * @param account
     */
    ActAccount accountInput(Account account,String source);

    boolean simulationStaffRegistration(SimulationStaffRegistrationDto dto);

    ActAccount saveWithLock(String actAccountId, String coreAccountId);
}