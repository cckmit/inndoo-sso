package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.core.dao.AccountFormMetaDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AccountFormMetaMapper;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import com.ytdinfo.inndoo.modules.core.service.AccountFormMetaService;
import com.ytdinfo.inndoo.modules.core.service.AchieveListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
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
 * 动态表单控件配置信息接口实现
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "AccountFormMeta")
public class AccountFormMetaServiceImpl implements AccountFormMetaService {

    @Autowired
    private AccountFormMetaDao formMetaDataDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AccountFormMetaMapper accountFormMetaMapper;
    @Autowired
    private AchieveListService achieveListService;

    @Override
    public AccountFormMetaDao getRepository() {
        return formMetaDataDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    @Override
    public AccountFormMeta get(String id) {
        Optional<AccountFormMeta> entity = getRepository().findById(id);
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
    public AccountFormMeta save(AccountFormMeta entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @Override
    public AccountFormMeta update(AccountFormMeta entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    @Override
    public void delete(AccountFormMeta entity) {
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
    public Iterable<AccountFormMeta> saveOrUpdateAll(Iterable<AccountFormMeta> entities) {
        List<AccountFormMeta> list = getRepository().saveAll(entities);
        return list;
    }

    /**
     * 根据Id批量删除
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        AccountFormMetaDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<AccountFormMeta> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
    }

    /**
     * 批量删除
     * @param entities
     */

    @Override
    public void delete(Iterable<AccountFormMeta> entities) {
        getRepository().deleteAll(entities);
    }

    @Override
    public Page<AccountFormMeta> findByCondition(AccountFormMeta formMetaData, SearchVo searchVo, Pageable pageable) {

        return formMetaDataDao.findAll(new Specification<AccountFormMeta>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<AccountFormMeta> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
    public void deleteByAccountFormId(String accountFormId) {
        accountFormMetaMapper.deleteByAccountFormId(accountFormId);
    }

    @Override
    @Cacheable(key = "'MetaList:Identifier:'+#accountFormId")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    public List<AccountFormMeta> findListByAccountFormId(String accountFormId) {
        return formMetaDataDao.findByAccountFormIdAndIsRequiredIsTrue(accountFormId);
    }


    // 根据注册页 和配置类型 获取注册配置
    @Override
    public List<AccountFormMeta> findByAccountFormIdAndMetaType(String accountFormId,String metaType) {
        return formMetaDataDao.findByAccountFormIdAndMetaType(accountFormId,metaType);
    }



    @Override
    public List<AccountFormMeta> findFormMetaListByIds(List<String> ids) {
        return formMetaDataDao.findAllById(ids);
    }

    @Override
    @Cacheable(key = "'NameList:'+#listId")
    @CacheExpire(expire = CommonConstant.SECOND_1MONTH)
    public List<AccountFormMeta> findByNameList(String listId, List<String> ids) {
        RedisUtil.addKeyToStore(RedisKeyStoreType.AccountFormMeta_NameList.getPrefixKey(),RedisKeyStoreType.AccountFormMeta_NameList.getPrefixKey()+listId);
        return formMetaDataDao.findAllById(ids);
    }


}