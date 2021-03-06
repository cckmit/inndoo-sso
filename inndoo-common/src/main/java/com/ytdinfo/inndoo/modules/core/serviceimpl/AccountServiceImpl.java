package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SM2Utils;
import com.ytdinfo.inndoo.common.vo.EncryptVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.AccountDao;
import com.ytdinfo.inndoo.modules.core.dao.AccountFormFieldDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.StaffMapper;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountFormFieldService;
import com.ytdinfo.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ????????????????????????
 *
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "Account")
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDao accountDao;
    @Autowired
    private AccountFormFieldDao accountFormFieldDao;
    @Autowired
    private AccountFormFieldService accountFormFieldService;

    @Autowired
    private IAccountFormFieldService iAccountFormFieldService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StaffService staffService;
    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private SM2Utils sm2Utils;

    @Override
    public AccountDao getRepository() {
        return accountDao;
    }

    /**
     * ??????ID??????
     *
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    public Account get(String id) {
        Optional<Account> entity = getRepository().findById(id);
        if (!entity.isPresent()) {
            return null;
        }
        Account account = entity.get();
        Account decryptAccount = new Account();
        BeanUtils.copyProperties(account, decryptAccount);
        decryptAccount = decryptAccount(decryptAccount);
        return decryptAccount;
    }

    /**
     * ?????????????????????????????????
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Account save(Account entity) {
        String phone = entity.getPhone();
        Account account;
        Field[] fields = ReflectUtil.getFields(Account.class);
        for (Field field : fields) {
            if ("java.lang.String".equals(field.getType().getName())) {
                Object fieldValue = ReflectUtil.getFieldValue(entity, field);
                if (fieldValue == null || StrUtil.isBlank(fieldValue.toString())) {
                    ReflectUtil.setFieldValue(entity, field, StrUtil.EMPTY);
                } else {
                    if (!field.getName().equals("id") && !field.getName().equals("identifier") && !field.getName().equals("appid")
                            && !field.getName().equals("createBy") && !field.getName().equals("updateBy") && !field.getName().equals("md5Phone") && !field.getName().equals("md5identifier")) {
                        ReflectUtil.setFieldValue(entity, field, AESUtil.encrypt(fieldValue.toString().trim()));
                    }
                }
            }
        }
        if(null == entity.getIsAgreement()){
            entity.setIsAgreement(false);
        }
        if (StrUtil.isNotEmpty(phone)) {
            entity.setMd5Phone(MD5Util.md5(phone));
        }
        if (null == entity.getCreateTime()) {
            entity.setCreateTime(new Date());
        }else {
            entity.setUpdateTime(new Date());
        }
        //id????????????
        if (StrUtil.isBlank(entity.getId())) {
            account = getRepository().save(entity);
        } else {
            //id???????????????
            account = getRepository().saveAndFlush(entity);
        }
        List<AccountFormField> oldList = accountFormFieldDao.findByAccountId(account.getId());
        List<AccountFormField> list = entity.getAccountFormFields();
        if (CollectionUtil.isNotEmpty(list)) {
            for (AccountFormField accountFormField : list) {
                if (StrUtil.isNotBlank(accountFormField.getFieldData()) && accountFormField.getFieldData().length() > 100) {
                    accountFormField.setFieldShortData(AESUtil.encrypt(accountFormField.getFieldData().substring(0, 100)));
                } else {
                    accountFormField.setFieldShortData(AESUtil.encrypt(accountFormField.getFieldData().trim()));
                }
                accountFormField.setFieldData(AESUtil.encrypt(accountFormField.getFieldData().trim()));
                accountFormField.setAccountId(account.getId());
                if (oldList.size() > 0) {

                    List<AccountFormField> oldAccountFormFields = oldList.stream().filter(item -> item.getMetaTitle().equals(accountFormField.getMetaTitle())).collect(Collectors.toList());
                    //????????????accountFormField?????? ????????????id??????
                    if (null != oldAccountFormFields && oldAccountFormFields.size() > 0) {
                        accountFormField.setId(oldAccountFormFields.get(0).getId());
                        // accountFormFieldService.update(accountFormField);
                    }
                }

            }
            //??????????????????
            accountFormFieldService.saveOrUpdateAll(list);
            List<AccountFormField> newList = accountFormFieldDao.findByAccountId(account.getId());
            account.setAccountFormFields(newList);
        } else {
            account.setAccountFormFields(oldList);
        }
        Account decryptAccount = new Account();
        BeanUtils.copyProperties(account, decryptAccount);
        decryptAccount = decryptAccount(decryptAccount);
        //?????? Account:identifier:??????
        String key = "Account::identifier:" + account.getIdentifier();
        redisTemplate.opsForValue().set(key, decryptAccount, CommonConstant.SECOND_10MUNITE, TimeUnit.SECONDS);
        return decryptAccount;
    }

    /**
     * ??????????????????save?????????
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    public Account update(Account entity) {
        Account account = save(entity);
        //?????? Account:identifier:??????
        String key = "Account::identifier:" + account.getIdentifier();
        redisTemplate.opsForValue().set(key, account, CommonConstant.SECOND_10MUNITE, TimeUnit.SECONDS);
        return account;
    }

    /**
     * ??????
     *
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(Account entity) {
        Account account = get(entity.getId());
        List<AccountFormField> oldList = account.getAccountFormFields();
        if (null != oldList && oldList.size() > 0) {
            accountFormFieldService.delete(oldList);
        }
        getRepository().delete(entity);
        //?????? Account:identifier:??????
        String key = "Account::identifier:" + account.getIdentifier();
        redisTemplate.delete(key);
    }

    /**
     * ??????Id??????
     *
     * @param id
     */
    @CacheEvict(key = "#id")
    @Override
    public void delete(String id) {
        Account account = get(id);
        List<AccountFormField> oldList = account.getAccountFormFields();
        if (null != oldList) {
            accountFormFieldService.delete(oldList);
        }
        getRepository().deleteById(id);
        //?????? Account:identifier:??????
        String key = "Account::identifier:" + account.getIdentifier();
        redisTemplate.delete(key);
    }


    /**
     * ?????????????????????
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<Account> saveOrUpdateAll(Iterable<Account> entities) {
        List<Account> list = new ArrayList<>();
        List<String> redisKeys = new ArrayList<>();
        for (Account entity : entities) {
            Account account = save(entity);
            list.add(account);
            redisKeys.add("Account::" + entity.getId());
            //?????? Account:identifier:??????
            String key = "Account::identifier:" + account.getIdentifier();
            redisKeys.add(key);
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * ??????Id????????????
     *
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        AccountDao repository = getRepository();
        List<String> redisKeys = new ArrayList<>();
        for (String id : ids) {
            Account account = get(id);
            List<AccountFormField> oldList = account.getAccountFormFields();
            if (null != oldList) {
                accountFormFieldService.delete(oldList);
            }
            getRepository().deleteById(id);
            redisKeys.add("Account::" + id);
            //?????? Account:identifier:??????
            String key = "Account::identifier:" + account.getIdentifier();
            redisKeys.add(key);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * ????????????
     *
     * @param entities
     */

    @Override
    public void delete(Iterable<Account> entities) {
        List<String> redisKeys = new ArrayList<>();
        for (Account entity : entities) {
            Account account = get(entity.getId());
            List<AccountFormField> oldList = account.getAccountFormFields();
            if (null != oldList && oldList.size() > 0) {
                accountFormFieldService.delete(oldList);
            }
            getRepository().deleteById(entity.getId());
            redisKeys.add("Account::" + entity.getId());
            //?????? Account:identifier:??????
            String key = "Account::identifier:" + account.getIdentifier();
            redisKeys.add(key);
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<Account> findByCondition(Account account, SearchVo searchVo, Pageable pageable) {

        return accountDao.findAll(new Specification<Account>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<Account> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                // TODO ??????????????????????????????????????? ??????????????????????????????
                Path<Date> createTimeField = root.get("createTime");
                Path<String> appidField = root.get("appid");
                Path<String> phoneField = root.get("phone");
                Path<String> customerNoField = root.get("customerNo");
                Path<String> nameField = root.get("name");
                Path<String> idcardNoField = root.get("idcardNo");
                Path<String> bankcardNoField = root.get("bankcardNo");
                Path<String> emailField = root.get("email");
                Path<String> addressField = root.get("address");
                Path<String> staffNoField = root.get("staffNo");
                Path<String> deptNoField = root.get("deptNo");
                Path<String> idField = root.get("id");
                List<Predicate> list = new ArrayList<Predicate>();
                //appid
                list.add(cb.equal(appidField, account.getAppid()));
                //????????????
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                //??????
                if (StrUtil.isNotBlank(account.getPhone())) {
                    list.add(cb.equal(phoneField, AESUtil.encrypt(account.getPhone())));
                }
                //?????????
                if (StrUtil.isNotBlank(account.getCustomerNo())) {
                    list.add(cb.equal(customerNoField, AESUtil.encrypt(account.getCustomerNo())));
                }

                if (StrUtil.isNotBlank(account.getName())) {
                    list.add(cb.equal(nameField, AESUtil.encrypt(account.getName())));
                }

                if (StrUtil.isNotBlank(account.getIdcardNo())) {
                    list.add(cb.equal(idcardNoField, AESUtil.encrypt(account.getIdcardNo())));
                }

                if (StrUtil.isNotBlank(account.getBankcardNo())) {
                    list.add(cb.equal(bankcardNoField, AESUtil.encrypt(account.getBankcardNo())));
                }

                if (StrUtil.isNotBlank(account.getEmail())) {
                    list.add(cb.equal(emailField, AESUtil.encrypt(account.getEmail())));
                }
                if (StrUtil.isNotBlank(account.getAddress())) {
                    list.add(cb.equal(addressField, AESUtil.encrypt(account.getAddress())));
                }
                if (StrUtil.isNotBlank(account.getStaffNo())) {
                    list.add(cb.equal(staffNoField, AESUtil.encrypt(account.getStaffNo())));
                }
                if (StrUtil.isNotBlank(account.getDeptNo())) {
                    list.add(cb.equal(deptNoField, AESUtil.encrypt(account.getDeptNo())));
                }
                if (StrUtil.isNotBlank(account.getAccountFormFieldValue())) {
                     List<String> accountIds = iAccountFormFieldService.findAccountIdsByFieldData(account.getAccountFormFieldValue());
                    if (CollectionUtil.isNotEmpty(accountIds)) {
                        list.add(idField.in(accountIds));
                    } else {
                        //?????????????????????????????????
                        list.add(cb.equal(idField, "err"));
                    }
                }
                //????????????id
                if (StrUtil.isNotBlank(account.getActAccountId())) {
                    ActAccount actAccount = actAccountService.findByActAccountId(account.getActAccountId().trim());
                    //????????????????????????
                    if (null != actAccount) {
                        list.add(cb.equal(idField, actAccount.getCoreAccountId().trim()));
                    }
                    //???????????????????????????????????????id
                    if (null == actAccount) {
                        list.add(cb.equal(idField, account.getActAccountId().trim()));
                    }
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
    public List<Account> findByPhones(List<String> phones) {
        return getRepository().findByAppidAndPhoneIn(UserContext.getAppid(), phones);
    }

    @Override
    public List<Account> listByIds(List<String> list) {
        return getRepository().findAllById(list);
    }

    @Override
    public List<Account> findBatchByfindByIds(List<String> ids, int num) {
        List<Account> allAccounts = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(ids)) {
            num = (num > 0) ? num : 10000;
            int length = ids.size();
            if (length <= num) {
                allAccounts = getRepository().findAllById(ids);
            } else {
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = ids.subList(i * num, (i + 1) * num);
                    if (CollectionUtil.isNotEmpty(temp)) {
                        List<Account> selectAccounts = getRepository().findAllById(temp);
                        if (CollectionUtil.isNotEmpty(selectAccounts)) {
                            allAccounts.addAll(selectAccounts);
                        }
                    }
                }
                List<String> temp1 = ids.subList(times * num, length);
                if (CollectionUtil.isNotEmpty(temp1)) {
                    List<Account> selectAccounts = getRepository().findAllById(temp1);
                    if (CollectionUtil.isNotEmpty(selectAccounts)) {
                        allAccounts.addAll(selectAccounts);
                    }
                }
            }
        }
        return allAccounts;
    }

    // ?????????????????????????????????
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Account> saveBindaccountForm(Account entity, AccountForm accountForm, Map<String, Object> map) {
        if (accountForm.getFormType() == 1) {
            if (null == entity.getIsStaff()) {
                entity.setIsStaff(0);
            }
            if (null != entity.getIsStaff() && entity.getIsStaff() != 1) {
                entity.setIsStaff(0);
            }
        }
        if (accountForm.getFormType() == 0) {
            entity.setIsStaff(1);
        }
        Integer formType = accountForm.getFormType();
        if (accountForm != null) {
            if (formType == 0) {
                String checkStaff = accountForm.getCheckStaff();

                if (StrUtil.isNotBlank(checkStaff)) {
                    String[] checkStaffs = checkStaff.split(",");
                    Map<String, Object> selectMap = new HashMap<>();
                    if (Arrays.asList(checkStaffs).contains("deptNo")) {
                        if (StrUtil.isBlank(entity.getDeptNo())) {
                            return new ResultUtil<Account>().setErrorMsg("????????????????????????");
                        }
                        if (StrUtil.isBlank(map.get("deptNo").toString())) {
                            return new ResultUtil<Account>().setErrorMsg("??????????????????????????????????????????????????????????????????????????????????????????????????????");
                        }
                        selectMap.put("deptNo", entity.getDeptNo());
                    }
                    if (Arrays.asList(checkStaffs).contains("staffNo")) {
                        if (StrUtil.isBlank(entity.getStaffNo())) {
                            return new ResultUtil<Account>().setErrorMsg("???????????????");
                        }
                        if (StrUtil.isBlank(map.get("staffNo").toString())) {
                            return new ResultUtil<Account>().setErrorMsg("???????????????????????????????????????????????????????????????????????????????????????????????????");
                        }
                        selectMap.put("staffNo", entity.getStaffNo());
                    }
                    if (Arrays.asList(checkStaffs).contains("phone")) {
                        if (StrUtil.isBlank(entity.getPhone())) {
                            return new ResultUtil<Account>().setErrorMsg("?????????????????????");
                        }
                        if (StrUtil.isBlank(map.get("phone").toString())) {
                            return new ResultUtil<Account>().setErrorMsg("???????????????????????????????????????????????????????????????????????????????????????????????????");
                        }
                        selectMap.put("phone", AESUtil.encrypt(entity.getPhone().trim()));
                    }
                    if (Arrays.asList(checkStaffs).contains("name")) {
                        if (StrUtil.isBlank(entity.getName())) {
                            return new ResultUtil<Account>().setErrorMsg("????????????????????????");
                        }
                        if (StrUtil.isBlank(map.get("name").toString())) {
                            return new ResultUtil<Account>().setErrorMsg("????????????????????????????????????????????????????????????????????????????????????????????????");
                        }
                        selectMap.put("name", AESUtil.encrypt(entity.getName().trim()));
                    }
                    Integer status = 0;//??????
                    selectMap.put("status", status);
                    selectMap.put("appid", UserContext.getAppid());
                    if (selectMap.keySet().size() < 2) {
                        return new ResultUtil<Account>().setErrorMsg("????????????????????????????????????????????????????????????????????????????????????");
                    }
                    List<Staff> staffs = staffMapper.findByMap(selectMap);
                    if (CollectionUtil.isEmpty(staffs)) {
                        return new ResultUtil<Account>().setErrorMsg("?????????????????????????????????");
                    } else {
                        Staff staff = staffs.get(0);
                        if (staffs.size() > 1) {
                            return new ResultUtil<Account>().setErrorMsg("??????????????????????????????????????????");
                        } else {
                            if (StrUtil.isNotBlank(staff.getAccountId())) {
                                if (!staff.getAccountId().equals(entity.getId())) {
                                    return new ResultUtil<Account>().setErrorMsg("?????????????????????????????????????????????");
                                }
                            }
                        }
                        staff.setAccountId(entity.getId());
                        if (StrUtil.isNotBlank(entity.getName())) {
                            staff.setName(AESUtil.encrypt(entity.getName().trim()));
                        }
                        if (StrUtil.isNotBlank(entity.getPhone())) {
                            staff.setPhone(AESUtil.encrypt(entity.getPhone()));
                        }
                        if (StrUtil.isNotBlank(entity.getStaffNo())) {
                            staff.setStaffNo(entity.getStaffNo());
                        }
                        if (StrUtil.isNotBlank(entity.getDeptNo())) {
                            staff.setDeptNo(entity.getDeptNo());
                        }
                        staffService.update(staff);
                        entity.setStaffNo(staff.getStaffNo());
                        Integer isStaff = 1;
                        entity.setIsStaff(isStaff);
                    }
                    //??????????????????????????????????????????????????????
                    entity = accountService.save(entity);

                } else {
                    return new ResultUtil<Account>().setErrorMsg("????????????????????????????????????????????????????????????????????????????????????");
                }
            }
            if (formType == 1) {
                //?????????????????????????????????????????????????????????
                entity = accountService.save(entity);
            }
        } else {
            return new ResultUtil<Account>().setErrorMsg("?????????????????????????????????");
        }
        return new ResultUtil<Account>().setData(entity);
    }

    /**
     * Account???????????????
     *
     * @param entity
     * @return
     */
    @Override
    public Account decryptAccount(Account entity) {
        Field[] fields = ReflectUtil.getFields(Account.class);
        for (Field field : fields) {
            if ("java.lang.String".equals(field.getType().getName())) {
                Object fieldValue = ReflectUtil.getFieldValue(entity, field);
                if (null == fieldValue || StrUtil.isBlank(fieldValue.toString())) {
                    ReflectUtil.setFieldValue(entity, field, StrUtil.EMPTY);
                } else {
                    if (!field.getName().equals("id") && !field.getName().equals("identifier") && !field.getName().equals("appid")
                            && !field.getName().equals("createBy") && !field.getName().equals("updateBy") && !field.getName().equals("md5Phone")
                            && !field.getName().equals("md5identifier")) {
                        String value = AESUtil.decrypt(fieldValue.toString());
                        ReflectUtil.setFieldValue(entity, field, value);
                    }
                }
            }
        }

        List<AccountFormField> list = iAccountFormFieldService.findWithFormByAccount(entity);
        if (list.size() > 0) {
            for (AccountFormField accountFormField : list) {
                if (StrUtil.isNotBlank(accountFormField.getFieldShortData())) {
                    accountFormField.setFieldShortData(AESUtil.decrypt(accountFormField.getFieldShortData()));
                }
                if (StrUtil.isNotBlank(accountFormField.getFieldData())) {
                    accountFormField.setFieldData(AESUtil.decrypt(accountFormField.getFieldData()));
                }
            }
            entity.setAccountFormFields(list);
        }

        return entity;
    }


    @Override
    @Cacheable(key = "'identifier:'+#identifier")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Account findByidentifier(String identifier) {
        Account account = accountDao.findByAppidAndIdentifier(UserContext.getAppid(), identifier);
        if (null != account) {
            account = get(account.getId());
        }
        return account;
    }

    @Override
    public List<Account> findByAppidAndPhone(String appid, String phone) {
        return accountDao.findByAppidAndPhone(appid, phone);
    }

    /**
     * ???entity?????????????????????identifierAccount
     *
     * @param entity            ????????????????????????
     * @param identifierAccount ?????????????????????
     * @return
     */
    @Override
    public Account copyAccount(Account entity, Account identifierAccount) {
//        entity.setBirthday(null);
        String id = identifierAccount.getId();
        Field[] fields = ReflectUtil.getFields(Account.class);
        for (Field field : fields) {
            if (String.class.isAssignableFrom(field.getType())) {
                String fieldValue = (String) ReflectUtil.getFieldValue(entity, field);
                if (null != fieldValue && StrUtil.isNotBlank(fieldValue)) {
                    ReflectUtil.setFieldValue(identifierAccount, field, fieldValue);
                }
            }
            if ("isStaff".equals(field.getName())) {
                Integer fieldValue = (Integer) ReflectUtil.getFieldValue(entity, field);
                if (null != fieldValue) {
                    ReflectUtil.setFieldValue(identifierAccount, field, fieldValue);
                }
            }
            if ("createTime".equals(field.getName())) {
                Date fieldValue = (Date) ReflectUtil.getFieldValue(entity, field);
                if (null != fieldValue) {
                    ReflectUtil.setFieldValue(identifierAccount, field, fieldValue);
                }
            }
        }
        List<AccountFormField> identifierAccountlist = identifierAccount.getAccountFormFields();
        List<AccountFormField> entityAccountlist = entity.getAccountFormFields();
        //?????????????????????
        if (null != identifierAccountlist && identifierAccountlist.size() > 0) {
            if (CollectionUtil.isNotEmpty(entityAccountlist)) {
                for (AccountFormField accountFormField : entityAccountlist) {
                    List<AccountFormField> addAccountFormFields = identifierAccountlist.stream().filter(item -> item.getMetaTitle().equals(accountFormField.getMetaTitle())).collect(Collectors.toList());
                    if (null != addAccountFormFields && addAccountFormFields.size() > 0) {
                        addAccountFormFields.get(0).setFieldData(accountFormField.getFieldData());
                    } else {
                        identifierAccountlist.add(accountFormField);
                    }
                }
            }
        } else {
            identifierAccount.setAccountFormFields(entityAccountlist);
        }
        identifierAccount.setId(id);
        return identifierAccount;
    }

    /**
     * ????????????????????????
     *
     * @param account
     * @return
     */
    @Override
    public Result<String> getIdentifier(Account account) {
        String identifier = "";

        //????????????????????????
        Boolean isIdentifierForm = true;
        AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(account.getAppid(), isIdentifierForm);
        //???????????????????????????????????????????????????
        List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
        Map dentifierMap = new HashMap<>();
        if (null != IsIdentifierFormMetas && IsIdentifierFormMetas.size() > 0) {
            for (AccountFormMeta accountFormMeta : IsIdentifierFormMetas) {
                if (accountFormMeta.getIsStandard()) {
                    Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                    if (null == object) {
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "????????????????????????????????????");
                    }
                    dentifierMap.put(accountFormMeta.getId(), object.toString().trim());
                }
                if (!accountFormMeta.getIsStandard()) {
                    List<AccountFormField> accountFormFields = account.getAccountFormFields();
                    //????????????????????????????????????accountFormMeta.getTitle()???
                    List<AccountFormField> addAccountFormFields = accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                    if (null != addAccountFormFields && addAccountFormFields.size() > 0) {
                        dentifierMap.put(accountFormMeta.getId(), addAccountFormFields.get(0).getFieldData().trim());
                    } else {
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "????????????????????????????????????");
                    }
                }
            }
            identifier = SecureUtil.signParams(DigestAlgorithm.MD5, dentifierMap, "&", "=", true);

        } else {
            return new ResultUtil<String>().setErrorMsg("???????????????????????????");
        }
        return new ResultUtil<String>().setData(identifier);
    }

    @Override
    public Result<String> getmd5Identifier(Account account) {
        String md5Identifier = "";

        //????????????????????????
        Boolean isIdentifierForm = true;
        AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(account.getAppid(), isIdentifierForm);
        //???????????????????????????????????????????????????
        List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
        List<String> identifiers = new ArrayList<>();
        if (null != IsIdentifierFormMetas && IsIdentifierFormMetas.size() > 0) {
            for (AccountFormMeta accountFormMeta : IsIdentifierFormMetas) {
                if (accountFormMeta.getIsStandard()) {
                    Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                    if (null == object) {
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "????????????????????????????????????");
                    }
                    identifiers.add(object.toString().trim());
                }
                if (!accountFormMeta.getIsStandard()) {
                    List<AccountFormField> accountFormFields = account.getAccountFormFields();
                    //????????????????????????????????????accountFormMeta.getTitle()???
                    List<AccountFormField> addAccountFormFields = accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                    if (null != addAccountFormFields && addAccountFormFields.size() > 0) {
                        identifiers.add( addAccountFormFields.get(0).getFieldData().trim());
                    } else {
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "????????????????????????????????????");
                    }
                }
            }
            identifiers = identifiers.stream().sorted()
                    .collect(Collectors.toList());
            StringBuffer str = new StringBuffer();
            for(String identifier: identifiers){
                str.append(identifier);
            }
            md5Identifier = MD5Util.md5(str.toString());
        } else {
            return new ResultUtil<String>().setErrorMsg("???????????????????????????");
        }
        return new ResultUtil<String>().setData(md5Identifier);
    }


    @Override
    public List<Account> findByCustomerNoAndAppid(String customerNo, String appid) {
        return accountDao.findByCustomerNoAndAppid(customerNo,appid);
    }

    /**
     * ??????????????????
     * @param account
     * @return
     */
    @Override
    public Account clearAccount(Account account) {
        //????????????????????????
        Boolean isIdentifierForm = true;
        AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(account.getAppid(), isIdentifierForm);
        //???????????????????????????????????????????????????
        List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
        Map dentifierMap = new HashMap<>();
        List<String> notStandardList = new ArrayList<>();
        List<AccountFormField> accountFormFields = accountFormFieldService.findByAccountId(account.getId());
        for (AccountFormMeta accountFormMeta : IsIdentifierFormMetas) {
            if (accountFormMeta.getIsStandard()) {
                Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                dentifierMap.put(accountFormMeta.getMetaType(), object.toString().trim());
            }
            if (!accountFormMeta.getIsStandard()) {
                //????????????????????????????????????accountFormMeta.getTitle()???
                List<AccountFormField> addAccountFormFields = accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                if (null != addAccountFormFields && addAccountFormFields.size() > 0) {
                    notStandardList.add(accountFormMeta.getTitle() );
                }
            }
        }
        Account copyAccount = new Account();
        Set keys = dentifierMap.keySet();
        Field[] fields = ReflectUtil.getFields(Account.class);
        for (Field field : fields) {
            if(keys.contains(field.getName())){
                ReflectUtil.setFieldValue(copyAccount, field, dentifierMap.get(field.getName()));
            }
        }
        if(CollectionUtil.isNotEmpty(notStandardList)){
            List<AccountFormField> deleteAccountFormField = new ArrayList<>();
            for (AccountFormField accountFormField : accountFormFields) {
                List<String> filters = notStandardList.stream().filter(item -> item.equals(accountFormField.getMetaTitle())).collect(Collectors.toList());
                if(CollectionUtil.isEmpty(filters)){
                    deleteAccountFormField.add(accountFormField);
                }
            }
            if(CollectionUtil.isNotEmpty(deleteAccountFormField)){
                accountFormFieldService.delete(deleteAccountFormField);
            }
        }else {
            accountFormFieldService.delete(accountFormFields);
        }
        Integer isStaff = 0;
        copyAccount.setIsStaff(isStaff);
        copyAccount.setId(account.getId());
        copyAccount.setIdentifier(account.getIdentifier());
        copyAccount.setIsAgreement(false);
        return copyAccount;
    }

    @Override
    public List<Account> findListByidentifier(String identifier) {
        return accountDao.findByIdentifier(identifier);
    }

    @Override
    public List<Account> findByAppidAndMd5Phone(String appid, String phone) {
        return accountDao.findByAppidAndMd5Phone(appid, phone);
    }



    @Override
    public Account convertRsa(EncryptVo vo) {
        try {
            if (vo != null) {
                return JSONUtil.toBean(sm2Utils.decrypt(vo.getVoJson()), Account.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}