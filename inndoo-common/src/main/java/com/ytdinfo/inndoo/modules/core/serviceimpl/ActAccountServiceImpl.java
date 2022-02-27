package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.dto.SimulationStaffRegistrationDto;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.core.dao.ActAccountDao;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.mqutil.ExceptionLogUtil;
import com.ytdinfo.inndoo.modules.core.service.*;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.persistence.criteria.*;
import java.util.*;

/**
 * 活动平台Account关联表接口实现
 *
 * @author Timmy
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "ActAccount")
public class ActAccountServiceImpl implements ActAccountService {

    @Autowired
    private ActAccountDao actAccountDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private ActivityApiUtil activityApiUtil;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private StaffService staffService;
    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ExceptionLogService exceptionLogService;

    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    @Autowired
    private ExceptionLogUtil exceptionLogUtil;

    @Override
    public ActAccountDao getRepository() {
        return actAccountDao;
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ActAccount get(String id) {
        Optional<ActAccount> entity = getRepository().findById(id);
        if (entity.isPresent()) {
            return entity.get();
        }
        return null;
    }

    /**
     * 保存
     *
     * @param entity
     * @return
     */
    //@CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ActAccount save(ActAccount entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    //@CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ActAccount update(ActAccount entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     *
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(ActAccount entity) {
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
//    @CacheEvict(key = "#id")
    @Override
    public void delete(String id) {
        getRepository().deleteById(id);
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<ActAccount> saveOrUpdateAll(Iterable<ActAccount> entities) {
        List<ActAccount> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (ActAccount entity:entities){
        //    redisKeys.add("ActAccount::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String[] ids) {
        ActAccountDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<ActAccount> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        //for (String id:ids){
        //    redisKeys.add("ActAccount::" + id);
        //}
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     *
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<ActAccount> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (ActAccount entity:entities){
        //    redisKeys.add("ActAccount::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<ActAccount> findByCondition(ActAccount actAccount, SearchVo searchVo, Pageable pageable) {

        return actAccountDao.findAll(new Specification<ActAccount>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<ActAccount> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField = root.get("createTime");

                List<Predicate> list = new ArrayList<Predicate>();

                //创建时间
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    @Override
    public ActAccount findByActAccountId(String actAccountId) {
        return getRepository().findByActAccountId(actAccountId);
    }

    @Override
    public List<ActAccount> findByCoreAccountIds(List<String> coreAccountIds) {
        return getRepository().findByCoreAccountIdIn(coreAccountIds);
    }

    @Override
    public List<ActAccount> findByCoreAccountId(String coreAccountId) {
        return getRepository().findByCoreAccountId(coreAccountId);
    }

    @Override
    public List<ActAccount> findBatchByfindByActAccountIds(List<String> actAccountIds, int num) {
        List<ActAccount> allActAccounts = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(actAccountIds)) {
            num = (num > 0) ? num : 10000;
            int length = actAccountIds.size();
            if (length <= num) {
                allActAccounts = getRepository().findByActAccountIdIn(actAccountIds);
            } else {
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = actAccountIds.subList(i * num, (i + 1) * num);
                    if (CollectionUtil.isNotEmpty(temp)) {
                        List<ActAccount> selectActAccounts = getRepository().findByActAccountIdIn(temp);
                        if (CollectionUtil.isNotEmpty(selectActAccounts)) {
                            allActAccounts.addAll(selectActAccounts);
                        }
                    }
                }
                List<String> temp1 = actAccountIds.subList(times * num, length);
                if (CollectionUtil.isNotEmpty(temp1)) {
                    List<ActAccount> selectActAccounts = getRepository().findByActAccountIdIn(temp1);
                    if (CollectionUtil.isNotEmpty(selectActAccounts)) {
                        allActAccounts.addAll(selectActAccounts);
                    }
                }
            }
        }
        return allActAccounts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActAccount accountInput(Account account, String source) {
        return privateAccountInput(account, source);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean simulationStaffRegistration(SimulationStaffRegistrationDto dto) {
        Department department = departmentService.findByDeptCode(dto.getDeptCode());
        if (department == null) {
            return false;
        }
        String decryptPhone = AESUtil.decrypt(dto.getPhone());

        Account account = new Account();
        account.setAppid(dto.getAppId());
        account.setPhone(decryptPhone);
        account.setIdcardNo(StrUtil.EMPTY);
        account.setBankcardNo(StrUtil.EMPTY);
        account.setCustomerNo(StrUtil.EMPTY);
        account.setBirthday(StrUtil.EMPTY);
        account.setName(AESUtil.decrypt(dto.getName()));
        account.setLicensePlateNo(StrUtil.EMPTY);
        // 普通客户0、员工1
        account.setIsStaff(1);
        account.setStaffNo(dto.getStaffNo());
        account.setDeptNo(department.getId());
        account.setActAccountId(AESUtil.decrypt(dto.getActAccountId(), AESUtil.WXLOGIN_PASSWORD));
        account.setAccountFormFields(new ArrayList<>());
        account.setCreateTime(dto.getBindTime());

        ActAccount actAccount = privateAccountInput(account, null);
        if (actAccount != null) {
            Staff staff = new Staff();
            staff.setAccountId(actAccount.getCoreAccountId());
            staff.setAppid(account.getAppid());
            staff.setName(dto.getName());
            staff.setPhone(dto.getPhone());
            staff.setStaffNo(dto.getStaffNo());
            staff.setDeptNo(department.getId());
            Staff localStaff = staffService.findByStaffNo(staff.getStaffNo());
            if (localStaff != null) {
                staff.setId(localStaff.getId());
            }
            List<Staff> list = new ArrayList<>();
            list.add(staff);
            staffService.saveOrUpdateAll(list);
            staffService.addToCache(staff.getAccountId());
        }
        return true;
    }

    private ActAccount privateAccountInput(Account account, String source) {
        String identifier = "";
        //拼接用户唯一标识
        Result<String> result = accountService.getIdentifier(account);
        if (result.isSuccess()) {
            identifier = result.getResult();
        } else {
            throw new RuntimeException(result.getMessage());
        }
        //唯一标识不存在时
        if (StrUtil.isBlank(account.getIdentifier())) {
            Account identifierAccount;
            try {
                identifierAccount = accountService.findByidentifier(identifier);
            } catch (Exception e) {
                String key = "Account::identifier:" + identifier;
                redisTemplate.delete(key);
                identifierAccount = accountService.findByidentifier(identifier);
            }

            if (null == identifierAccount) {
                account.setIdentifier(identifier);
            } else {
                if (StrUtil.isBlank(account.getId())) {
                    account = accountService.copyAccount(account, identifierAccount);
                }
                if (StrUtil.isNotBlank(account.getId()) && !identifierAccount.getId().equals(account.getId())) {
                    account = accountService.copyAccount(account, identifierAccount);
                }
                account.setIdentifier(identifier);
            }
        } else {
            //唯一标识存在时判断是否正确
            if (!identifier.equals(account.getIdentifier())) {
                throw new RuntimeException("用户唯一标识不匹配无法添加账户");
            }
        }
//        Map<String, Object> accountFormMap = new HashMap<>();
//        accountFormMap.put("name", "账户批量导入注册页");
//        accountFormMap.put("formType", 1);
//        accountFormMap.put("appid", UserContext.getAppid());

        AccountForm accountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(),true);
        accountForm.setFormType(1);
        if (null == accountForm ) {
            throw new RuntimeException("必须首先添加客户唯一标识注册页");
        }
        //表示普通用户注册页
        if (accountForm.getFormType() == 1) {
            if (null == account.getIsStaff()) {
                account.setIsStaff(0);
            }
            if (null != account.getIsStaff() && account.getIsStaff() != 1) {
                account.setIsStaff(0);
            }
        }
        String actAccountId = account.getActAccountId();
        String phone = account.getPhone();
        String formId = accountForm.getId();
        Map<String, Object> map = new HashMap<>();

        Date accountCreateTime = account.getCreateTime();

        Account saveAndUpdateAccount;
        Result<Account> saveAndUpdateAccountResult = accountService.saveBindaccountForm(account, accountForm, map);
        if (!saveAndUpdateAccountResult.isSuccess()) {
            throw new RuntimeException(saveAndUpdateAccountResult.getMessage());
        } else {
            saveAndUpdateAccount = saveAndUpdateAccountResult.getResult();
            if (accountCreateTime != null) {
                saveAndUpdateAccount.setCreateTime(accountCreateTime);
                saveAndUpdateAccount = accountService.save(saveAndUpdateAccount);
            }
        }
        if (StrUtil.isBlank(actAccountId)) {
            throw new RuntimeException("必须包含绑定act账户");
        }
        actAccountId = AESUtil.encrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD);
        // 已验证 清除key
        redisTemplate.delete(CommonConstant.PRE_SMS + phone);
        String actDecodeAccountId = AESUtil.decrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD);
        String coreAccountId = saveAndUpdateAccount.getId();
        // 如果source不为空 则无需向活动平台回调触发事件
        if (StrUtil.isEmpty(source)) {
            //返回加密的活动平台AccountId
            Result bindAccountResult = activityApiUtil.bindAccount(AESUtil.encrypt(coreAccountId, AESUtil.WXLOGIN_PASSWORD), actAccountId, formId, saveAndUpdateAccount
                    .getCreateTime());
            if (bindAccountResult == null) {
                throw new RuntimeException("添加账户失败，请求活动平台异常");
            } else if (!bindAccountResult.isSuccess()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new RuntimeException("添加账户失败，"+bindAccountResult.getMessage());
            }
        }
        return saveWithLock(actDecodeAccountId, coreAccountId);
    }

    @Override
    public ActAccount saveWithLock(String actAccountId, String coreAccountId) {
        Object object = lockTemplate.execute("bindAccount:" + actAccountId, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                ActAccount actAccount = findByActAccountId(actAccountId);
                if (actAccount != null) {
                    if (!actAccount.getCoreAccountId().equals(coreAccountId)) {
                        actAccountService.delete(actAccount.getId());
                        ActAccount actAccountNew = new ActAccount();
                        actAccountNew.setActAccountId(actAccountId);
                        actAccountNew.setCoreAccountId(coreAccountId);
                        actAccountService.save(actAccountNew);
                        actAccount = actAccountNew;
                    }
                } else {
                    ActAccount actAccountNew = new ActAccount();
                    actAccountNew.setActAccountId(actAccountId);
                    actAccountNew.setCoreAccountId(coreAccountId);
                    actAccountService.save(actAccountNew);
                    actAccount = actAccountNew;
                }
                return actAccount;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                ExceptionLog exceptionLog = new ExceptionLog();
                exceptionLog.setUrl("save With Lock Timeout");
                exceptionLog.setException("save With Lock Timeout");
                exceptionLog.setMsgBody("actAccountId：" + actAccountId);
                exceptionLog.setAppid(UserContext.getAppid());
                //exceptionLogService.save(exceptionLog);
                exceptionLogUtil.sendMessage(exceptionLog);
                return null;
            }
        });
        if (object != null) {
            return (ActAccount) object;
        }
        return null;
    }

}