package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.vo.UnbindingSetting;
import com.ytdinfo.inndoo.modules.core.dao.BindLogDao;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.entity.BindLog;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import com.ytdinfo.inndoo.modules.core.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
//@CacheConfig(cacheNames = "Account")
public class BindLogServiceImpl implements BindLogService {

    @Autowired
    private BindLogDao bindLogDao;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ActAccountService actAccountService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @Override
    public BindLogDao getRepository() {
        return bindLogDao;
    }

    @Override
    public List<BindLog> findByActAccountId(String accountId) {
        return bindLogDao.findByActAccountIdOrderByCreateTime(accountId);
    }

    @Override
    public List<BindLog> findByPhone(String accountId) {
        return bindLogDao.findByPhoneOrderByCreateTime(accountId);
    }

    @Override
    public BindLog findFirstByActAccountId(String accountId) {
        return bindLogDao.findTopByActAccountIdOrderByCreateTime(accountId);
    }

    @Override
    public int findBindLogCountByActAccountIdAndCreateTimeBetween(String accountId, Date startTime, Date endTime) {
        return bindLogDao.countByActAccountIdAndCreateTimeBetween(accountId, startTime, endTime);
    }

    @Override
    public int findBindLogCountByPhoneAndCreateTimeBetween(String phone, Date startTime, Date endTime) {
        return bindLogDao.countByPhoneAndCreateTimeBetween(phone, startTime, endTime);
    }

    @Override
    public BindLog findFirstByPhone(String phone) {
        return bindLogDao.findTopByPhoneOrderByCreateTime(phone);
    }

    @Override
    @Transactional
    public Result<String> unbind(String actAccountId, UnbindingSetting setting) {
        Result<String> result;
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if (actAccount == null) {
            result = new ResultUtil<String>().setSuccessMsg("未查询到您的账户的信息");
            result.setResult("");
            return result;
        }
        if (StrUtil.isEmpty(actAccount.getCoreAccountId())) {
            result = new ResultUtil<String>().setSuccessMsg("您的账户已经解绑");
            result.setResult("");
            return result;
        }
        Account account = accountService.get(actAccount.getCoreAccountId());
        if (account == null) {
            result = new ResultUtil<String>().setSuccessMsg("您的账户尚未绑定");
            result.setResult("");
            return result;
        }
        if (StrUtil.isEmpty(account.getPhone())) {
            result = new ResultUtil<String>().setSuccessMsg("您的账户已经解绑");
            result.setResult("");
            return result;
        }

        String phone = account.getPhone();
        String aesPhone = AESUtil.encrypt(phone);
        if (setting != null) {
            Long accountUnbindTimes = setting.getAccountUnbindTimes();
            Long accountUnbindDayInterval = setting.getAccountUnbindDayInterval();
            Long phoneUnbindTimes = setting.getPhoneUnbindTimes();
            Long phoneUnbindDayInterval = setting.getPhoneUnbindDayInterval();

            //按账户
            if (accountUnbindTimes != null && accountUnbindTimes.intValue() > 0) {
                BindLog firstBindLog = findFirstByActAccountId(actAccountId);
                if (firstBindLog != null) {
                    Date date = DateUtil.date();
                    Date firstBindDate = firstBindLog.getCreateTime();
                    firstBindDate = DateUtil.beginOfDay(firstBindDate);
                    long betweenDay = DateUtil.between(firstBindDate, date, DateUnit.DAY);
                    long times = betweenDay / accountUnbindDayInterval;
                    Long dayOffset = times * accountUnbindDayInterval;
                    Date startTime = DateUtil.offsetDay(firstBindDate, dayOffset.intValue());
                    Date endTime = DateUtil.offsetDay(startTime, accountUnbindDayInterval.intValue());
                    int unbindCount = findBindLogCountByActAccountIdAndCreateTimeBetween(actAccountId, startTime, endTime);
                    if (unbindCount >= accountUnbindTimes.intValue()) {
                        result = new ResultUtil<String>().setSuccessMsg("您的解绑次数已达到上限");
                        result.setResult("");
                        return result;
                    }
                }
            }
            //按手机号
            if (phoneUnbindTimes != null && phoneUnbindTimes.intValue() > 0) {
                BindLog firstBindLog = findFirstByPhone(aesPhone);
                if (firstBindLog != null) {
                    Date date = DateUtil.date();
                    Date firstBindDate = firstBindLog.getCreateTime();
                    firstBindDate = DateUtil.beginOfDay(firstBindDate);
                    long betweenDay = DateUtil.between(firstBindDate, date, DateUnit.DAY);
                    long times = betweenDay / phoneUnbindDayInterval;
                    Long dayOffset = times * phoneUnbindDayInterval;
                    Date startTime = DateUtil.offsetDay(firstBindDate, dayOffset.intValue());
                    Date endTime = DateUtil.offsetDay(startTime, phoneUnbindDayInterval.intValue());
                    int unbindCount = findBindLogCountByPhoneAndCreateTimeBetween(aesPhone, startTime, endTime);
                    if (unbindCount >= phoneUnbindTimes.intValue()) {
                        result = new ResultUtil<String>().setSuccessMsg("您的解绑次数已达到上限");
                        result.setResult("");
                        return result;
                    }
                }
            }
        }

        List<String> coreAccountIdList = new ArrayList<>();
        coreAccountIdList.add(account.getId());
        //解除小核心项目act和core账户的绑定
        List<ActAccount> actAccounts = actAccountService.findByCoreAccountIds(coreAccountIdList);
        if (null != actAccounts && actAccounts.size() > 0) {
            actAccountService.delete(actAccounts);
        }
        //解除员工和账户的绑定
        List<Staff> staffs = staffService.findByAccountIds(coreAccountIdList);
        for (Staff staff : staffs) {
            staffService.removeFromCache(staff.getAccountId());
            staff.setAccountId("");
        }
        if (null != staffs && staffs.size() > 0) {
            staffService.saveOrUpdateAll(staffs);
        }

        //如果是员工解除员工和账户的绑定
        if (account.getIsStaff() == 1) {
            Integer isStaff = 0;
            account.setIsStaff(isStaff);
            account.setStaffNo("");
            accountService.save(account);
        }

        //解除act绑定小核心账户
        Boolean untiedAccount = activityApiUtil.untied(coreAccountIdList);
        if (!untiedAccount) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result = new ResultUtil<String>().setErrorMsg("解绑账户异常，请稍后再试");
            result.setResult("");
            return result;
        }

