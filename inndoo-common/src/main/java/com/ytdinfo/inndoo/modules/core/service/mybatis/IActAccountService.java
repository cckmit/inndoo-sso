package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;

import java.util.List;

/**
 * 活动平台账户信息接口
 * @author haiqing
 */
public interface IActAccountService extends IService<ActAccount> {

    void deleteByCoreAccountId (String coreAccountId) ;

}