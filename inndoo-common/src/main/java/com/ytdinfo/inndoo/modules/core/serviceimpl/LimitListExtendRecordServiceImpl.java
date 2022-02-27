package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.LimitListExtendRecordDao;
import com.ytdinfo.inndoo.modules.core.entity.LimitListExtendRecord;
import com.ytdinfo.inndoo.modules.core.service.LimitListExtendRecordService;
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
 * 受限名单扩展信息清单接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "LimitListExtendRecord")
public class LimitListExtendRecordServiceImpl implements LimitListExtendRecordService {

    @Autowired
    private LimitListExtendRecordDao limitListExtendRecordDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public LimitListExtendRecordDao getRepository() {
        return limitListExtendRecordDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public LimitListExtendRecord get(String id) {
        Optional<LimitListExtendRecord> entity = getRepository().findById(id);
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
    public LimitListExtendRecord save(LimitListExtendRecord entity) {
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
    public LimitListExtendRecord update(LimitListExtendRecord entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(LimitListExtendRecord entity) {
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
    public Iterable<LimitListExtendRecord> saveOrUpdateAll(Iterable<LimitListExtendRecord> entities) {
        List<LimitListExtendRecord> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (LimitListExtendRecord entity:entities){
            redisKeys.add("LimitListExtendRecord::" + entity.getId());
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
        LimitListExtendRecordDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<LimitListExtendRecord> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            redisKeys.add("LimitListExtendRecord::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<LimitListExtendRecord> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (LimitListExtendRecord entity:entities){
            redisKeys.add("LimitListExtendRecord::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<LimitListExtendRecord> findByCondition(LimitListExtendRecord limitListExtendRecord, SearchVo searchVo, Pageable pageable) {

        return limitListExtendRecordDao.findAll(new Specification<LimitListExtendRecord>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<LimitListExtendRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> appidField=root.get("appid");

                List<Predicate> list = new ArrayList<Predicate>();

                list.add(cb.equal(appidField, limitListExtendRecord.getAppid()));
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
    public List<LimitListExtendRecord> findByListIdAndRecordId(String listId, String recordId) {
        return getRepository().findByListIdAndRecordId(listId,recordId);
    }

    @Override
    public LimitListExtendRecord findByRecordIdAndMetaTitle(String recordId, String metaTitle) {
        return getRepository().findByRecordIdAndMetaTitle(recordId,metaTitle);
    }

    @Override
    public List<LimitListExtendRecord> findByListIdAndIdentifierIn(String id, List<String> identifiers) {
        return getRepository().findByListIdAndIdentifierIn( id,identifiers);
    }

    @Override
    public List<LimitListExtendRecord> findByListId(String listId) {
        return getRepository().findByListId(listId);
    }

    @Override
    public void deleteByListIdAndIdentifier(String listId, String identifier) {
        getRepository().deleteByListIdAndIdentifier(listId,identifier);
    }
}