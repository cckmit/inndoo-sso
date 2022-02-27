package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AccountMapper;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 客户数据
 * @author Jxy
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "Account")
public class IAccountServiceImpl extends BaseServiceImpl<AccountMapper, Account> implements IAccountService {
    @Autowired
    private AccountMapper accountMapper;

    @Override
    public List<Account> findByMap(Map<String, Object> map) {
        return accountMapper.findByMap(map);
    }

    @Override
    public Integer aesDataSwitchPassword(Map<String,Object> map)
    {
        return accountMapper.aesDataSwitchPassword(map);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        accountMapper.deleteById(id);
    }

    @Override
    public Integer countByCreateTime(String startTime, String endTime) {
        return accountMapper.countByCreateTime(UserContext.getAppid(), startTime, endTime);
    }
}
