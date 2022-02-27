package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.dto.DynamicApiDto;
import com.ytdinfo.inndoo.modules.core.dao.DynamicApiDao;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.vo.SearchVo;
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
 * 动态接口接口实现
 *
 * @author zhuzheng
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "DynamicApi")
public class DynamicApiServiceImpl implements DynamicApiService {

    @Autowired
    private DynamicApiDao dynamicApiDao;

    @Autowired
    private DynamicApiBeanService dynamicApiBeanService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate4getValue;

    @Override
    public DynamicApiDao getRepository() {
        return dynamicApiDao;
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
    public DynamicApi get(String id) {
        Optional<DynamicApi> entity = getRepository().findById(id);
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
    public DynamicApi save(DynamicApi entity) {
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
    public DynamicApi update(DynamicApi entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     *
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(DynamicApi entity) {
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
    public Iterable<DynamicApi> saveOrUpdateAll(Iterable<DynamicApi> entities) {
        List<DynamicApi> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (DynamicApi entity : entities) {
            redisKeys.add("DynamicApi::" + entity.getId());
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
        DynamicApiDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<DynamicApi> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id : ids) {
            redisKeys.add("DynamicApi::" + id);
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
    public void delete(Iterable<DynamicApi> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (DynamicApi entity : entities) {
            redisKeys.add("DynamicApi::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<DynamicApi> findByCondition(DynamicApi dynamicApi, SearchVo searchVo, Pageable pageable) {

        return dynamicApiDao.findAll(new Specification<DynamicApi>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<DynamicApi> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<String> appIdField = root.get("appid");
                Path<String> nameField = root.get("name");
                Path<Date> createTimeField = root.get("createTime");

                List<Predicate> list = new ArrayList<Predicate>();
                list.add(cb.equal(appIdField, dynamicApi.getAppid()));
                if(StrUtil.isNotEmpty(dynamicApi.getName())) {
                    list.add(cb.like(nameField, "%" + dynamicApi.getName() + "%"));
                }
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
    public Result<Object> getValue(ApiCheck apiCheck, String actAccountId, String coreAccountId, String openId,Byte accountType) {
        String redisKey = "API_CHECK_VALUE_" + apiCheck.getId() + "_" + StrUtil.trimToEmpty(actAccountId) + "_" + StrUtil.trimToEmpty(coreAccountId) + "_" + StrUtil.trimToEmpty(openId);
        if (apiCheck.getIsCache() != null && apiCheck.getIsCache()) {
            Object value = redisTemplate4getValue.opsForValue().get(redisKey);
            if (value != null) {
                return new ResultUtil<Object>().setData(value);
            }
        }
        Result<Object> result = dynamicApiBeanService.getBean(apiCheck.getDynamicApiId());
        if (result.isSuccess()) {
            DynamicApiBaseService dynamicApiBaseService = (DynamicApiBaseService) result.getResult();
            DynamicApiDto dto = new DynamicApiDto(UserContext.getTenantId(), UserContext.getAppid(), actAccountId, coreAccountId, openId,accountType);
            result = dynamicApiBaseService.getValue(dto);
            if (result != null && apiCheck.getIsCache() != null && apiCheck.getIsCache()) {
                Long cacheTime = apiCheck.getCacheTime();
                String timeUnit = apiCheck.getTimeUnit();
                TimeUnit unit;
                switch (timeUnit) {
                    case "SECONDS":
                        unit = TimeUnit.SECONDS;
                        break;
                    case "MINUTES":
                        unit = TimeUnit.MINUTES;
                        break;
                    case "HOURS":
                        unit = TimeUnit.HOURS;
                        break;
                    case "DAYS":
                        unit = TimeUnit.DAYS;
                        break;
                    default:
                        unit = null;
                        break;
                }
                if (unit != null) {
                    redisTemplate4getValue.opsForValue().set(redisKey, result.getResult(), cacheTime, unit);
                }
            }
        }
        return result;
    }

    @Override
    public List<DynamicApi> findByAppid(String appid) {
        return dynamicApiDao.findByAppid(appid);
    }

    @Override
    public List<DynamicApi> findByDynamicCodeIdsLike(String dynamicCodeId) {
        return dynamicApiDao.findByAppidAndDynamicCodeIdsLike(UserContext.getAppid(),dynamicCodeId);
    }
}