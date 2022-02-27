package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.Account;

import java.util.List;

/**
 * 会员账号数据处理层
 * @author Timmy
 */
public interface AccountDao extends BaseDao<Account,String> {
    List<Account> findByAppidAndPhoneIn(String appid,List<String> phones);

    Account findByAppidAndIdentifier(String appid,String identifier);

    List<Account> findByAppidAndPhone(String appid, String phone);

    List<Account> findByCustomerNoAndAppid(String customerNo,String appid);

    List<Account> findByIdentifier(String identifier);

    List<Account> findByAppidAndMd5Phone(String appid, String phone);
}