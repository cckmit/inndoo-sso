package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.ApiAccountDao;
import com.ytdinfo.inndoo.modules.core.entity.ApiAccount;
import com.ytdinfo.inndoo.modules.core.service.ApiAccountService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * API用户帐号管理接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "ApiAccount")
public class ApiAccountServiceImpl implements ApiAccountService {

    @Autowired
    private ApiAccountDao apiAccountDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public ApiAccountDao getRepository() {
        return apiAccountDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Override
    public ApiAccount get(String id) {
        return getRepository().getOne(id);
    }

    /**
     * 保存
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.appkey")
    @CacheExpire(expire = CommonConstant.SECOND_1YEAR)
    @Override
    public ApiAccount save(ApiAccount entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.appkey")
    @CacheExpire(expire = CommonConstant.SECOND_1YEAR)
    @Override
    public ApiAccount update(ApiAccount entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.appkey")
    @Override
    public void delete(ApiAccount entity) {
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     * @param id
     */
    @Override
    public void delete(String id)
    {
        ApiAccount apiAccount = getRepository().getOne(id);
        redisTemplate.delete("ApiAccount::" + apiAccount.getAppkey());
        getRepository().deleteById(id);
    }

    /**
     * 批量保存与修改
     * @param entities
     * @return
     */
    @Override
    public Iterable<ApiAccount> saveOrUpdateAll(Iterable<ApiAccount> entities) {
        List<ApiAccount> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (ApiAccount entity:entities){
            redisKeys.add("ApiAccount::" + entity.getAppkey());
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
        ApiAccountDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<ApiAccount> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (ApiAccount entity:list4Delete){
            redisKeys.add("ApiAccount::" + entity.getAppkey());
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<ApiAccount> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (ApiAccount entity:entities){
            redisKeys.add("ApiAccount::" + entity.getAppkey());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<ApiAccount> findByCondition(ApiAccount apiAccount, SearchVo searchVo, Pageable pageable) {

        return apiAccountDao.findAll(new Specification<ApiAccount>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<ApiAccount> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
    @Cacheable(key = "#appkey")
    @CacheExpire(expire = CommonConstant.SECOND_1YEAR)
    public ApiAccount findByAppkey(String appkey) {
        return apiAccountDao.findByAppkey(appkey);
    }
}