package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.DecryptAccountDao;
import com.ytdinfo.inndoo.modules.core.entity.DecryptAccount;
import com.ytdinfo.inndoo.modules.core.service.DecryptAccountService;
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
 * 账户解密信息接口实现
 * @author cnyao
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "DecryptAccount")
public class DecryptAccountServiceImpl implements DecryptAccountService {

    @Autowired
    private DecryptAccountDao decryptAccountDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public DecryptAccountDao getRepository() {
        return decryptAccountDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public DecryptAccount get(String id) {
        Optional<DecryptAccount> entity = getRepository().findById(id);
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
    public DecryptAccount save(DecryptAccount entity) {
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
    public DecryptAccount update(DecryptAccount entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(DecryptAccount entity) {
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
    public Iterable<DecryptAccount> saveOrUpdateAll(Iterable<DecryptAccount> entities) {
        List<DecryptAccount> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (DecryptAccount entity:entities){
        //    redisKeys.add("DecryptAccount::" + entity.getId());
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
        DecryptAccountDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<DecryptAccount> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        //for (String id:ids){
        //    redisKeys.add("DecryptAccount::" + id);
        //}
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<DecryptAccount> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (DecryptAccount entity:entities){
        //    redisKeys.add("DecryptAccount::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<DecryptAccount> findByCondition(DecryptAccount decryptAccount, SearchVo searchVo, Pageable pageable) {

        return decryptAccountDao.findAll(new Specification<DecryptAccount>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<DecryptAccount> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
    public DecryptAccount findByCoreAccountId(String coreAccountId) {
        return decryptAccountDao.findByCoreAccountId(coreAccountId);
    }
}