package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.core.dao.AccountFormResourceDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AccountFormResourceMapper;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormResource;
import com.ytdinfo.inndoo.modules.core.service.AccountFormResourceService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
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
import java.util.*;

/**
 * 注册页面ui资源管理接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "AccountFormResource")
public class AccountFormResourceServiceImpl implements AccountFormResourceService {

    @Autowired
    private AccountFormResourceDao accountFormResourceDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    AccountFormResourceMapper AccountFormResourceMapper;

    @Override
    public AccountFormResourceDao getRepository() {
        return accountFormResourceDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public AccountFormResource get(String id) {
        RedisUtil.addKeyToStore(RedisKeyStoreType.AccountFormResource.getPrefixKey(),RedisKeyStoreType.AccountFormResource.getPrefixKey() + id);
        Optional<AccountFormResource> entity = getRepository().findById(id);
        if(entity.isPresent()){
            return entity.get();
        }
        return null;
    }

    /**
     * 保存
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public AccountFormResource save(AccountFormResource entity) {
        RedisUtil.addKeyToStore(RedisKeyStoreType.AccountFormResource.getPrefixKey(),RedisKeyStoreType.AccountFormResource.getPrefixKey() + entity.getId());
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public AccountFormResource update(AccountFormResource entity) {
        RedisUtil.addKeyToStore(RedisKeyStoreType.AccountFormResource.getPrefixKey(), RedisKeyStoreType.AccountFormResource.getPrefixKey() + entity.getId());
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(AccountFormResource entity) {
        RedisUtil.removeKeyFromStore(RedisKeyStoreType.AccountFormResource.getPrefixKey(), RedisKeyStoreType.AccountFormResource.getPrefixKey() + entity.getId());
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     * @param id
     */
    @CacheEvict(key = "#id")
    @Override
    public void delete(String id)
    {
        RedisUtil.removeKeyFromStore(RedisKeyStoreType.AccountFormResource.getPrefixKey(),RedisKeyStoreType.AccountFormResource.getPrefixKey() + id);
        getRepository().deleteById(id);
    }

    /**
     * 批量保存与修改
     * @param entities
     * @return
     */
    @Override
    public Iterable<AccountFormResource> saveOrUpdateAll(Iterable<AccountFormResource> entities) {
        List<AccountFormResource> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (AccountFormResource entity:entities){
            redisKeys.add("AccountFormResource::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        AccountFormResourceDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<AccountFormResource> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("AccountFormResource::" + id);
            RedisUtil.removeKeyFromStore(RedisKeyStoreType.AccountFormResource.getPrefixKey(),RedisKeyStoreType.AccountFormResource.getPrefixKey() + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<AccountFormResource> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (AccountFormResource entity:entities){
            redisKeys.add("AccountFormResource::" + entity.getId());
            RedisUtil.removeKeyFromStore(RedisKeyStoreType.AccountFormResource.getPrefixKey(),RedisKeyStoreType.AccountFormResource.getPrefixKey() + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<AccountFormResource> findByCondition(AccountFormResource accountFormResource, SearchVo searchVo, Pageable pageable) {

        return accountFormResourceDao.findAll(new Specification<AccountFormResource>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<AccountFormResource> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");

                List<Predicate> list = new ArrayList<Predicate>();

                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                Predicate[] arr = new Predicate[list.size()];
                if(list.size() > 0){
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    @Override
    public void deleteByAccountFormId(String accountFormId) {
        AccountFormResourceMapper.deleteByAccountFormId(accountFormId);
    }
}