package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.dao.ApiCheckDao;
import com.ytdinfo.inndoo.modules.core.entity.ApiCheck;
import com.ytdinfo.inndoo.modules.core.service.ApiCheckService;
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

@Slf4j
@Service

@CacheConfig(cacheNames = "apiCheck")
public class ApiCheckServiceImpl implements ApiCheckService {

    @Autowired
    private ApiCheckDao apiCheckDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public ApiCheckDao getRepository() {
        return apiCheckDao;
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ApiCheck get(String id) {
        Optional<ApiCheck> entity = getRepository().findById(id);
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
    @CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ApiCheck save(ApiCheck entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ApiCheck update(ApiCheck entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     *
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(ApiCheck entity) {
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
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<ApiCheck> saveOrUpdateAll(Iterable<ApiCheck> entities) {
        List<ApiCheck> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (ApiCheck entity:entities){
            redisKeys.add("apiCheck::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
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
        ApiCheckDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<ApiCheck> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("apiCheck::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     *
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<ApiCheck> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (ApiCheck entity:entities){
            redisKeys.add("apiCheck::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<ApiCheck> findByCondition(ApiCheck apiCheck, SearchVo searchVo, Pageable pageable) {

        return apiCheckDao.findAll(new Specification<ApiCheck>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<ApiCheck> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> nameField=root.get("name");
                Path<String> appidField = root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();

                // appid
                list.add(cb.equal(appidField,apiCheck.getAppid()));
                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                //白名单名称
                if (StrUtil.isNotBlank(apiCheck.getName())) {
                    list.add(cb.like(nameField,'%'+apiCheck.getName().trim()+'%'));
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
    public List<ApiCheck> findByDynamicApiIdAndIsDeleted(String dynamicApiId, boolean isDeleted) {
        return apiCheckDao.findByDynamicApiIdAndIsDeleted(dynamicApiId,isDeleted);
    }

    @Override
    public List<ApiCheck> findByAppid(String appid) {
        return apiCheckDao.findByAppid(appid);
    }

}