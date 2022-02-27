package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.DynamicCodeDao;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCode;
import com.ytdinfo.inndoo.modules.core.service.DynamicCodeService;
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
 * 动态代码接口实现
 * @author zhuzheng
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "DynamicCode")
public class DynamicCodeServiceImpl implements DynamicCodeService {

    @Autowired
    private DynamicCodeDao dynamicCodeDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public DynamicCodeDao getRepository() {
        return dynamicCodeDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DynamicCode get(String id) {
        Optional<DynamicCode> entity = getRepository().findById(id);
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
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DynamicCode save(DynamicCode entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DynamicCode update(DynamicCode entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(DynamicCode entity) {
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
        getRepository().deleteById(id);
    }

    /**
     * 批量保存与修改
     * @param entities
     * @return
     */
    @Override
    public Iterable<DynamicCode> saveOrUpdateAll(Iterable<DynamicCode> entities) {
        List<DynamicCode> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (DynamicCode entity:entities){
            redisKeys.add("DynamicCode::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String[] ids) {
        DynamicCodeDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<DynamicCode> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("DynamicCode::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<DynamicCode> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (DynamicCode entity:entities){
            redisKeys.add("DynamicCode::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<DynamicCode> findByCondition(DynamicCode dynamicCode, SearchVo searchVo, Pageable pageable) {

        return dynamicCodeDao.findAll(new Specification<DynamicCode>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<DynamicCode> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<String> appIdField = root.get("appid");
                Path<String> nameField = root.get("name");
                Path<Date> createTimeField=root.get("createTime");

                List<Predicate> list = new ArrayList<Predicate>();

                list.add(cb.equal(appIdField,dynamicCode.getAppid()));
                if(StrUtil.isNotEmpty(dynamicCode.getName())) {
                    list.add(cb.like(nameField, "%" + dynamicCode.getName() + "%"));
                }

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
    public List<DynamicCode> findByAppid(String appid) {
        return dynamicCodeDao.findByAppid(appid);
    }
}