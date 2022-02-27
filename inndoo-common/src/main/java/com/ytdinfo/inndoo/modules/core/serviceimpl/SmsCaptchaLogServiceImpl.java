package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.core.dao.SmsCaptchaLogDao;
import com.ytdinfo.inndoo.modules.core.entity.SmsCaptchaLog;
import com.ytdinfo.inndoo.modules.core.service.SmsCaptchaLogService;
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
import java.sql.Struct;
import java.util.*;

/**
 * 手机短信验证码日志接口实现
 * @author Nolan
 */
@Slf4j
@Service
public class SmsCaptchaLogServiceImpl implements SmsCaptchaLogService {

    @Autowired
    private SmsCaptchaLogDao smsCaptchaLogDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public SmsCaptchaLogDao getRepository() {
        return smsCaptchaLogDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Override
    public SmsCaptchaLog get(String id) {
        Optional<SmsCaptchaLog> entity = getRepository().findById(id);
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
    @Override
    public SmsCaptchaLog save(SmsCaptchaLog entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @Override
    public SmsCaptchaLog update(SmsCaptchaLog entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @Override
    public void delete(SmsCaptchaLog entity) {
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     * @param id
     */
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
    public Iterable<SmsCaptchaLog> saveOrUpdateAll(Iterable<SmsCaptchaLog> entities) {
        List<SmsCaptchaLog> list = getRepository().saveAll(entities);
        return list;
    }

    /**
     * 根据Id批量删除
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String[] ids) {
        SmsCaptchaLogDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<SmsCaptchaLog> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<SmsCaptchaLog> entities) {
        getRepository().deleteAll(entities);
    }

    @Override
    public Page<SmsCaptchaLog> findByCondition(SmsCaptchaLog smsCaptchaLog, SearchVo searchVo, Pageable pageable) {

        return smsCaptchaLogDao.findAll(new Specification<SmsCaptchaLog>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<SmsCaptchaLog> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> phoneField=root.get("phone");
                Path<String> appidField=root.get("appid");
                Path<String> tenantIdField=root.get("tenantId");
                Path<Integer> sendStatusField=root.get("sendStatus");

                List<Predicate> list = new ArrayList<Predicate>();

                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                if(StrUtil.isNotBlank(smsCaptchaLog.getPhone())){
                    list.add(cb.equal(phoneField,smsCaptchaLog.getPhone()));
                }
                if(StrUtil.isNotBlank(smsCaptchaLog.getAppid())){
                    list.add(cb.equal(appidField,smsCaptchaLog.getAppid()));
                }
                if(StrUtil.isNotBlank(smsCaptchaLog.getTenantId())){
                    list.add(cb.equal(tenantIdField,smsCaptchaLog.getTenantId()));
                }
                if(Objects.nonNull(smsCaptchaLog.getSendStatus())){
                    list.add(cb.equal(sendStatusField,smsCaptchaLog.getSendStatus()));
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