        if (setting != null) {
            BindLog bindLog = new BindLog();
            bindLog.setActAccountId(actAccountId);
            bindLog.setCoreAccountId(account.getId());
            bindLog.setPhone(aesPhone);
            bindLog.setIsBind(false);
            save(bindLog);
        }
        result = new ResultUtil<String>().setSuccessMsg("解绑成功");
        result.setResult("");
        return result;
    }

    @Override
    @Transactional
    public Result<String> unbind2(String actAccountId) {
        Result<String> result;
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if (actAccount == null) {
            result = new ResultUtil<String>().setSuccessMsg("未查询到您的账户的信息");
            result.setResult("");
            return result;
        }
        if (StrUtil.isEmpty(actAccount.getCoreAccountId())) {
            result = new ResultUtil<String>().setSuccessMsg("您的账户已经解绑");
            result.setResult("");
            return result;
        }
        Account account = accountService.get(actAccount.getCoreAccountId());
        if (account == null) {
            result = new ResultUtil<String>().setSuccessMsg("您的账户尚未绑定");
            result.setResult("");
            return result;
        }
        if (StrUtil.isEmpty(account.getPhone())) {
            result = new ResultUtil<String>().setSuccessMsg("您的账户已经解绑");
            result.setResult("");
            return result;
        }

        String phone = account.getPhone();
        //String aesPhone = AESUtil.encrypt(phone);
        List<String> coreAccountIdList = new ArrayList<>();
        coreAccountIdList.add(account.getId());
        //解除小核心项目act和core账户的绑定
        actAccountService.delete(actAccount);
        //解除员工和账户的绑定
        List<Staff> staffs = staffService.findByAccountIds(coreAccountIdList);
        for (Staff staff : staffs) {
            staffService.removeFromCache(staff.getAccountId());
            staff.setAccountId("");
        }
        if (null != staffs && staffs.size() > 0) {
            staffService.saveOrUpdateAll(staffs);
        }

        //如果是员工解除员工和账户的绑定
        if (account.getIsStaff() == 1) {
            Integer isStaff = 0;
            account.setIsStaff(isStaff);
            account.setStaffNo("");
            accountService.save(account);
        }

        //解除act绑定小核心账户
        Boolean untiedAccount = activityApiUtil.untiedActAccountId(actAccountId);
        if (!untiedAccount) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result = new ResultUtil<String>().setErrorMsg("解绑账户异常，请稍后再试");
            result.setResult("");
            return result;
        }
        result = new ResultUtil<String>().setSuccessMsg("解绑成功");
        result.setResult("");
        return result;
    }

    @Override
    @Transactional
    public Result<String> unbind(String actAccountId) {
        return unbind(actAccountId, null);
    }
}