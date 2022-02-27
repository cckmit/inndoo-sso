package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.ApiCostTypeConstant;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.constant.SettingConstant;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.base.vo.ProxySetting;
import com.ytdinfo.inndoo.modules.core.dao.PhoneLocationDao;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import com.ytdinfo.inndoo.modules.core.service.PhoneLocationService;
import com.ytdinfo.model.request.PhoneLocationRequest;
import com.ytdinfo.model.response.PhoneLocationResponse;
import com.ytdinfo.util.APIRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.*;

/**
 * 手机号码归属地接口实现
 *
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "PhoneLocation")
public class PhoneLocationServiceImpl implements PhoneLocationService {

    @Autowired
    private PhoneLocationDao phoneLocationDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @XxlConf("wxapi.rooturl")
    private String wxApiRootUrl;

    @XxlConf("wxapi.aeskey")
    private String wxApiAesKey;

    @XxlConf("wxapi.appkey.phonelocation")
    private String phoneLocationAppKey;

    @Override
    public PhoneLocationDao getRepository() {
        return phoneLocationDao;
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    @Override
    public PhoneLocation get(String id) {
        Optional<PhoneLocation> entity = getRepository().findById(id);
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
    @CachePut(key = "#entity.phone")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    public PhoneLocation save(PhoneLocation entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.phone")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    public PhoneLocation update(PhoneLocation entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     *
     * @param entity
     */
    @CacheEvict(key = "#entity.phone")
    @Override
    public void delete(PhoneLocation entity) {
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
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
    public Iterable<PhoneLocation> saveOrUpdateAll(Iterable<PhoneLocation> entities) {
        List<PhoneLocation> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (PhoneLocation entity : entities) {
            redisKeys.add("PhoneLocation::" + entity.getPhone());
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        PhoneLocationDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<PhoneLocation> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (PhoneLocation entity : list4Delete) {
            redisKeys.add("PhoneLocation::" + entity.getPhone());
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * 批量删除
     *
     * @param entities
     */

    @Override
    public void delete(Iterable<PhoneLocation> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (PhoneLocation entity : entities) {
            redisKeys.add("PhoneLocation::" + entity.getPhone());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<PhoneLocation> findByCondition(PhoneLocation phoneLocation, SearchVo searchVo, Pageable pageable) {

        return phoneLocationDao.findAll(new Specification<PhoneLocation>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<PhoneLocation> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
     * 根据密文phone获取
     *
     * @param phone
     * @return
     */
    @Override
    @Cacheable(key = "#phone")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    public PhoneLocation getByPhone(String phone) {
        return getRepository().findByPhone(phone);
    }

    /**
     * 保存
     *
     * @param entity
     * @return
     */
    @Override
    @CachePut(key = "#entity.phone")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    public PhoneLocation saveByPhone(PhoneLocation entity) {
        return getRepository().save(entity);
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @Override
    @CachePut(key = "#entity.phone")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    public PhoneLocation updateByPhone(PhoneLocation entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * 删除
     *
     * @param entity
     */
    @Override
    @CacheEvict(key = "#entity.phone")
    public void deleteByPhone(PhoneLocation entity) {
        getRepository().delete(entity);
    }

    /**
     * @param phone 明文
     * @throws Exception
     */
    @Override
    public PhoneLocation getPhoneLocationFromApi(String phone) throws Exception {
        String aesPhone = AESUtil.encrypt(phone);
        PhoneLocationRequest phoneLocationRequest = new PhoneLocationRequest();
        phoneLocationRequest.setUrl(wxApiRootUrl);
        phoneLocationRequest.setAesKey(wxApiAesKey);
        phoneLocationRequest.setAppkey(phoneLocationAppKey);
        phoneLocationRequest.setPhone(phone);
        phoneLocationRequest.setCp("1");
        phoneLocationRequest.setUseDb(true);
        APIRequest<PhoneLocationRequest, PhoneLocationResponse> apiPhoneLocationRequest = new APIRequest<>();

        StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
        String v = redisTemplate.opsForValue().get(SettingConstant.PROXY_SETTING);
        ProxySetting setting = new Gson().fromJson(v, ProxySetting.class);
        PhoneLocationResponse phoneLocationResponse;
        Map<String,Object> proxyMap = HttpRequestUtil.getProxyMap(wxApiRootUrl);
        if(proxyMap != null){
            String host = proxyMap.get("host").toString();
            Integer port =  (Integer) proxyMap.get("port");
            phoneLocationResponse = apiPhoneLocationRequest.request(phoneLocationRequest, PhoneLocationResponse.class, HttpClientUtil.getHttpClient(), host,port);
        } else {
            phoneLocationResponse = apiPhoneLocationRequest.request(phoneLocationRequest, PhoneLocationResponse.class, HttpClientUtil.getHttpClient());
        }
        if (phoneLocationResponse.isSuccess()) {
            //调活动平台api接口，记录归属地校验接口费用
            activityApiUtil.noteApiCost(ApiCostTypeConstant.HOME_VERIFICATION);
            PhoneLocation phoneLocation = new PhoneLocation();
            phoneLocation.setPhone(aesPhone);
            phoneLocation.setProvince(phoneLocationResponse.getProvince());
            phoneLocation.setCity(phoneLocationResponse.getCity());
            phoneLocation.setCompany(phoneLocationResponse.getOperators());
            return phoneLocation;
        } else {
            throw new Exception(phoneLocationResponse.getErr_msg());
        }
    }

}