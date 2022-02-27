package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.StaffRoleDao;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import com.ytdinfo.inndoo.modules.core.service.StaffRoleService;
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
 * 员工-角色接口实现
 * @author Nolan
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "StaffRole")
public class StaffRoleServiceImpl implements StaffRoleService {

    @Autowired
    private StaffRoleDao staffRoleDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public StaffRoleDao getRepository() {
        return staffRoleDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public StaffRole get(String id) {
        Optional<StaffRole> entity = getRepository().findById(id);
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
    public StaffRole save(StaffRole entity) {
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
    public StaffRole update(StaffRole entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(StaffRole entity) {
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
    public Iterable<StaffRole> saveOrUpdateAll(Iterable<StaffRole> entities) {
        List<StaffRole> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (StaffRole entity:entities){
        //    redisKeys.add("StaffRole::" + entity.getId());
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
        StaffRoleDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<StaffRole> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        //for (String id:ids){
        //    redisKeys.add("StaffRole::" + id);
        //}
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<StaffRole> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (StaffRole entity:entities){
        //    redisKeys.add("StaffRole::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }


    /**
     * 删除
     * @param staffId
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByStaffId(String staffId) {
        getRepository().deleteByStaffId(staffId);
        //List<String> redisKeys = new ArrayList<>();
        //for (StaffRole entity:entities){
        //    redisKeys.add("StaffRole::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<StaffRole> findByCondition(StaffRole staffRole, SearchVo searchVo, Pageable pageable) {

        return staffRoleDao.findAll(new Specification<StaffRole>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<StaffRole> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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


    /**
     * 根据角色查询绑定关系
     * @param roleId
     * @return
     */
    @Override
    public  List<StaffRole> findByRoleId(String roleId) {
        List<StaffRole> list = getRepository().findByRoleId(roleId);
        //List<String> redisKeys = new ArrayList<>();
        //for (StaffRole entity:entities){
        //    redisKeys.add("StaffRole::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
        return list;
    }

    @Override
    public StaffRole findByRoleIdAndStaffId(String roleId, String staffId) {
        return staffRoleDao.findByStaffIdAndRoleId(staffId,roleId);
    }

}