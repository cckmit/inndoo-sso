package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.modules.core.dao.CustomerInformationExtendDao;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformationExtend;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationExtendService;
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
 * 客户信息拓展表接口实现
 * @author yaochangning
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "CustomerInformationExtend")
public class CustomerInformationExtendServiceImpl implements CustomerInformationExtendService {

    @Autowired
    private CustomerInformationExtendDao customerInformationExtendDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public CustomerInformationExtendDao getRepository() {
        return customerInformationExtendDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public CustomerInformationExtend get(String id) {
        Optional<CustomerInformationExtend> entity = getRepository().findById(id);
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
    public CustomerInformationExtend save(CustomerInformationExtend entity) {
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
    public CustomerInformationExtend update(CustomerInformationExtend entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(CustomerInformationExtend entity) {
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
    public Iterable<CustomerInformationExtend> saveOrUpdateAll(Iterable<CustomerInformationExtend> entities) {
        List<CustomerInformationExtend> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (CustomerInformationExtend entity:entities){
        //    redisKeys.add("CustomerInformationExtend::" + entity.getId());
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
        CustomerInformationExtendDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<CustomerInformationExtend> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        //for (String id:ids){
        //    redisKeys.add("CustomerInformationExtend::" + id);
        //}
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<CustomerInformationExtend> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (CustomerInformationExtend entity:entities){
        //    redisKeys.add("CustomerInformationExtend::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<CustomerInformationExtend> findByCondition(CustomerInformationExtend customerInformationExtend, SearchVo searchVo, Pageable pageable) {

        return customerInformationExtendDao.findAll(new Specification<CustomerInformationExtend>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<CustomerInformationExtend> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
    public List<CustomerInformationExtend> findByCustomerInformationId(String customerInformationId) {
        return customerInformationExtendDao.findByCustomerInformationIdAndAppid(customerInformationId,UserContext.getAppid());
    }

    @Override
    public Integer deleteByCustomerInformationId(String customerInformationId) {
        return customerInformationExtendDao.deleteByCustomerInformationId(customerInformationId);
    }
}