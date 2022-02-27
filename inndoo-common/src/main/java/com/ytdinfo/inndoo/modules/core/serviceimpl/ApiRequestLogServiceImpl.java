package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.modules.core.dao.ApiRequestLogDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.ApiRequestLogMapper;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;
import com.ytdinfo.inndoo.modules.core.service.ApiRequestLogService;
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
 * api请求日志接口实现
 * @author zhuzheng
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "ApiRequestLog")
public class ApiRequestLogServiceImpl implements ApiRequestLogService {

    @Autowired
    private ApiRequestLogDao apiRequestLogDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ApiRequestLogMapper apiRequestLogMapper;

    @Override
    public ApiRequestLogDao getRepository() {
        return apiRequestLogDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ApiRequestLog get(String id) {
        Optional<ApiRequestLog> entity = getRepository().findById(id);
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
    public ApiRequestLog save(ApiRequestLog entity) {
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
    public ApiRequestLog update(ApiRequestLog entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(ApiRequestLog entity) {
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
    public Iterable<ApiRequestLog> saveOrUpdateAll(Iterable<ApiRequestLog> entities) {
        List<ApiRequestLog> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (ApiRequestLog entity:entities){
        //    redisKeys.add("ApiRequestLog::" + entity.getId());
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
        ApiRequestLogDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<ApiRequestLog> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        //for (String id:ids){
        //    redisKeys.add("ApiRequestLog::" + id);
        //}
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<ApiRequestLog> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (ApiRequestLog entity:entities){
        //    redisKeys.add("ApiRequestLog::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<ApiRequestLog> findByCondition(ApiRequestLog apiRequestLog, SearchVo searchVo, Pageable pageable) {

        return apiRequestLogDao.findAll(new Specification<ApiRequestLog>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<ApiRequestLog> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> appidField = root.get("appid");
                Path<String> urlField = root.get("url");
                Path<String> requestBodyField = root.get("requestBody");
                Path<String> responseBodyField = root.get("responseBody");
                Path<String> exceptionField = root.get("exception");
                List<Predicate> list = new ArrayList<Predicate>();
                list.add(cb.equal(appidField,UserContext.getAppid()));
                if(StrUtil.isNotBlank(apiRequestLog.getUrl())){
                    list.add(cb.like(urlField, '%' + apiRequestLog.getUrl().trim() + '%'));
                }
                if(StrUtil.isNotBlank(apiRequestLog.getRequestBody())){
                    list.add(cb.like(requestBodyField, '%' + apiRequestLog.getRequestBody().trim() + '%'));
                }
                if(StrUtil.isNotBlank(apiRequestLog.getResponseBody())){
                    list.add(cb.like(responseBodyField, '%' + apiRequestLog.getResponseBody().trim() + '%'));
                }
                if(StrUtil.isNotBlank(apiRequestLog.getException())){
                    list.add(cb.like(exceptionField, '%' + apiRequestLog.getException().trim() + '%'));
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
    public void saveBatch(List<ApiRequestLog> logs, int batchSize) {
        int count = logs.size() / batchSize;
        for (int i = 0; i < count; i++) {
            List<ApiRequestLog> subLogs = logs.subList(i * batchSize, (i + 1) * batchSize);
            saveOrUpdateAll(subLogs);
        }
        int offset = logs.size() % batchSize;
        if (offset > 0) {
            List<ApiRequestLog> subLogs = logs.subList(count * batchSize, logs.size());
            saveOrUpdateAll(subLogs);
        }
    }

    @Override
    public int clearAllApiRequestLog() {
        int k = apiRequestLogMapper.clearAllApiRequestLog();
        while(k > 0){
            k = apiRequestLogMapper.clearAllApiRequestLog();
            if(k == 0){
                break;
            }
        }
        return k;
    }
}