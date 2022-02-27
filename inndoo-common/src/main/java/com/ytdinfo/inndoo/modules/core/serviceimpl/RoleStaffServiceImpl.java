package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.RoleStaffDao;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.service.RoleStaffService;
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
 * 角色（员工）接口实现
 *
 * @author Nolan
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "RoleStaff")
public class RoleStaffServiceImpl implements RoleStaffService {

    @Autowired
    private RoleStaffDao roleStaffDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public RoleStaffDao getRepository() {
        return roleStaffDao;
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public RoleStaff get(String id) {
        Optional<RoleStaff> entity = getRepository().findById(id);
        if (entity.isPresent()) {
            return entity.get();
        }
        return null;
    }

    /**
     * 保存
     *
     * @param entity
     * @return
     */
    //@CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public RoleStaff save(RoleStaff entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    //@CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public RoleStaff update(RoleStaff entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     *
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(RoleStaff entity) {
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
    //@CacheEvict(key = "#id")
    @Override
    public void delete(String id) {
        getRepository().deleteById(id);
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<RoleStaff> saveOrUpdateAll(Iterable<RoleStaff> entities) {
        List<RoleStaff> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (RoleStaff entity:entities){
        //    redisKeys.add("RoleStaff::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String[] ids) {
        RoleStaffDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<RoleStaff> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        //for (String id:ids){
        //    redisKeys.add("RoleStaff::" + id);
        //}
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     *
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<RoleStaff> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (RoleStaff entity:entities){
        //    redisKeys.add("RoleStaff::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<RoleStaff> findByCondition(RoleStaff roleStaff, SearchVo searchVo, Pageable pageable) {

        return roleStaffDao.findAll(new Specification<RoleStaff>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<RoleStaff> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField = root.get("createTime");

                List<Predicate> list = new ArrayList<Predicate>();

                //创建时间
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }


    /**
     * 获取默认角色
     *
     * @param defaultRole
     * @return
     */
    @Override
    public List<RoleStaff> findByDefaultRole(Boolean defaultRole) {
        return roleStaffDao.findByDefaultRole(defaultRole);
    }


    @Override
    public List<RoleStaff> findByIdIn(List<String> ids) {
        return roleStaffDao.findByIdIn(ids);
    }

    @Override
    public List<RoleStaff> findByNameIn(List<String> names) {
        return roleStaffDao.findByNameIn(names);
    }

    @Override
    public Map<String, RoleStaff> getIdMap() {
        List<RoleStaff> roles = findAll();
        Map<String, RoleStaff> staffMap = new HashMap<>();
        for (RoleStaff roleStaff : roles) {
            staffMap.put(roleStaff.getId(), roleStaff);
        }
        return staffMap;
    }

    @Override
    public Map<String, RoleStaff> getNameMap() {
        List<RoleStaff> roles = findAll();
        Map<String, RoleStaff> staffMap = new HashMap<>();
        for (RoleStaff roleStaff : roles) {
            staffMap.put(roleStaff.getName(), roleStaff);
        }
        return staffMap;
    }



    @Override
    public List<RoleStaff> findByName(String name) {
        return roleStaffDao.findByName(name);
    }

    @Override
    public List<RoleStaff> findByCode(String code) {
        return roleStaffDao.findByCode(code);
    }


}