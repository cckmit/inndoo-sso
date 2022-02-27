package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.DecryptAccount;

import java.util.List;

/**
 * 账户解密信息数据处理层
 * @author cnyao
 */
public interface DecryptAccountDao extends BaseDao<DecryptAccount,String> {
    DecryptAccount findByCoreAccountId(String coreAccoutId);
}