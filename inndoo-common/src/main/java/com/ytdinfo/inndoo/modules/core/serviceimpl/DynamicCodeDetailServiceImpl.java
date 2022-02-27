package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.DynamicCodeDetailDao;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCodeDetail;
import com.ytdinfo.inndoo.modules.core.service.DynamicCodeDetailService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DynamicCodeDetailServiceImpl implements DynamicCodeDetailService {

    @Autowired
    private DynamicCodeDetailDao dynamicCodeDetailDao;

    @Autowired
    private RedisTemplate<String,DynamicCodeDetail> redisTemplate;

    @Override
    public DynamicCodeDetailDao getRepository() {
        return dynamicCodeDetailDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DynamicCodeDetail get(String id) {
        Optional<DynamicCodeDetail> entity = getRepository().findById(id);
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
    public DynamicCodeDetail save(DynamicCodeDetail entity) {
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
    public DynamicCodeDetail update(DynamicCodeDetail entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(DynamicCodeDetail entity) {
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
    public Iterable<DynamicCodeDetail> saveOrUpdateAll(Iterable<DynamicCodeDetail> entities) {
        List<DynamicCodeDetail> list = getRepository().saveAll(entities);
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
        DynamicCodeDetailDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<DynamicCodeDetail> list4Delete = repository.findAllById(list);
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
    public void delete(Iterable<DynamicCodeDetail> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (DynamicApiDetail entity:entities){
        //    redisKeys.add("DynamicApiDetail::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<DynamicCodeDetail> findByCondition(DynamicCodeDetail dynamicCodeDetail, SearchVo searchVo, Pageable pageable) {

        return dynamicCodeDetailDao.findAll(new Specification<DynamicCodeDetail>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<DynamicCodeDetail> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
    public DynamicCodeDetail findByDynamicCodeIdAndVersion(String dynamicCodeId, String version) {
        String cacheKey = "dynamicCodeDetail::" + dynamicCodeId + "::" + version;
        DynamicCodeDetail dynamicCodeDetail = redisTemplate.opsForValue().get(cacheKey);
        if (dynamicCodeDetail == null) {
            dynamicCodeDetail = dynamicCodeDetailDao.findByDynamicCodeIdAndVersion(dynamicCodeId, version);
            if (dynamicCodeDetail != null) {
                redisTemplate.opsForValue().set(cacheKey, dynamicCodeDetail, 3, TimeUnit.DAYS);
            }
        }
        return dynamicCodeDetail;
    }
}