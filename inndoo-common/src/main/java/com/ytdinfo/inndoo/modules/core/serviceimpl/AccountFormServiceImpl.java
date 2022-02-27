package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.DateUtils;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.core.dao.AccountFormDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AccountFormMapper;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AccountFormMetaMapper;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AccountFormResourceMapper;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormResource;
import com.ytdinfo.inndoo.modules.core.service.AccountFormMetaService;
import com.ytdinfo.inndoo.modules.core.service.AccountFormResourceService;
import com.ytdinfo.inndoo.modules.core.service.AccountFormService;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 会员注册页面主信息接口实现
 *
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "AccountForm")
public class AccountFormServiceImpl implements AccountFormService {

    @Autowired
    private AccountFormDao accountFormDao;
    @Autowired
    private AccountFormMapper accountFormMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    AccountFormMetaService accountFormMetaService;
    @Autowired
    AccountFormResourceService accountFormResourceService;
    @Autowired
    AccountFormMetaMapper accountFormMetaMapper;
    @Autowired
    AccountFormResourceMapper accountFormResourceMapper;

    @Override
    public AccountFormDao getRepository() {
        return accountFormDao;
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    public AccountForm get(String id) {
        Optional<AccountForm> entity = accountFormDao.findById(id);
        if (!entity.isPresent()) {
            return null;
        }
        AccountForm accountForm = entity.get();
        List<AccountFormMeta> AccountFormMetas = accountFormMetaMapper.selectAccountFormMetasByAccountFormId(id);
        List<AccountFormResource> AccountFormResources = accountFormResourceMapper.selectAccountFormResourcesByAccountFormId(id);
        accountForm.setAccountFormMetas(AccountFormMetas);
        accountForm.setAccountFormResources(AccountFormResources);
        accountForm.setViewEndDate(DateUtil.format(accountForm.getEndDate(), "yyyy-MM-dd"));
        accountForm.setViewStartDate(DateUtil.format(accountForm.getStartDate(), "yyyy-MM-dd"));
        setActStatus(accountForm);
        return accountForm;
    }

    /**
     * 保存
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountForm save(AccountForm entity) {
        if (StrUtil.isBlank(entity.getPlatformLimit())) {
            entity.setPlatformLimit("");
        }
        if (StrUtil.isBlank(entity.getStartTime())) {
            entity.setStartTime("00:00:00");
        }
        if (StrUtil.isBlank(entity.getEndTime())) {
            entity.setEndTime("23:59:59");
        }
        if(null == entity.getType() ){
            Byte type = 0;
            entity.setType(type);
        }
        Boolean isDefault = true;
        Boolean isIdentifierForm = false;
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("appid",UserContext.getAppid());
        map.put("formType",entity.getFormType());
        map.put("isDefault",isDefault);
        map.put("isIdentifierForm", isIdentifierForm);
        //查询默认注册表单数量
        Integer count = accountFormMapper.selectCountByMap(map);
        if (count < 1) {
            entity.setIsDefault(isDefault);
        }else {
            entity.setIsDefault(Boolean.FALSE);
        }
        if(StrUtil.isNotBlank(entity.getName())){
            entity.setName(entity.getName().trim());
        }
        if(StrUtil.isNotBlank(entity.getTitle())){
            entity.setTitle(entity.getTitle().trim());
        }
        if (null != entity.getId() && !"".equals(entity.getId())) {
            //修改
            AccountForm accountForm = getRepository().saveAndFlush(entity);
            accountForm = updateAccountForm(accountForm, entity);
            accountForm.setViewEndDate(DateUtil.format(accountForm.getEndDate(), "yyyy-MM-dd"));
            accountForm.setViewStartDate(DateUtil.format(accountForm.getStartDate(), "yyyy-MM-dd"));
            return accountForm;
        } else {
            //添加
            entity.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
            if(entity.getStartDate() == null){
                entity.setStartDate(DateUtil.parseDate("1899-01-01"));
            }
            if(entity.getEndDate() == null){
                entity.setEndDate(DateUtil.parseDate("2099-12-31"));
            }
            AccountForm accountForm = getRepository().save(entity);
            accountForm = updateAccountForm(accountForm, entity);
            accountForm.setViewEndDate(DateUtil.format(accountForm.getEndDate(), "yyyy-MM-dd"));
            accountForm.setViewStartDate(DateUtil.format(accountForm.getStartDate(), "yyyy-MM-dd"));
            return accountForm;
        }
    }

    /**
     * 用来更新AccountFormMeta和AccountFormResource
     *
     * @param accountForm 修改或添加后的对象
     * @param entity
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public AccountForm updateAccountForm(AccountForm accountForm, AccountForm entity) {
        //用作排序
        int afmCount = 0;
        List<AccountFormMeta> accountFormMetas = new ArrayList<>();
        List<AccountFormResource> accountFormResources = new ArrayList<>();
        if (null != entity.getDeleteAccountFormMetaIds() && entity.getDeleteAccountFormMetaIds().size() > 0) {
            for (String id : entity.getDeleteAccountFormMetaIds()) {
                accountFormMetaService.delete(id);
            }
        }
        if(null == entity.getType()){
            Byte type = 0;
            entity.setType(type);
        }
        if (null != entity.getAccountFormMetas() && entity.getAccountFormMetas().size() > 0) {
            for (AccountFormMeta afm : entity.getAccountFormMetas()) {
                //添加
                afm.setCreateTime(new Date());
                afm.setAccountFormId(accountForm.getId());
                afm.setAppid(UserContext.getAppid());
                //排序
                afm.setSortOrder(afmCount++);
                if (null != afm.getId() && !"".equals(afm.getId())) {
                    accountFormMetas.add(accountFormMetaService.update(afm));
                } else {
                    afm.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    accountFormMetas.add(accountFormMetaService.save(afm));
                }

            }
        }
        if (null != entity.getAccountFormResources() && entity.getAccountFormResources().size() > 0) {
            for (AccountFormResource afr : entity.getAccountFormResources()) {
                afr.setAppid(UserContext.getAppid());
                //添加
                afr.setCreateTime(new Date());
                afr.setFormId(accountForm.getId());
                if (StrUtil.isBlank(afr.getResourceData())) {
                    afr.setResourceData("");
                }
                if (null != afr.getId() && !"".equals(afr.getId())) {
                    accountFormResources.add(accountFormResourceService.update(afr));
                } else {
                    afr.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    accountFormResources.add(accountFormResourceService.save(afr));
                }

            }
        }
        accountForm.setAccountFormMetas(accountFormMetas);
        accountForm.setAccountFormResources(accountFormResources);

        Set<String> keysAccountFormMetaNameList = RedisUtil.membersFromKeyStore(RedisKeyStoreType.AccountFormMeta_NameList.getPrefixKey());
        redisTemplate.delete(keysAccountFormMetaNameList);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.AccountFormMeta_NameList.getPrefixKey());
        redisTemplate.delete("AccountFormMeta::MetaList:Identifier:" + accountForm.getId());
        return accountForm;
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountForm update(AccountForm entity) {
        AccountForm accountForm = getRepository().saveAndFlush(entity);
        return updateAccountForm(accountForm, entity);
    }

    /**
     * 修改状态
     *
     * @param entity
     * @return
     */
    @Override

    public AccountForm updateStatus(AccountForm entity) {
        AccountForm accountForm = getRepository().saveAndFlush(entity);
        return accountForm;
    }

    @Override
    public List<AccountForm> findByAppid(String appid) {
        return getRepository().findByAppid(appid);
    }

    /**
     * 删除
     *
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(AccountForm entity) {
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
    @CacheEvict(key = "#id")
    @Override
    public void delete(String id) {
        getRepository().deleteById(id);
        accountFormMetaService.deleteByAccountFormId(id);
        accountFormResourceMapper.deleteByAccountFormId(id);
        List<String> redisKeys = new ArrayList<>();
        redisKeys.add("AccountForm::" + id);
        Set<String> accountFormResourcekeys = RedisUtil.membersFromKeyStore(RedisKeyStoreType.AccountFormResource.getPrefixKey());
        redisTemplate.delete(accountFormResourcekeys);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.AccountFormResource.getPrefixKey());
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<AccountForm> saveOrUpdateAll(Iterable<AccountForm> entities) {
        List<AccountForm> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (AccountForm entity : entities) {
            redisKeys.add("AccountForm::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        AccountFormDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<AccountForm> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id : ids) {
            redisKeys.add("AccountForm::" + id);
            accountFormMetaService.deleteByAccountFormId(id);
            accountFormResourceMapper.deleteByAccountFormId(id);
        }
        Set<String> accountFormResourcekeys = RedisUtil.membersFromKeyStore(RedisKeyStoreType.AccountFormResource.getPrefixKey());
        redisTemplate.delete(accountFormResourcekeys);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.AccountFormResource.getPrefixKey());
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     *
     * @param entities
     */
    @Override
    public void delete(Iterable<AccountForm> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (AccountForm entity : entities) {
            redisKeys.add("AccountForm::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<AccountForm> findByCondition(AccountForm accountForm, SearchVo searchVo, Pageable pageable) {

        return accountFormDao.findAll(new Specification<AccountForm>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<AccountForm> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                Path<String> nameField = root.get("name");
                Path<String> idField = root.get("id");
                Path<String> appidField = root.get("appid");
                Path<Integer> formTypeField = root.get("formType");
                Path<Byte> typeField = root.get("type");
                Path<Boolean> isIdentifierFormField = root.get("isIdentifierForm");
                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                // Path<Date> createTimeField=root.get("createTime");
                List<Predicate> list = new ArrayList<Predicate>();
                list.add(cb.equal(appidField, accountForm.getAppid()));
                //模糊搜素
                if (StrUtil.isNotBlank(accountForm.getName())) {
                    list.add(cb.like(nameField, '%' + accountForm.getName().trim() + '%'));
                }
                if (null != accountForm.getFormType()) {
                    list.add(cb.equal(formTypeField,accountForm.getFormType() ));
                }
                if(null != accountForm.getType()){
                    list.add(cb.equal(typeField,accountForm.getType() ));
                }
                //查询非身份识别表单
                Boolean isIdentifierForm = false;
                list.add(cb.equal(isIdentifierFormField, isIdentifierForm));
                //创建时间
//                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
//                    Date start = DateUtil.parse(searchVo.getStartDate());
//                    Date end = DateUtil.parse(searchVo.getEndDate());
//                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
//                }

                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    /**
     * 获取身份主键表
     * @param appid
     * @param isIdentifierForm
     * @return
     */
    @Override
    @Cacheable(key = "'AccountForm:Identifier:'+#appid")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    public AccountForm findByAppidAndIsIdentifierForm(String appid, Boolean isIdentifierForm) {
        List<AccountForm> accountForms = accountFormDao.findByAppidAndIsIdentifierForm(appid,isIdentifierForm);
        if(null == accountForms || accountForms.size() < 1){
            return null;
        }
        AccountForm accountForm = accountForms.get(0);
        List<AccountFormMeta> AccountFormMetas = accountFormMetaMapper.selectAccountFormMetasByAccountFormId(accountForm.getId());
        List<AccountFormResource> AccountFormResources = accountFormResourceMapper.selectAccountFormResourcesByAccountFormId(accountForm.getId());
        accountForm.setAccountFormMetas(AccountFormMetas);
        accountForm.setAccountFormResources(AccountFormResources);
        setActStatus(accountForm);
        return accountForm;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountForm setDefault(AccountForm accountForm) {
        Map<String,Object> map = new HashMap<>();
        map.put("appid",UserContext.getAppid());
        map.put("formType",accountForm.getFormType());
        accountFormMapper.updateStatus(map);
        AccountForm entity = get(accountForm.getId());
        entity.setIsDefault(true);
        entity = save(entity);
        return entity;
    }

    @Override
    public List<AccountForm> findByMap(Map<String, Object> map) {

        return accountFormMapper.findByMap(map);
    }

    @CachePut(key = "'AccountForm:queryByName:'+#name+'-'+#appid")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    public AccountForm queryByName(String name,String appid ) {
        AccountForm accountForm = new AccountForm();
        Map<String,Object> map = new HashMap<>();
        map.put("name",name);
        map.put("appid",UserContext.getAppid());
        Integer formType = 1;
        map.put("formType",formType);
        List<AccountForm> accountForms = accountFormMapper.findByMap(map);
        if (CollectionUtil.isNotEmpty(accountForms) ) {
            accountForm = accountForms.get(0);
            List<AccountFormMeta> accountFormMetas = accountFormMetaService.findListByAccountFormId(accountForm.getId());
            accountForm.setAccountFormMetas(accountFormMetas);
        }
        return accountForm;
    }

    /**
     * 保存身份识别表单
     * @param entity
     * @return
     */
    @CachePut(key = "'AccountForm:Identifier:'+#entity.appid")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    public AccountForm saveIdentifierForm(AccountForm entity) {
        Boolean IsIdentifierForm = true;
        entity.setIsIdentifierForm(IsIdentifierForm);
        return save(entity);
    }

    /**
     * 判断状态
     * @param aF
     * @return
     */
    @Override
    public AccountForm setActStatus(AccountForm aF) {

        if (null != aF.getStatus() && aF.getStatus() == 0) {
            aF.setActStatus(0);
        } else if (null != aF.getStatus() && aF.getStatus() == -1) {
            aF.setActStatus(3);
        } else if (null != aF.getStatus() && aF.getStatus() == 1) {
            Date date = new Date();
            Date startDate = aF.getStartDate();
            Date endDate = DateUtil.offsetDay(aF.getEndDate(), 1);
            if (date.getTime() <= startDate.getTime()) {
                aF.setActStatus(-1);
            } else if (date.getTime() >= endDate.getTime()) {
                aF.setActStatus(2);
            } else if (date.getTime() > startDate.getTime() && date.getTime() < endDate.getTime()) {
                aF.setActStatus(1);
            }
        }
        return aF;
    }

    @Override
    public long countByName(String name) {
        return accountFormDao.countByName(name);
    }
}