package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.AchieveListDao;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.service.AchieveListService;
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
 * 达标清单接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "AchieveList")
public class AchieveListServiceImpl implements AchieveListService {

    @Autowired
    private AchieveListDao achieveListDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public AchieveListDao getRepository() {
        return achieveListDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    public AchieveList get(String id) {
        Optional<AchieveList> entity = getRepository().findById(id);
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
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    public AchieveList save(AchieveList entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    @Override
    public AchieveList update(AchieveList entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(AchieveList entity) {
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
    public Iterable<AchieveList> saveOrUpdateAll(Iterable<AchieveList> entities) {
        List<AchieveList> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (AchieveList entity:entities){
            redisKeys.add("AchieveList::" + entity.getId());
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
        AchieveListDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<AchieveList> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("AchieveList::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<AchieveList> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (AchieveList entity:entities){
            redisKeys.add("AchieveList::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<AchieveList> findByCondition(AchieveList achieveList, SearchVo searchVo, Pageable pageable) {

        return achieveListDao.findAll(new Specification<AchieveList>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<AchieveList> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> appidField=root.get("appid");
                Path<String> nameField=root.get("name");
                List<Predicate> list = new ArrayList<Predicate>();
                list.add(cb.equal(appidField,achieveList.getAppid()));
                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                //白名单名称
                if (StrUtil.isNotBlank(achieveList.getName())) {
                    list.add(cb.like(nameField,'%'+achieveList.getName().trim()+'%'));
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
    public List<AchieveList> findByLikeValidateFields(String validateFields) {
        return achieveListDao.findByValidateFieldsContains(validateFields);
    }

    @Override
    public List<AchieveList> findByAppid(String appid) {
        return achieveListDao.findByAppid(appid);
    }

    @Override
    public long countByAppidAndName(String appid, String name) {
        return achieveListDao.countByAppidAndName(appid,name);
    }

    @Override
    public List<AchieveList> findByAppidAndName(String name, String appid) {
        return achieveListDao.findByAppidAndName(appid,name);
    }

    @Override
    public List<AchieveList> findList(String appid) {
        return achieveListDao.findByAppid(appid);
    }

    @Override
    public  List<AchieveList> findByListTypeAndIsEncryption(Integer ListType, byte IsEncryption)
    {
        return achieveListDao.findByListTypeAndIsEncryption( ListType,  IsEncryption);
    }
}