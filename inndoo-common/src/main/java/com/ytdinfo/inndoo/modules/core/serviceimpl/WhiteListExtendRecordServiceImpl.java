package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.WhiteListExtendRecordDao;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListExtendRecord;
import com.ytdinfo.inndoo.modules.core.service.WhiteListExtendRecordService;
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
 * 白名单扩展清单接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "WhiteListExtendRecord")
public class WhiteListExtendRecordServiceImpl implements WhiteListExtendRecordService {

    @Autowired
    private WhiteListExtendRecordDao whiteListExtendRecordDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public WhiteListExtendRecordDao getRepository() {
        return whiteListExtendRecordDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public WhiteListExtendRecord get(String id) {
        Optional<WhiteListExtendRecord> entity = getRepository().findById(id);
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
    public WhiteListExtendRecord save(WhiteListExtendRecord entity) {
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
    public WhiteListExtendRecord update(WhiteListExtendRecord entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(WhiteListExtendRecord entity) {
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
    public Iterable<WhiteListExtendRecord> saveOrUpdateAll(Iterable<WhiteListExtendRecord> entities) {
        List<WhiteListExtendRecord> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (WhiteListExtendRecord entity:entities){
            redisKeys.add("WhiteListExtendRecord::" + entity.getId());
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
        WhiteListExtendRecordDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<WhiteListExtendRecord> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("WhiteListExtendRecord::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<WhiteListExtendRecord> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (WhiteListExtendRecord entity:entities){
            redisKeys.add("WhiteListExtendRecord::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<WhiteListExtendRecord> findByCondition(WhiteListExtendRecord whiteListExtendRecord, SearchVo searchVo, Pageable pageable) {

        return whiteListExtendRecordDao.findAll(new Specification<WhiteListExtendRecord>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<WhiteListExtendRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> appidField=root.get("appid");

                List<Predicate> list = new ArrayList<Predicate>();
                list.add(cb.equal(appidField, whiteListExtendRecord.getAppid()));
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
    public List<WhiteListExtendRecord> findByListId(String whiteListId) {
        return getRepository().findByListId(whiteListId);
    }

    @Override
    public WhiteListExtendRecord findByRecordIdAndMetaTitle(String recordId, String metaTitle) {
        return getRepository().findByRecordIdAndMetaTitle(recordId,metaTitle);
    }

    @Override
    public List<WhiteListExtendRecord> findByListIdAndRecordId(String listId, String recordId) {
        return getRepository().findByListIdAndRecordId(listId,recordId);
    }

    @Override
    public List<WhiteListExtendRecord> findByListIdAndIdentifierIn(String listId,List<String> identifiers) {
        return getRepository().findByListIdAndIdentifierIn( listId,identifiers);
    }

    @Override
    public void deleteByListIdAndIdentifier(String listId, String identifier) {
        getRepository().deleteByListIdAndIdentifier(listId,identifier);
    }
}