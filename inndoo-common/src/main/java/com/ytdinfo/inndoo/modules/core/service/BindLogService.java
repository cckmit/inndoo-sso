package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.base.vo.UnbindingSetting;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import com.ytdinfo.inndoo.modules.core.entity.BindLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 绑定日志接口
 * @author Timmy
 */
public interface BindLogService extends BaseService<BindLog,String> {

    List<BindLog> findByActAccountId(String accountId);

    List<BindLog> findByPhone(String accountId);

    BindLog findFirstByActAccountId(String accountId);

    int findBindLogCountByActAccountIdAndCreateTimeBetween(String accountId, Date startTime, Date endTime);

    int findBindLogCountByPhoneAndCreateTimeBetween(String phone, Date startTime, Date endTime);

    BindLog findFirstByPhone(String phone);

    Result<String> unbind(String accountId, UnbindingSetting setting);

    Result<String> unbind2(String accountId);

    Result<String> unbind(String accountId);
}