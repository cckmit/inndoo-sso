package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.Digester;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.LinkTypeConstant;
import com.ytdinfo.inndoo.common.constant.ListTypeConstant;
import com.ytdinfo.inndoo.common.constant.NameListType;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.EncryptionMethodType;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.NameListValidateResultVo;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.common.vo.WhiteListResultVo;
import com.ytdinfo.inndoo.modules.core.dao.WhiteListExtendRecordDao;
import com.ytdinfo.inndoo.modules.core.dao.WhiteListRecordDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.WhiteListExtendRecordMapper;
import com.ytdinfo.inndoo.modules.core.dao.mapper.WhiteListRecordMapper;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListExtendRecordService;
import com.ytdinfo.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.util.*;

/**
 * 白名单清单接口实现
 *
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "WhiteListRecord")
public class WhiteListRecordServiceImpl implements WhiteListRecordService {

    @Autowired
    private WhiteListRecordDao whiteListRecordDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private WhiteListService whiteListService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private AccountService accountService;
    @XxlConf("core.front.rooturl")
    private String coreRootUrl;
    @Autowired
    private WhiteListRecordService whiteListRecordService;
    @Autowired
    private WhiteListExtendRecordService whiteListExtendRecordService;
    @Autowired
    private LimitListRecordService limitListRecordService;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private IWhiteListExtendRecordService iWhiteListExtendRecordService;
    @Autowired
    private DictDataApiListService dictDataApiListService;
    @Autowired
    private WhiteListExtendRecordDao whiteListExtendRecordDao;
    @Autowired
    private WhiteListRecordMapper whiteListRecordMapper;
    @Autowired
    private WhiteListExtendRecordMapper whiteListExtendRecordMapper;
    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    @Override
    public WhiteListRecordDao getRepository() {
        return whiteListRecordDao;
    }


    @Override
    public long countByListId(String listId) {
        return getRepository().countByListId(listId);
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    @Override
    public WhiteListRecord get(String id) {
        Optional<WhiteListRecord> entity = getRepository().findById(id);
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
    @Override
    public WhiteListRecord save(WhiteListRecord entity) {
        WhiteListRecord whiteListRecord = getRepository().save(entity);
        String cacheKey = "WhiteListRecord:" + entity.getListId();
        redisTemplate.opsForHash().put(cacheKey, entity.getIdentifier(), entity.getTimes());
        return whiteListRecord;
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @Override
    public WhiteListRecord update(WhiteListRecord entity) {
        WhiteListRecord whiteListRecord = getRepository().saveAndFlush(entity);
        String cacheKey = "WhiteListRecord:" + entity.getListId();
        redisTemplate.opsForHash().put(cacheKey, entity.getIdentifier(), entity.getTimes());
        return whiteListRecord;
    }

    /**
     * 删除
     *
     * @param entity
     */
    @Override
    public void delete(WhiteListRecord entity) {
        getRepository().delete(entity);
        String cacheKey = "WhiteListRecord:" + entity.getListId();
        redisTemplate.opsForHash().delete(cacheKey, entity.getIdentifier());
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
    public Iterable<WhiteListRecord> saveOrUpdateAll(Iterable<WhiteListRecord> entities) {
        List<WhiteListRecord> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        String cacheKey = StrUtil.EMPTY;
        for (WhiteListRecord entity : entities) {
            cacheKey = "WhiteListRecord:" + entity.getListId();
            redisKeys.add(entity.getIdentifier());
        }
        redisTemplate.opsForHash().delete(cacheKey, redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        WhiteListRecordDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<WhiteListRecord> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        String cacheKey = StrUtil.EMPTY;
        for (WhiteListRecord entity : list4Delete) {
            cacheKey = "WhiteListRecord:" + entity.getListId();
            redisKeys.add(entity.getIdentifier());
        }
        redisTemplate.opsForHash().delete(cacheKey, redisKeys);
    }

    /**
     * 批量删除
     *
     * @param entities
     */

    @Override
    public void delete(Iterable<WhiteListRecord> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        String cacheKey = StrUtil.EMPTY;
        for (WhiteListRecord entity : entities) {
            cacheKey = "WhiteListRecord:" + entity.getListId();
            redisKeys.add(entity.getIdentifier());
        }
        redisTemplate.opsForHash().delete(cacheKey, redisKeys);
    }

    @Override
    public Page<WhiteListRecord> findByCondition(WhiteListRecord whiteListRecord, SearchVo searchVo, Pageable pageable, String whiteListId) {

        return whiteListRecordDao.findAll(new Specification<WhiteListRecord>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<WhiteListRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField = root.get("createTime");
                Path<String> listIdField = root.get("listId");
                Path<String> identifierField = root.get("identifier");
                Path<String> appidField = root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();
                list.add(cb.equal(appidField, whiteListRecord.getAppid()));
                //创建时间
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                //白名单id
                if (StrUtil.isNotBlank(whiteListId)) {
                    list.add(cb.equal(listIdField, whiteListId.trim()));
                }
                WhiteList whiteList = whiteListService.get(whiteListId);
                if (whiteList != null) {
                    if (!whiteList.getListType().equals(NameListType.ADVANCED)) {
                        //白名单用户标识
                        if (StrUtil.isNotBlank(whiteListRecord.getIdentifier())) {
                            String searchIdentifierField = whiteListRecord.getIdentifier();
                            if (NameListType.PHONE.equals(whiteList.getListType()) && whiteList.getIsEncryption().intValue() == 0) {
                                searchIdentifierField = AESUtil.encrypt(searchIdentifierField);
                                list.add(cb.equal(identifierField, searchIdentifierField));
                            } else {
                                list.add(cb.like(identifierField, "%" + StringUtils.trim(whiteListRecord.getIdentifier()) + "%"));
                            }
                        }
                    } else {
                        if (StrUtil.isNotBlank(whiteListRecord.getIdentifier())) {
                            list.add(cb.like(identifierField, "%" + StringUtils.trim(whiteListRecord.getIdentifier()) + "%"));
                        }
                    }
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
    public List<WhiteListRecord> findByIdentifiers(List<String> identifiers) {
        return getRepository().findByIdentifierIn(identifiers);
    }
    @Override
    public NameListValidateResultVo validateByCache(WhiteList whiteList, String identifier) {
        NameListValidateResultVo resultVo = new NameListValidateResultVo();
        String cacheKey = "WhiteListRecord:" + whiteList.getId();
        // 加密
        if (whiteList.getIsEncryption() == 1) {
            // 加盐
            String salt = whiteList.getDataSalt();
            if(whiteList.getIsDataAddSalt() == 1 && StrUtil.isNotEmpty(salt)){
                switch (whiteList.getDataSaltMethod()) {
                    case 0: {
                        //数据前后加盐
                        if(whiteList.getDataSaltPosition()==0){
                            identifier = salt + identifier;
                        }else if(whiteList.getDataSaltPosition()==1){
                            identifier = identifier + salt;
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }

            switch (whiteList.getEncryptionMethod()) {
                case 0: {
                    //MD5
                    identifier = MD5Util.md5(identifier);
                    break;
                }
                case 1: {
                    //AES
                    identifier = AESUtil.encrypt(identifier, whiteList.getEncryptionPassword());
                    break;
                }
                case 2: {
                    // SHA256
                    identifier = DigestUtil.sha256Hex(identifier);
                    break;
                }
                case 3: {
                    // SM3
                    identifier = SmUtil.sm3(identifier);
                    break;
                }
                default: {
                    break;
                }
            }
            identifier = identifier.toUpperCase();
        }

        Object value = redisTemplate.opsForHash().get(cacheKey, identifier);
        Integer times = -1;
        if (value != null) {
            times = Integer.parseInt(value.toString());
        }
        if(whiteList.getIsTimes().intValue() == 0){
            resultVo.setTimes(0);
        }else{
            resultVo.setTimes(times);
        }
        resultVo.setMatch(!times.equals(-1));
        resultVo.setFormId(whiteList.getFormId());
        String registerUrl = coreRootUrl + "/" + UserContext.getTenantId() + "/" + whiteList.getAppid() + "/user-login?id=" + whiteList.getFormId();
        resultVo.setRegisterUrl(registerUrl);
        return resultVo;
    }

    @Override
    public NameListValidateResultVo verify(String listId, String recordIdentifier,String openId) {
        WhiteList whiteList = whiteListService.get(listId);
        NameListValidateResultVo resultVo = new NameListValidateResultVo();
        resultVo.setMatch(false);
        resultVo.setTimes(0);
        if (whiteList == null) {
            return resultVo;
        }
        String linkId = whiteList.getLinkId();
        if (StrUtil.isNotEmpty(linkId)) {
            Byte linkType = whiteList.getLinkType();
            switch (linkType) {
                case LinkTypeConstant.WHITE:
                    return verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.LIMIT:
                    return limitListRecordService.verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.ACHIEVE:
                    return achieveListRecordService.verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.API:
                    return dictDataApiListService.verify(linkId, recordIdentifier, openId);
            }
        }
        //优先从缓存中查询
        resultVo = validateByCache(whiteList, recordIdentifier);
        if (resultVo.isMatch()) {
            return resultVo;
        }
        Integer listType = whiteList.getListType();
        if (NameListType.PHONE.equals(listType)) {
            Account account = accountService.get(recordIdentifier);
            if (account == null) {
                resultVo = new NameListValidateResultVo();
                resultVo.setMatch(false);
                return resultVo;
            }
            String phone = account.getPhone();
            if (whiteList.getIsEncryption() == 0) {
                phone = AESUtil.encrypt(phone);
                //phone = AESUtil.comEncrypt(phone);
            }
            resultVo = validateByCache(whiteList, phone);
            return resultVo;
        }
        if(NameListType.ACTACCOUNTID.equals(listType)){
            // 没有名单不包含资格次数的可以查名单中有无关联账户
            if ( whiteList.getIsTimes().intValue() == 0 ){
                ActAccount actAccount = actAccountService.findByActAccountId(recordIdentifier);
                if(null != actAccount){
                    List<ActAccount> actAccounts = actAccountService.findByCoreAccountId(actAccount.getCoreAccountId());
                    for(ActAccount actAcc:actAccounts ){
                        String actAccountId = actAcc.getActAccountId();
                        resultVo = validateByCache(whiteList, actAccountId);
                        if(resultVo.isMatch()){
                            return resultVo;
                        }
                    }
                    resultVo = new NameListValidateResultVo();
                    resultVo.setMatch(false);
                    return resultVo;
                }else {
                    resultVo = new NameListValidateResultVo();
                    resultVo.setMatch(false);
                    return resultVo;
                }
            }
        }
        //高级校验需要查询Account相关信息
        if (NameListType.ADVANCED.equals(listType)) {
            //高级校验字段清单
            String validateFields = whiteList.getValidateFields();
            String[] fields = validateFields.split(",");
            List<String> fieldIds = new ArrayList<>();
            CollectionUtil.addAll(fieldIds, fields);
            //包含缓存（第一次查询走数据库）
            List<AccountFormMeta> formMetas = accountFormMetaService.findByNameList(listId, fieldIds);
            Map map = new HashMap<>();
            //含缓存，并且应查询并初始化扩展信息
            Account account = accountService.get(recordIdentifier);
            List<AccountFormField> formFields = account.getAccountFormFields();
            for (AccountFormMeta meta : formMetas) {
                if (Boolean.TRUE.equals(meta.getIsStandard())) {
                    String fieldValue = ReflectUtil.getFieldValue(account, meta.getMetaType()).toString();
                    map.put(meta.getId(), AESUtil.encrypt(fieldValue));
                    //map.put(meta.getId(), AESUtil.comEncrypt(fieldValue));
                } else {
                    formFields.forEach(accountFormField -> {
                        if (meta.getId().equals(accountFormField.getMetaId())) {
                            map.put(meta.getId(), AESUtil.encrypt(accountFormField.getFieldData()));
                            //map.put(meta.getId(), AESUtil.comEncrypt(accountFormField.getFieldData()));
                        }
                    });
                }
            }
            String identifier = SecureUtil.signParams(DigestAlgorithm.MD5, map, "&", "=", true);
            if (StrUtil.isNotEmpty(identifier)) {
                resultVo = validateByCache(whiteList, identifier);
            }
        }
        return resultVo;
    }

    @Override
    public void removeCache(String listId, List<String> removeMd5s) {
        String cacheKey = "WhiteListRecord:" + listId;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        boundHashOperations.delete(removeMd5s);
    }

    @Override
    public void updateCacheTime(WhiteList whiteList) {
        String cacheKey = "WhiteListRecord:" + whiteList.getId();
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        boundHashOperations.expireAt(whiteList.getExpireDate());
    }

    @Override
    public void loadCache(String listId) {
        WhiteList whiteList = whiteListService.get(listId);
        if (whiteList == null) {
            return;
        }
        Date d1 = new Date();
        String cacheKey = "WhiteListRecord:" + listId;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        int pageSize = 5000;
        PageVo page = new PageVo();
        page.setPageNumber(0);
        page.setPageSize(pageSize);
        page.setOrder(Sort.Direction.ASC.name());
        page.setSort("id");
        Pageable pageable = PageUtil.initPage(page);
        String id = "0";
        while (true) {
            final String finalId = id;
            List<WhiteListRecord> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
                List<Predicate> list = new ArrayList<Predicate>();
                Path<String> listIdField = root.get("listId");
                Path<String> idField = root.get("id");
                list.add(criteriaBuilder.equal(listIdField, listId));
                list.add(criteriaBuilder.greaterThan(idField, finalId));
                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    query.where(list.toArray(arr));
                }
                return null;
            }, pageable);
            Map<String, Integer> map = new HashMap<>();
            for (WhiteListRecord record : listRecords) {
                if(ListTypeConstant.ADVANCED.equals(whiteList.getListType())){
                    List<WhiteListExtendRecord> listExtendRecords = whiteListExtendRecordService.findByListIdAndRecordId(listId, record.getId());
                    record.setExtendInfo(listExtendRecords);
                    Map mapFields = new HashMap<>();
                    for (String metaId : whiteList.getValidateFields().split(",")) {
                        if(StrUtil.isNotEmpty(metaId)){
                            WhiteListExtendRecord extendRecord = CollectionUtil.findOne(record.getExtendInfo(), whiteListExtendRecord -> whiteListExtendRecord.getFormMetaId().equals(metaId));
                            String extendRecordValue = extendRecord.getRecord();
                            mapFields.put(metaId,extendRecordValue);
                        }
                    }
                    String identifier = SecureUtil.signParams(DigestAlgorithm.MD5, mapFields, "&", "=", true);
                    map.put(identifier, record.getTimes());
                }else{
                    String identifier = record.getIdentifier();
                    map.put(identifier, record.getTimes());
                }
            }
            boundHashOperations.putAll(map);
            if (listRecords.size() != pageSize) {
                break;
            }
            id = listRecords.get(pageSize - 1).getId();
        }
        boundHashOperations.expireAt(whiteList.getExpireDate());
        Date d2 = new Date();
        System.out.println(d2.getTime() - d1.getTime());
    }

    public void loadSingleCache(String listId,WhiteListRecord record){
        WhiteList whiteList = whiteListService.get(listId);
        String cacheKey = "WhiteListRecord:" + listId;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        List<WhiteListExtendRecord> listExtendRecords = whiteListExtendRecordService.findByListIdAndRecordId(listId, record.getId());
        record.setExtendInfo(listExtendRecords);
        Map mapFields = new HashMap<>();
        Map<String, Integer> map = new HashMap<>();
        if(ListTypeConstant.ADVANCED.equals(whiteList.getListType())){
            for (String metaId : whiteList.getValidateFields().split(",")) {
                if(StrUtil.isNotEmpty(metaId)){
                    WhiteListExtendRecord extendRecord = CollectionUtil.findOne(record.getExtendInfo(), whiteListExtendRecord -> whiteListExtendRecord.getFormMetaId().equals(metaId));
                    String extendRecordValue = extendRecord.getRecord();
                    mapFields.put(metaId,extendRecordValue);
                }
            }
            String identifier = SecureUtil.signParams(DigestAlgorithm.MD5, mapFields, "&", "=", true);
            map.put(identifier, record.getTimes());
        }else{
            String identifier = record.getIdentifier();
            map.put(identifier, record.getTimes());
        }
        boundHashOperations.putAll(map);
        boundHashOperations.expireAt(whiteList.getExpireDate());
    }

    @Override
    public boolean existsByListIdAndIdentifier(String listId, String recordIdentifier) {
        return whiteListRecordDao.existsByListIdAndIdentifier(listId,recordIdentifier);
    }

    @Override
    public WhiteListRecord findByListIdAndId(String listId, String id) {
        return whiteListRecordDao.findByListIdAndId(listId,id);
    }

    @Override
    public WhiteListRecord findByListIdAndIdentifier(String listId, String identifier) {
        return whiteListRecordDao.findByListIdAndIdentifier(listId,identifier);
    }

    @Override
    public void handlePushListRecord(String listId, String record, String times) {
        //加个锁：listId + record
        String lockKey = "handlePushListRecord:" + listId + record;
        lockTemplate.execute(lockKey, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                WhiteList whiteList = whiteListService.get(listId);
                String identifier = record;
                if (whiteList.getIsEncryption() == 0 && whiteList.getListType() == 2) {
                    identifier = AESUtil.encrypt(identifier);
                }
                // 加密，转大写
                if(whiteList.getIsEncryption() == 1){
                    identifier = identifier.toUpperCase();
                }
                //先去缓存找，没有则塞DB、加缓冲；如果有则判断参数是否相等
                NameListValidateResultVo nameListValidateResultVo = whiteListRecordService.verify(listId, identifier, null);
                if (!nameListValidateResultVo.isMatch()) {
                    //缓存中没找到，塞DB，重新加载缓存
                    WhiteListRecord whiteListRecord = new WhiteListRecord();
                    whiteListRecord.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    whiteListRecord.setListId(whiteList.getId());
                    whiteListRecord.setCreateTime(new Date());
                    whiteListRecord.setUpdateTime(new Date());
                    whiteListRecord.setIdentifier(identifier);
                    if (StringUtils.isNotBlank(times))
                    {
                        whiteListRecord.setTimes(Integer.parseInt(times));
                    }
                    whiteListRecordService.save(whiteListRecord);
                    whiteListRecordService.loadSingleCache(whiteList.getId(), whiteListRecord);
                }
                return null;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                handlePushListRecord(listId, record, times);
                return null;
            }
        });
    }

    //接口加1调用，累计值，时时更新数据库和缓存
    @Override
    public void AddUpTimesPushListRecord(String listId, String record, String strtimes) {
        //加个锁：listId + record
        String lockKey = "AddUpTimesPushListRecord:" + listId + record;
        lockTemplate.execute(lockKey, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                WhiteList whiteList = whiteListService.get(listId);
                String identifier = record;
                if (whiteList.getIsEncryption() == 0 && whiteList.getListType() == 2) {
                    identifier = AESUtil.encrypt(identifier);
                }
                // 加密，转大写
                if (whiteList.getIsEncryption() == 1) {
                    identifier = identifier.toUpperCase();
                }
                //先去查数据库，有则累加更新，没有则直接加入
                String cacheKey = "WhiteListRecord:" + whiteList.getId();
                // 加密
                if (whiteList.getIsEncryption() == 1) {
                    switch (whiteList.getEncryptionMethod()) {
                        case 0: {
                            //MD5
                            identifier = MD5Util.md5(identifier);
                            break;
                        }
                        case 1: {
                            //AES
                            identifier = AESUtil.encrypt(identifier, whiteList.getEncryptionPassword());
                            break;
                        }
                        case 3: {
                            // SM3
                            identifier = SmUtil.sm3(identifier);
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                    identifier = identifier.toUpperCase();
                }
                Integer times = Integer.parseInt(strtimes);
                if (redisTemplate.opsForHash().hasKey(cacheKey, identifier)) {
                    Object value = redisTemplate.opsForHash().get(cacheKey, identifier);
                    Integer subtimes = 0;
                    if (value != null) {
                        subtimes = Integer.parseInt(value.toString());
                    }
                    redisTemplate.opsForHash().increment(cacheKey, identifier, subtimes);
                    //更新数据库
                    WhiteListRecord whiteListRecord = whiteListRecordService.findByListIdAndIdentifier(listId, identifier);
                    whiteListRecord.setTimes(whiteListRecord.getTimes() + times);
                    whiteListRecordService.save(whiteListRecord);
                } else {
                    //缓存中没找到，塞DB，重新加载缓存
                    WhiteListRecord whiteListRecord = new WhiteListRecord();
                    whiteListRecord.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    whiteListRecord.setListId(whiteList.getId());
                    whiteListRecord.setCreateTime(new Date());
                    whiteListRecord.setUpdateTime(new Date());
                    whiteListRecord.setIdentifier(identifier);
                    whiteListRecord.setTimes(times);
                    whiteListRecordService.save(whiteListRecord);
                    whiteListRecordService.loadSingleCache(whiteList.getId(), whiteListRecord);
                }
                return null;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                AddUpTimesPushListRecord(listId, record, strtimes);
                return null;
            }
        });
    }

    @Override
    public List<List<String>> toWrite(String listId) {
        List<List<String>> rows = new ArrayList<>();
        WhiteList whiteList = whiteListService.get(listId);
        if (whiteList == null) {
            return rows;
        }
        String listTypeName = "";
        if(whiteList.getListType() == 0){
            listTypeName = "高级校验";
        } else if (whiteList.getListType() == 1){
            listTypeName = "微信用户openid";
        }else if (whiteList.getListType() == 2){
            listTypeName = "手机号";
        }else if (whiteList.getListType() == 3){
            listTypeName = "小核心账户";
        }else if (whiteList.getListType() == 4){
            listTypeName = "活动平台账户";
        }else if (whiteList.getListType() == 5){
            listTypeName = "帐号openid";
        }
        Byte isTimes = whiteList.getIsTimes();
        int pageSize = 5000;
        PageVo page = new PageVo();
        page.setPageNumber(0);
        page.setPageSize(pageSize);
        page.setOrder(Sort.Direction.ASC.name());
        page.setSort("id");
        int listType = whiteList.getListType();
        String id = "0";
        Pageable pageable = PageUtil.initPage(page);
        if (NameListType.ADVANCED.equals(listType)) {
            String validateFields = whiteList.getValidateFields();
            if (StringUtils.isNotBlank(validateFields)) {
                String[] validateField = validateFields.split(",");
                List<String> headList = new ArrayList();
                List<AccountFormMeta> accountFormMetas = new ArrayList();
                for (String s : validateField) {
                    if (!StrUtil.isBlank(s)) {
                        AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                        if (formMeta != null) {
                            headList.add(formMeta.getTitle());
                            accountFormMetas.add(formMeta);
                        }
                    }
                }
                if(isTimes.intValue() == 1){
                    headList.add("资格次数");
                }
                headList.add("类型");
                headList.add("创建时间");
                headList.add("修改时间");
                rows.add(headList);
                Map<String, Object> findMap = new HashMap<>();
                findMap.put("formMetas", accountFormMetas);
                findMap.put("listId", listId);
                findMap.put("pageIndex", 0);
                findMap.put("pageSize", pageSize);
                while (true) {
                    final String finalId = id;
                    findMap.put("id", finalId);
                    List<Map<String, Object>> listRecords = iWhiteListExtendRecordService.findTransformDate(findMap);
                    for (Map<String, Object> record : listRecords) {
                        List<String> row = new ArrayList<>();
                        for (AccountFormMeta temp : accountFormMetas) {
                            Object column = record.get(temp.getMetaType());
                            row.add(column == null ? "" : AESUtil.decrypt(column.toString()));
                        }
                        if (isTimes.intValue() == 1 && record.get("times") != null) {
                            row.add(record.get("times").toString());
                        }
                        row.add(listTypeName);
                        row.add(record.get("create_time").toString());
                        row.add(record.get("update_time").toString());
                        rows.add(row);
                    }
                    if (listRecords.size() != pageSize) {
                        break;
                    }
                    id = listRecords.get(pageSize - 1).get("identifier").toString();
                }
            }
        } else {
            List<String> headList = new ArrayList<>();
            headList.add("用户识别码");
            if(isTimes.intValue() == 1){
                headList.add("资格次数");
            }
            headList.add("类型");
            headList.add("创建时间");
            headList.add("修改时间");
            rows.add(headList);
            while (true) {
                final String finalId = id;
                List<WhiteListRecord> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
                    List<Predicate> list = new ArrayList<Predicate>();
                    Path<String> listIdField = root.get("listId");
                    Path<String> idField = root.get("id");
                    list.add(criteriaBuilder.equal(listIdField, listId));
                    list.add(criteriaBuilder.greaterThan(idField, finalId));
                    Predicate[] arr = new Predicate[list.size()];
                    if (list.size() > 0) {
                        query.where(list.toArray(arr));
                    }
                    return null;
                }, pageable);
                for (WhiteListRecord record : listRecords) {
                    String identifier = record.getIdentifier();
                    if (NameListType.PHONE.equals(listType) && whiteList.getIsEncryption() == 0) {
                        identifier = AESUtil.decrypt(identifier);
                    }
                    Integer times = record.getTimes();
                    List<String> row = new ArrayList<>();
                    row.add(identifier);
                    if(isTimes.intValue() == 1){
                        row.add(times == null ? "0" : times.toString());
                    }
                    row.add(listTypeName);
                    row.add(DateUtil.format(record.getCreateTime(),"yyyy-MM-dd HH:mm:ss"));
                    row.add(DateUtil.format(record.getUpdateTime(),"yyyy-MM-dd HH:mm:ss"));
                    rows.add(row);
                }
                if (listRecords.size() != pageSize) {
                    break;
                }
                id = listRecords.get(pageSize - 1).getId();
            }
        }

        return rows;
    }

    @Override
    public WhiteListResultVo findByWhiteListAndNextId(WhiteList whiteList, String nextId) {
        WhiteListResultVo whiteListResultVo = new WhiteListResultVo();
        String appid = UserContext.getAppid();
        Integer listType = whiteList.getListType();
        whiteListResultVo.setListType(listType);
        String listId = whiteList.getId();
        List<String> recordList = new ArrayList<>();
        //数据量较大，使用JPA分页查询，循环处理
        int pageSize = 1000;
        PageVo page = new PageVo();
        page.setPageNumber(0);
        page.setPageSize(pageSize);
        page.setOrder(Sort.Direction.ASC.name());
        page.setSort("id");
        Pageable pageable = PageUtil.initPage(page);
        String id = "";
        if (StrUtil.isBlank(nextId)) {
            id = "0";
        } else {
            id = nextId;
        }
        List<WhiteListRecord> whiteListRecordList = new ArrayList<>();
        whiteListRecordList = getRepository().findOnePage((root, query, criteriaBuilder) -> {
            List<Predicate> list = new ArrayList<Predicate>();
            Path<String> listIdField = root.get("listId");
            Path<Integer> statusField = root.get("status");
            Path<String> idField = root.get("id");
            //白名单Id
            list.add(criteriaBuilder.equal(listIdField, listId));
            //状态
            list.add(criteriaBuilder.equal(statusField, 0));
            //主键id，提交效率
            list.add(criteriaBuilder.greaterThan(idField, nextId));
            Predicate[] arr = new Predicate[list.size()];
            if (list.size() > 0) {
                query.where(list.toArray(arr));
            }
            return null;
        }, pageable);
        if (whiteListRecordList.size() < pageSize) {
            whiteListResultVo.setNextId("");
        } else {
            whiteListResultVo.setNextId(whiteListRecordList.get(whiteListRecordList.size() - 1).getId());
        }
        //根据listType进行转换
        switch (listType) {
            case 1: //openId和account的情况下，可以直接复制identifier字段就行，act平台会根据listType进行区分
            case 3:
            case 4: //新增支持ActAccount模式
                for (WhiteListRecord whiteListRecord : whiteListRecordList) {
                    String identifier = whiteListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(identifier)) {
                        recordList.add(identifier);
                    }
                }
                break;
            case 2: //手机号码，需要根据identifier字段中的手机号从account表中取到拥有该手机号用户的accountId
                for (WhiteListRecord whiteListRecord : whiteListRecordList) {
                    String phone = whiteListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(phone)) {
                        List<Account> accountList = new ArrayList<>();
                        if (whiteList.getIsEncryption().equals((byte) 1) && EncryptionMethodType.MD5.getValue().equals(whiteList.getEncryptionMethod())) {
                            accountList = accountService.findByAppidAndMd5Phone(appid, phone);
                        } else {
                            accountList = accountService.findByAppidAndPhone(appid, phone);
                        }
                        for (Account account : accountList) {
                            recordList.add(account.getId());
                        }
                    }
                }
                break;
        }
        whiteListResultVo.setRecordList(recordList);
        return whiteListResultVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByListId(String listId) {
        while (true) {
            int rows = whiteListRecordMapper.batchDeleteByListId(listId);
            if (rows < 3000) {
                break;
            }
        }
        while (true) {
            int rows = whiteListExtendRecordMapper.batchDeleteByListId(listId);
            if (rows < 3000) {
                break;
            }
        }
        String cacheKey = "WhiteListRecord:" + listId;
        redisTemplate.unlink(cacheKey);
    }


    @Override
    public List<WhiteListRecord> findByListIdAndIsDeleted(String whiteListId, boolean b) {
        return getRepository().findByListIdAndIsDeleted(whiteListId, b);
    }

    @Override
    public String getMd5identifier(WhiteList whiteList, WhiteListRecord WhiteListRecord) {
        if (WhiteListRecord == null || WhiteListRecord.getExtendInfo() == null || WhiteListRecord.getExtendInfo().size() == 0) {
            return null;
        }
        String validateFields = whiteList.getValidateFields();
        String[] fields = StringUtils.split(validateFields, ",");
        if (fields == null || fields.length == 0) {
            return null;
        }
        Map paramMap = new HashMap<>();
        Map<String, WhiteListExtendRecord> extendRecordMap = new HashMap<>();
        List<WhiteListExtendRecord> extendRecords = WhiteListRecord.getExtendInfo();
        for (WhiteListExtendRecord extendRecord : extendRecords) {
            extendRecordMap.put(extendRecord.getFormMetaId(), extendRecord);
        }
        for (int i = 0; i < fields.length; i++) {
            WhiteListExtendRecord extendRecord = extendRecordMap.get(fields[i]);
            if (extendRecord != null) {
                paramMap.put(fields[i], extendRecord.getRecord());
            }
        }
        return SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
    }

}
