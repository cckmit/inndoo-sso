package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.modules.core.dao.mapper.ActAccountMapper;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IActAccountService;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 活动平台账户信息接口实现
 * @author haiqing
 */
@Slf4j
@Service
public class IActAccountServiceImpl extends BaseServiceImpl<ActAccountMapper, ActAccount> implements IActAccountService {

    @Autowired
    private ActAccountMapper actAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByCoreAccountId(String coreAccountId) {
        actAccountMapper.deleteByCoreAccountId(coreAccountId);
    }
}