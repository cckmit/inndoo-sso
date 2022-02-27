package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.dao.AccountFormFieldDao;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormField;
import com.ytdinfo.inndoo.modules.core.service.AccountFormFieldService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 会员注册扩展表单内容接口实现
 * @author Timmy
 */
@Slf4j
@Service
public class AccountFormFieldServiceImpl implements AccountFormFieldService {

    @Autowired
    private AccountFormFieldDao accountFormFieldDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public AccountFormFieldDao getRepository() {
        return accountFormFieldDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Override
    public AccountFormField get(String id) {
        return getRepository().getOne(id);
    }

    /**
     * 保存
     * @param entity
     * @return
     */
    @Override
    public AccountFormField save(AccountFormField entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @Override
    public AccountFormField update(AccountFormField entity) {
        AccountFormField accountFormField = getRepository().saveAndFlush(entity);
        return accountFormField;
    }

    /**
     * 删除
     * @param entity
     */
    @Override
    public void delete(AccountFormField entity) {
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
    public Iterable<AccountFormField> saveOrUpdateAll(Iterable<AccountFormField> entities) {
        return getRepository().saveAll(entities);
    }

    /**
     * 根据Id批量删除
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        AccountFormFieldDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<AccountFormField> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<AccountFormField> entities) {
        getRepository().deleteAll(entities);
    }

    @Override
    public Page<AccountFormField> findByCondition(AccountFormField accountFormField, SearchVo searchVo, Pageable pageable) {

        return accountFormFieldDao.findAll(new Specification<AccountFormField>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<AccountFormField> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");
                Path<String> appidField=root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();

                list.add(cb.equal(appidField, accountFormField.getAppid()));
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
    public List<AccountFormField> findByAccountId(String accountId) {
        return getRepository().findByAccountId(accountId);
    }
    @Override
    public List<AccountFormField> findByAccountIdIn(List<String> accountId) {
        return getRepository().findByAccountIdIn(accountId);
    }

    @Override
    public List<AccountFormField> findByFieldDataAndMetaTitle(String fieldData, String metaTitle) {
        return getRepository().findByFieldDataAndMetaTitleAndAppid(fieldData,metaTitle,UserContext.getAppid());
    }

}