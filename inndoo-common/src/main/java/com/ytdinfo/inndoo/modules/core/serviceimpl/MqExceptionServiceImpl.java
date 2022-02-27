package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.MqExceptionDao;
import com.ytdinfo.inndoo.modules.core.entity.MqException;
import com.ytdinfo.inndoo.modules.core.service.MqExceptionService;
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
 * mq执行异常接口实现
 * @author yaochangning
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "MqException")
public class MqExceptionServiceImpl implements MqExceptionService {

    @Autowired
    private MqExceptionDao mqExceptionDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public MqExceptionDao getRepository() {
        return mqExceptionDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public MqException get(String id) {
        Optional<MqException> entity = getRepository().findById(id);
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
    public MqException save(MqException entity) {
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
    public MqException update(MqException entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(MqException entity) {
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
    public Iterable<MqException> saveOrUpdateAll(Iterable<MqException> entities) {
        List<MqException> list = getRepository().saveAll(entities);
        /*List<String> redisKeys = new ArrayList<>();
        for (MqException entity:entities){
            redisKeys.add("MqException::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);*/
        return list;
    }

    /**
     * 根据Id批量删除
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        MqExceptionDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<MqException> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        /*List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("MqException::" + id);
        }
        redisTemplate.delete(redisKeys);*/
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<MqException> entities) {
        getRepository().deleteAll(entities);
        /*List<String> redisKeys = new ArrayList<>();
        for (MqException entity:entities){
            redisKeys.add("MqException::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);*/
    }

    @Override
    public Page<MqException> findByCondition(MqException mqException, SearchVo searchVo, Pageable pageable) {

        return mqExceptionDao.findAll(new Specification<MqException>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<MqException> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> queueNameField=root.get("queueName");
                Path<String> exceptionField=root.get("exception");
                Path<String> appidField=root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();
                list.add(cb.equal(appidField,mqException.getAppid()));
                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                if(StrUtil.isNotBlank(mqException.getQueueName())){
                    list.add(cb.like(queueNameField,"%"+mqException.getQueueName().trim()+"%"));
                }
                if(StrUtil.isNotBlank(mqException.getException())){
                    list.add(cb.like(exceptionField,"%"+mqException.getException().trim()+"%"));
                }

                Predicate[] arr = new Predicate[list.size()];
                if(list.size() > 0){
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }
}