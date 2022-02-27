package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.BindLog;

import java.util.Date;
import java.util.List;

public interface BindLogDao extends BaseDao<BindLog,String> {
    List<BindLog> findByActAccountIdOrderByCreateTime(String accountId);

    List<BindLog> findByPhoneOrderByCreateTime(String accountId);

    BindLog findTopByActAccountIdOrderByCreateTime(String accountId);

    int countByActAccountIdAndCreateTimeBetween(String accountId, Date startTime, Date endTime);

    BindLog findTopByPhoneOrderByCreateTime(String phone);

    int countByPhoneAndCreateTimeBetween(String phone, Date startTime, Date endTime);
}