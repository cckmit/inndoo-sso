package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.EncryptVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 会员账号接口
 * @author Timmy
 */
public interface AccountService extends BaseService<Account,String> {


    /**
    * 多条件分页获取
    * @param account
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<Account> findByCondition(Account account, SearchVo searchVo, Pageable pageable);

    List<Account> findByPhones(List<String> phones);

    List<Account> listByIds(List<String> list);

    List<Account>  findBatchByfindByIds(List<String> ids,int num);

    Result<Account>  saveBindaccountForm(Account entity, AccountForm accountForm, Map<String, Object> map);

    /**
     * account值解密
     * @param entity
     * @return
     */
    Account decryptAccount(Account entity);

    /**
     * 根据唯一标识查询
     * @param identifier
     * @return
     */
    Account findByidentifier(String identifier);

    List<Account> findByAppidAndPhone(String appid, String phone);

    Account copyAccount(Account entity, Account identifierAccount);

    Result<String> getIdentifier(Account account);

    Result<String> getmd5Identifier(Account account);

    /**
     * 通过核心客户号，查询账户信息
     * @param CustomerNo
     * @param appid
     * @return
     */
    List<Account> findByCustomerNoAndAppid(String CustomerNo,String appid);

    /**
     * 清空账号信息
     * @param account
     * @return
     */
    Account clearAccount(Account account);

    List<Account> findListByidentifier(String identifier);

    List<Account> findByAppidAndMd5Phone(String appid, String phone);

    /**
     * 转换加密的vo 为 Account    
     * @param vo
     * @return
     */
    Account convertRsa(EncryptVo vo );

}