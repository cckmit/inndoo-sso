package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.WhiteListDao;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import com.ytdinfo.inndoo.modules.core.service.WhiteListService;
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
 * 白名单接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "WhiteList")
public class WhiteListServiceImpl implements WhiteListService {

    @Autowired
    private WhiteListDao whiteListDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public WhiteListDao getRepository() {
        return whiteListDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public WhiteList get(String id) {
        Optional<WhiteList> entity = getRepository().findById(id);
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
    public WhiteList save(WhiteList entity) {
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
    public WhiteList update(WhiteList entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(WhiteList entity) {
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
    public Iterable<WhiteList> saveOrUpdateAll(Iterable<WhiteList> entities) {
        List<WhiteList> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (WhiteList entity:entities){
            redisKeys.add("WhiteList::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     * @param ids
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String[] ids) {
        WhiteListDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<WhiteList> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("WhiteList::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<WhiteList> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (WhiteList entity:entities){
            redisKeys.add("WhiteList::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<WhiteList> findByCondition(WhiteList whiteList, SearchVo searchVo, Pageable pageable) {

        return whiteListDao.findAll(new Specification<WhiteList>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<WhiteList> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> nameField=root.get("name");
                Path<String> appidField = root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();

                // appid
                list.add(cb.equal(appidField,whiteList.getAppid()));
                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                //白名单名称
                if (StrUtil.isNotBlank(whiteList.getName())) {
                    list.add(cb.like(nameField,'%'+whiteList.getName().trim()+'%'));
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
    public List<WhiteList> findByAppid(String appid) {
        return whiteListDao.findByAppid(appid);
    }

    @Override
    public List<WhiteList> findByAppidAndName(String appid, String name) {
        return whiteListDao.findByAppidAndName(appid,name);
    }

    @Override
    public long countByAppidAndName(String appid, String name) {
        return whiteListDao.countByAppidAndName(appid,name);
    }

    @Override
    public List<WhiteList> findList(String appid) {
        return whiteListDao.findByAppid(appid);
    }

    @Override
    public WhiteList findByName(String name) {
        return whiteListDao.findByName(name);
    }

    @Override
    public  List<WhiteList> findByListTypeAndIsEncryption(Integer ListType,byte IsEncryption)
    {
        return whiteListDao.findByListTypeAndIsEncryption(ListType,IsEncryption);
    }

}