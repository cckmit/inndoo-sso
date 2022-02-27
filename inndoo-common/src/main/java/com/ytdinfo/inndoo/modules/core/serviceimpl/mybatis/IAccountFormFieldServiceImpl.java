package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AccountFormFieldMapper;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormField;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountFormFieldService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 会员注册扩展表单内容接口实现
 * @author zhulin
 */
@Slf4j
@Service
public class IAccountFormFieldServiceImpl extends BaseServiceImpl<AccountFormFieldMapper, AccountFormField> implements IAccountFormFieldService {

    @Autowired
    private AccountFormFieldMapper accountFormFieldMapper;


    @Override
    public List<AccountFormField> findWithFormByAccount(Account account){
       return  accountFormFieldMapper.findWithFormByAccount(account);
    }

    @Override
    public Integer aesDataSwitchPassword(Map<String,Object> map)
    {
        return accountFormFieldMapper.aesDataSwitchPassword(map);
    }

    @Override
    public List<String> findAccountIdsByFieldData(String fieldData) {
        return accountFormFieldMapper.findAccountIdsByFieldData(fieldData);
    }


}