package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.LimitListDao;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import com.ytdinfo.inndoo.modules.core.service.LimitListService;
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

import javax.persistence.criteria.*;
import java.util.*;

/**
 * 受限名单接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "LimitList")
public class LimitListServiceImpl implements LimitListService {

    @Autowired
    private LimitListDao limitListDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public LimitListDao getRepository() {
        return limitListDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public LimitList get(String id) {
        Optional<LimitList> entity = getRepository().findById(id);
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
    public LimitList save(LimitList entity) {
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
    public LimitList update(LimitList entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(LimitList entity) {
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
    public Iterable<LimitList> saveOrUpdateAll(Iterable<LimitList> entities) {
        List<LimitList> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (LimitList entity:entities){
            redisKeys.add("LimitList::" + entity.getId());
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
        LimitListDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<LimitList> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("LimitList::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<LimitList> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (LimitList entity:entities){
            redisKeys.add("LimitList::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<LimitList> findByCondition(LimitList limitList, SearchVo searchVo, Pageable pageable) {

        return limitListDao.findAll(new Specification<LimitList>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<LimitList> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> nameField=root.get("name");
                Path<String> appidField=root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();

                list.add(cb.like(appidField,limitList.getAppid()));
                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                //白名单名称
                if (StrUtil.isNotBlank(limitList.getName())) {
                    list.add(cb.like(nameField,'%'+limitList.getName().trim()+'%'));
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
    public List<LimitList> findByAppid(String appid) {
        return limitListDao.findByAppid(appid);
    }

    @Override
    public long countByAppidAndName(String appid, String name) {
        return limitListDao.countByAppidAndName(appid,name);
    }

    @Override
    public List<LimitList> findByAppidAndName(String name, String appid) {
        return limitListDao.findByAppidAndName(appid,name);
    }

    @Override
    public List<LimitList> findList(String appid) {
        return limitListDao.findByAppid(appid);
    }

    @Override
    public List<LimitList> findByListTypeAndIsEncryption(Integer ListType, byte IsEncryption)
    {
        return limitListDao.findByListTypeAndIsEncryption(ListType,IsEncryption);
    }
}