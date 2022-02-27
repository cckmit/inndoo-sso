package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;

import java.util.List;

/**
 * 活动平台Account关联表数据处理层
 *
 * @author Timmy
 */
public interface ActAccountDao extends BaseDao<ActAccount, String> {

    ActAccount findByActAccountId(String actAccountId);

    List<ActAccount> findByCoreAccountIdIn(List<String> coreAccountIds);

    List<ActAccount> findByCoreAccountId(String coreAccountId);

    List<ActAccount> findByActAccountIdIn(List<String> actAccountIds);
}