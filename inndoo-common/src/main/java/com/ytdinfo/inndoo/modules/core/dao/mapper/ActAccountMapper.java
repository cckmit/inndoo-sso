package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;

import java.util.List;

/**
 * 活动平台账户信息数据处理层
 * @author haiqing
 */
public interface ActAccountMapper extends BaseInndooMapper<ActAccount> {
     Integer deleteByCoreAccountId(String coreAccountId);
}