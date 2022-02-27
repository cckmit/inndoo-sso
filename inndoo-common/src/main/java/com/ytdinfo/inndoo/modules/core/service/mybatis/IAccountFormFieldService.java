package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormField;

import java.util.List;
import java.util.Map;

/**
 * 会员注册扩展表单内容接口
 * @author zhulin
 */
public interface IAccountFormFieldService extends IService<AccountFormField> {
    List<AccountFormField> findWithFormByAccount(Account account);

    Integer aesDataSwitchPassword(Map<String,Object> map);

    List<String> findAccountIdsByFieldData(String fieldData);

}