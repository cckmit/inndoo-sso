package com.ytdinfo.inndoo.modules.base.serviceimpl;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.modules.base.dao.DictDataDao;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.base.service.DictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 字典数据接口实现
 *
 * @author Exrick
 */
@Slf4j
@Service
public class DictDataServiceImpl implements DictDataService {

    public static final String SMS_SIGNATURE = "smsSignature";

    @Autowired
    private DictService dictService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, DictData> redisTemplate;

    @Autowired
    private DictDataDao dictDataDao;

    @Override
    public DictDataDao getRepository() {
        return dictDataDao;
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    @Override
    public DictData get(String id) {
        DictData dictData = redisTemplate.opsForValue().get("dictData::" + id);
        if (dictData == null) {
            Optional<DictData> entity = getRepository().findById(id);
            if (entity.isPresent()) {
                dictData = entity.get();
                redisTemplate.opsForValue().set("dictData::" + id, dictData);
                return dictData;
            }
        }
        return dictData;
    }

    @Override
    public Page<DictData> findByCondition(DictData dictData, Pageable pageable) {

        return dictDataDao.findAll(new Specification<DictData>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<DictData> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                Path<String> titleField = root.get("title");
                Path<Integer> statusField = root.get("status");
                Path<String> dictIdField = root.get("dictId");

                List<Predicate> list = new ArrayList<Predicate>();

                //模糊搜素
                if (StrUtil.isNotBlank(dictData.getTitle())) {
                    list.add(cb.like(titleField, '%' + dictData.getTitle() + '%'));
                }

                //状态
                if (dictData.getStatus() != null) {
                    list.add(cb.equal(statusField, dictData.getStatus()));
                }

                //所属字典
                if (StrUtil.isNotBlank(dictData.getDictId())) {
                    list.add(cb.equal(dictIdField, dictData.getDictId()));
                }

                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    @Override
    public List<DictData> findByDictId(String dictId) {

        return dictDataDao.findByDictIdAndStatusOrderBySortOrder(dictId, CommonConstant.STATUS_NORMAL);
    }

    @Override
    public void deleteByDictId(String dictId) {

        dictDataDao.deleteByDictId(dictId);
    }

    @Override
    public String findSmsSignatureByAppid(String appid) {
        //先查缓存
        String smsSignature = stringRedisTemplate.opsForValue().get(SMS_SIGNATURE + ":" + appid);
        if (StrUtil.isNotBlank(smsSignature)) {
            return smsSignature;
        } else {
            String dictId = dictService.findByType(SMS_SIGNATURE).getId();
            DictData byDictIdAndTitle = dictDataDao.findByDictIdAndTitle(dictId, appid);
            String newSmsSignature = "【盈天地】";
            if (null != byDictIdAndTitle) {
                newSmsSignature = byDictIdAndTitle.getValue();
            }
            //缓存没有，查数据库后，往缓存再放一次
            stringRedisTemplate.opsForValue().set(SMS_SIGNATURE + ":" + appid, newSmsSignature);
            return newSmsSignature;
        }
    }

    @Override
    public List<DictData> getByValueAndDictId(String value, String dictId) {
        return dictDataDao.findByValueAndDictId(value, dictId);
    }

    @Override
    public DictData findByTitle(String title) {
        String cacheKey = "dictData::" + title;
        DictData dictData = redisTemplate.opsForValue().get(cacheKey);
        if (dictData == null) {
            dictData = dictDataDao.findFirstByTitle(title);
            if (dictData == null) {
                dictData = new DictData();
            }
            redisTemplate.opsForValue().set(title, dictData);
        }
        return dictData;
    }

    @Override
    public Boolean deleteByTitle(String title) {
        String cacheKey = "dictData::" + title;
        return redisTemplate.delete(cacheKey);
    }

}