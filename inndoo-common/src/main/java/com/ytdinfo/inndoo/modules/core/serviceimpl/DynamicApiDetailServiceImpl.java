package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.DynamicApiDetailDao;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApiDetail;
import com.ytdinfo.inndoo.modules.core.service.DynamicApiDetailService;
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
import java.util.concurrent.TimeUnit;

/**
 * 动态接口详情接口实现
 * @author zhuzheng
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "DynamicApiDetail")
public class DynamicApiDetailServiceImpl implements DynamicApiDetailService {

    @Autowired
    private DynamicApiDetailDao dynamicApiDetailDao;
    @Autowired
    private RedisTemplate<String,DynamicApiDetail> redisTemplate;

    @Override
    public DynamicApiDetailDao getRepository() {
        return dynamicApiDetailDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DynamicApiDetail get(String id) {
        Optional<DynamicApiDetail> entity = getRepository().findById(id);
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
    //@CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DynamicApiDetail save(DynamicApiDetail entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    //@CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DynamicApiDetail update(DynamicApiDetail entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(DynamicApiDetail entity) {
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     * @param id
     */
    //@CacheEvict(key = "#id")
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
    public Iterable<DynamicApiDetail> saveOrUpdateAll(Iterable<DynamicApiDetail> entities) {
        List<DynamicApiDetail> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (DynamicApiDetail entity:entities){
        //    redisKeys.add("DynamicApiDetail::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String[] ids) {
        DynamicApiDetailDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<DynamicApiDetail> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        //for (String id:ids){
        //    redisKeys.add("DynamicApiDetail::" + id);
        //}
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<DynamicApiDetail> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (DynamicApiDetail entity:entities){
        //    redisKeys.add("DynamicApiDetail::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<DynamicApiDetail> findByCondition(DynamicApiDetail dynamicApiDetail, SearchVo searchVo, Pageable pageable) {

        return dynamicApiDetailDao.findAll(new Specification<DynamicApiDetail>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<DynamicApiDetail> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
    public DynamicApiDetail findByDynamicApiIdAndVersion(String dynamicApiId, String version) {
        String cacheKey = "dynamicApiDetail::" + dynamicApiId + "::" + version;
        DynamicApiDetail dynamicApiDetail = redisTemplate.opsForValue().get(cacheKey);
        if (dynamicApiDetail == null) {
            dynamicApiDetail = dynamicApiDetailDao.findByDynamicApiIdAndVersion(dynamicApiId, version);
            if (dynamicApiDetail != null) {
                redisTemplate.opsForValue().set(cacheKey, dynamicApiDetail, 3, TimeUnit.DAYS);
            }
        }
        return dynamicApiDetail;
    }
}