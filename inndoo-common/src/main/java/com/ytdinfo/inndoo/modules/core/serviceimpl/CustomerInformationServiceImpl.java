package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Dict;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.base.service.DictService;
import com.ytdinfo.inndoo.modules.core.dao.CustomerInformationDao;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformation;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformationExtend;
import com.ytdinfo.inndoo.modules.core.service.AccountFormService;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationExtendService;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ICustomerInformationExtendService;
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
import java.util.stream.Collectors;

/**
 * 客户信息表接口实现
 * @author yaochangning
 */
@Slf4j
@Service
//@CacheConfig(cacheNames = "CustomerInformation")
public class CustomerInformationServiceImpl implements CustomerInformationService {

    @Autowired
    private CustomerInformationDao customerInformationDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DictService dictService;
    @Autowired
    private DictDataService dictDataService;
    @Autowired
    private CustomerInformationExtendService customerInformationExtendService;

    @Autowired
    private AccountFormService accountFormService;

    @Override
    public CustomerInformationDao getRepository() {
        return customerInformationDao;
    }

    /**
     * 根据ID获取
     * @param id
     * @return
     */
    //@Cacheable(key = "#id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public CustomerInformation get(String id) {
        CustomerInformation customerInformation = new CustomerInformation();
        Optional<CustomerInformation> entity = getRepository().findById(id);
        if(!entity.isPresent()){
            return null;
        }
        customerInformation = entity.get();
        List<CustomerInformationExtend> customerInformationExtends = customerInformationExtendService.findByCustomerInformationId(id);
        customerInformation.setCustomerInformationExtends(customerInformationExtends);
        return customerInformation;
    }

    /**
     * 保存
     * @param entity
     * @return
     */
    //@CachePut(key = "#entity.id")
    //@CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public CustomerInformation save(CustomerInformation entity) {
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
    public CustomerInformation update(CustomerInformation entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     * @param entity
     */
    //@CacheEvict(key = "#entity.id")
    @Override
    public void delete(CustomerInformation entity) {
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
    public Iterable<CustomerInformation> saveOrUpdateAll(Iterable<CustomerInformation> entities) {
        List<CustomerInformation> list = getRepository().saveAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (CustomerInformation entity:entities){
        //    redisKeys.add("CustomerInformation::" + entity.getId());
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
        CustomerInformationDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<CustomerInformation> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        //List<String> redisKeys = new ArrayList<>();
        for (String id:ids){
            customerInformationExtendService.deleteByCustomerInformationId(id);
        }
        //redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<CustomerInformation> entities) {
        getRepository().deleteAll(entities);
        //List<String> redisKeys = new ArrayList<>();
        //for (CustomerInformation entity:entities){
        //    redisKeys.add("CustomerInformation::" + entity.getId());
        //}
        //redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<CustomerInformation> findByCondition(CustomerInformation customerInformation, SearchVo searchVo, Pageable pageable) {

        return customerInformationDao.findAll(new Specification<CustomerInformation>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<CustomerInformation> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField=root.get("createTime");

                Path<String> phoneField=root.get("phone");
                Path<String> nameField=root.get("name");
                List<Predicate> list = new ArrayList<Predicate>();

                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                if(StrUtil.isNotBlank(customerInformation.getName())){
                    list.add(cb.like(nameField,"%"+customerInformation.getName()+ "%" ));
                }
                if(StrUtil.isNotBlank(customerInformation.getPhone())){
                    list.add(cb.like(phoneField,"%"+customerInformation.getPhone()+ "%" ));
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
    public List<DictData> findDictData() {
        //获取客户信息拓展字段
        Dict dict = dictService.findByType("customerInformationExtend");
        if(null != dict ){
            List<DictData> dictDatas = dictDataService.findByDictId(dict.getId());
            return dictDatas;
        }
        return null;
    }

    @Override
    public CustomerInformation findByIdentifier(String identifier) {
        CustomerInformation customerInformation = customerInformationDao.findByIdentifierAndAppid(identifier,UserContext.getAppid());
        if(null != customerInformation){
            List<CustomerInformationExtend> customerInformationExtends = customerInformationExtendService.findByCustomerInformationId(customerInformation.getId());
            customerInformation.setCustomerInformationExtends(customerInformationExtends);
        }
        return customerInformation;
    }

    /**
     * 生成会员唯一标识
     *
     * @param customerInformation
     * @return
     */
    @Override
    public Result<String> getIdentifier(CustomerInformation customerInformation) {
        String identifier = "";

        //获取身份识别表单
        Boolean isIdentifierForm = true;
        AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(), isIdentifierForm);
        //获取初始的用户标识的注册页控件列表
        List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
        Map dentifierMap = new HashMap<>();
        if (null != IsIdentifierFormMetas && IsIdentifierFormMetas.size() > 0) {
            for (AccountFormMeta accountFormMeta : IsIdentifierFormMetas) {
                if (accountFormMeta.getIsStandard()) {
                    Object object = ReflectUtil.getFieldValue(customerInformation, accountFormMeta.getMetaType());
                    if(null == object){
                        String title = accountFormMeta.getTitle();
                        List<CustomerInformationExtend> customerInformationExtends = customerInformation.getCustomerInformationExtends();
                        if(CollectionUtil.isNotEmpty(customerInformationExtends)){
                            List<CustomerInformationExtend> selectCustomerInformationExtends =
                                    customerInformationExtends.stream().filter(it -> it.getTitle().equals(title)).collect(Collectors.toList());
                            if(CollectionUtil.isNotEmpty(selectCustomerInformationExtends)){
                                if(StrUtil.isNotBlank(selectCustomerInformationExtends.get(0).getValue().trim())){
                                    dentifierMap.put(accountFormMeta.getId(), selectCustomerInformationExtends.get(0).getValue().trim());
                                }else {
                                    return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                                }
                            }else {
                                return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                            }
                        }else {
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }
                    }else {
                        if(StrUtil.isNotBlank(object.toString().trim())){
                            dentifierMap.put(accountFormMeta.getId(), object.toString().trim());
                        }else {
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }

                    }
                }
                if (!accountFormMeta.getIsStandard()) {
                    String title = accountFormMeta.getTitle();
                    if(title.equals("分行编码")){
                        Object object = ReflectUtil.getFieldValue(customerInformation, "bankBranchNo");
                        if(null == object){
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }else {
                            if(StrUtil.isNotBlank(object.toString())){
                                dentifierMap.put(accountFormMeta.getId(), object.toString().trim());
                            } else {
                                return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                            }
                        }
                    }
                    if(title.equals("分行名称")){
                        Object object = ReflectUtil.getFieldValue(customerInformation, "bankBranchName");
                        if(null == object){
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }else {
                            if(StrUtil.isNotBlank(object.toString())){
                                dentifierMap.put(accountFormMeta.getId(), object.toString().trim());
                            } else {
                                return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                            }
                        }
                    }
                    if(title.equals("机构编号")){
                        Object object = ReflectUtil.getFieldValue(customerInformation, "institutionalCode");
                        if(null == object){
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }else {
                            if(StrUtil.isNotBlank(object.toString())){
                                dentifierMap.put(accountFormMeta.getId(), object.toString().trim());
                            } else {
                                return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                            }
                        }
                    }
                    if(title.equals("机构名称")){
                        Object object = ReflectUtil.getFieldValue(customerInformation, "institutionalName");
                        if(null == object){
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }else {
                            if(StrUtil.isNotBlank(object.toString())){
                                dentifierMap.put(accountFormMeta.getId(), object.toString().trim());
                            } else {
                                return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                            }
                        }
                    }
                    if(title.equals("客群编码")){
                        Object object = ReflectUtil.getFieldValue(customerInformation, "customerGroupCoding");
                        if(null == object){
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }else {
                            if(StrUtil.isNotBlank(object.toString())){
                                dentifierMap.put(accountFormMeta.getId(), object.toString().trim());
                            } else {
                                return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                            }
                        }
                    }

                    List<CustomerInformationExtend> customerInformationExtends = customerInformation.getCustomerInformationExtends();
                    if(CollectionUtil.isNotEmpty(customerInformationExtends)){
                        List<CustomerInformationExtend> selectCustomerInformationExtends =
                                customerInformationExtends.stream().filter(it -> it.getTitle().equals(title)).collect(Collectors.toList());
                        if(CollectionUtil.isNotEmpty(selectCustomerInformationExtends)){
                            if(StrUtil.isNotBlank(selectCustomerInformationExtends.get(0).getValue().trim())){
                                dentifierMap.put(accountFormMeta.getId(), selectCustomerInformationExtends.get(0).getValue().trim());
                            }else {
                                return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                            }
                        }else {
                            return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                        }
                    }else {
                        return new ResultUtil<String>().setErrorMsg("用户识别控件"+ accountFormMeta.getTitle() +"excel必须要有填写信息");
                    }

                }
            }
            identifier = SecureUtil.signParams(DigestAlgorithm.MD5, dentifierMap, "&", "=", true);

        } else {
            return new ResultUtil<String>().setErrorMsg("必须有用户识别控件");
        }
        return new ResultUtil<String>().setData(identifier);
    }

    @Override
    public List<CustomerInformation> findBatchByfindByIdentifiers(List<String> identifiers, int num) {
        List<CustomerInformation> allCustomerInformations = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(identifiers)) {
            num = (num > 0) ? num : 10000;
            int length = identifiers.size();
            if (length <= num) {
                allCustomerInformations = getRepository().findByAppidAndIdentifierIn(UserContext.getAppid(),identifiers);
            } else {
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = identifiers.subList(i * num, (i + 1) * num);
                    if (CollectionUtil.isNotEmpty(temp)) {
                        List<CustomerInformation> selectCustomerInformations = getRepository().findByAppidAndIdentifierIn(UserContext.getAppid(),temp);
                        if (CollectionUtil.isNotEmpty(selectCustomerInformations)) {
                            allCustomerInformations.addAll(selectCustomerInformations);
                        }
                    }
                }
                List<String> temp1 = identifiers.subList(times * num, length);
                if (CollectionUtil.isNotEmpty(temp1)) {
                    List<CustomerInformation> selectCustomerInformations = getRepository().findByAppidAndIdentifierIn(UserContext.getAppid(),temp1);
                    if (CollectionUtil.isNotEmpty(selectCustomerInformations)) {
                        allCustomerInformations.addAll(selectCustomerInformations);
                    }
                }
            }
        }
        return allCustomerInformations;
    }


}