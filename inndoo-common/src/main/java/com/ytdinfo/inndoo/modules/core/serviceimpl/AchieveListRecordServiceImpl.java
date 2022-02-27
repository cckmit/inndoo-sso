package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.DigestUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.LinkTypeConstant;
import com.ytdinfo.inndoo.common.constant.ListTypeConstant;
import com.ytdinfo.inndoo.common.constant.NameListType;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.common.vo.consumer.AchieveListPushActOutVo;
import com.ytdinfo.inndoo.common.vo.consumer.AchieveListRecordVo;
import com.ytdinfo.inndoo.modules.core.dao.AchieveListRecordDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AchieveListExtendRecordMapper;
import com.ytdinfo.inndoo.modules.core.dao.mapper.AchieveListRecordMapper;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListExtendRecordService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListRecordService;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 达标名单清单接口实现
 *
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "AchieveListRecord")
public class AchieveListRecordServiceImpl implements AchieveListRecordService {

    @Autowired
    private AchieveListRecordDao achieveListRecordDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AchieveListService achieveListService;
    @Autowired
    private AccountFormFieldService accountFormFieldService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private AccountService accountService;
    @XxlConf("core.front.rooturl")
    private String coreRootUrl;
    @Autowired
    private AchieveListRecordMapper achieveListRecordMapper;
    @Autowired
    private AchieveListExtendRecordMapper achieveListExtendRecordMapper;
    @Autowired
    private WhiteListRecordService whiteListRecordService;
    @Autowired
    private LimitListRecordService limitListRecordService;

    @Autowired
    private IAchieveListExtendRecordService iAchieveListExtendRecordService;
    @Autowired
    private IAccountService iAccountService;
    @Autowired
    private AchieveListExtendRecordService achieveListExtendRecordService;
    @Autowired
    private IAchieveListRecordService iAchieveListRecordService;
    @Autowired
    private ActivityApiUtil activityApiUtil;
    @Autowired
    private RedisDistributedLockTemplate lockTemplate;
    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private DictDataApiListService dictDataApiListService;
    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private RabbitUtil rabbitUtil;
    @Override
    public AchieveListRecordDao getRepository() {
        return achieveListRecordDao;
    }
    @Autowired
    private ExceptionLogService exceptionLogService;
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
    public AchieveListRecord get(String id) {
        Optional<AchieveListRecord> entity = getRepository().findById(id);
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
    public AchieveListRecord save(AchieveListRecord entity) {
        AchieveListRecord achieveListRecord = getRepository().save(entity);
        // redisTemplate.opsForHash().put("AchieveListRecord:" + entity.getListId(), entity.getIdentifier(), entity.getTimes());
        return achieveListRecord;
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @Override
    public AchieveListRecord update(AchieveListRecord entity) {
        AchieveListRecord achieveListRecord = getRepository().saveAndFlush(entity);
        redisTemplate.opsForHash().put("AchieveListRecord:" + entity.getListId(), entity.getIdentifier(), entity.getTimes());
        return achieveListRecord;
    }

    /**
     * 删除
     *
     * @param entity
     */
    @Override
    public void delete(AchieveListRecord entity) {
        getRepository().delete(entity);
        if (!entity.getListType().equals(NameListType.ADVANCED)) {
            String cacheKey = "AchieveListRecord:" + entity.getListId();
            redisTemplate.opsForHash().delete(cacheKey, entity.getIdentifier());
        }
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
    @Override
    public void delete(String id) {
        AchieveListRecord entity = getRepository().getOne(id);
        getRepository().deleteById(id);
        if (!entity.getListType().equals(NameListType.ADVANCED)) {
            String cacheKey = "AchieveListRecord:" + entity.getListId();
            redisTemplate.opsForHash().delete(cacheKey, entity.getIdentifier());
        }
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<AchieveListRecord> saveOrUpdateAll(Iterable<AchieveListRecord> entities) {
        List<AchieveListRecord> list = getRepository().saveAll(entities);
        for (AchieveListRecord entity : entities) {
            redisTemplate.opsForHash().put("AchieveListRecord:" + entity.getListId(), entity.getIdentifier(), entity.getTimes());
        }
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */

    @Override
    public void delete(String[] ids) {
        AchieveListRecordDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<AchieveListRecord> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);

        for (AchieveListRecord entity : list4Delete) {
            if (!entity.getListType().equals(NameListType.ADVANCED)) {
                String cacheKey = "AchieveListRecord:" + entity.getListId();
                redisTemplate.opsForHash().delete(cacheKey, entity.getIdentifier());
            }
        }
    }

    /**
     * 批量删除
     *
     * @param entities
     */

    @Override
    public void delete(Iterable<AchieveListRecord> entities) {
        getRepository().deleteAll(entities);
        for (AchieveListRecord entity : entities) {
            if (!entity.getListType().equals(NameListType.ADVANCED)) {
                String cacheKey = "AchieveListRecord:" + entity.getListId();
                redisTemplate.opsForHash().delete(cacheKey, entity.getIdentifier());
            }
        }
    }

    @Override
    public Page<AchieveListRecord> findByCondition(AchieveListRecord achieveListRecord, SearchVo searchVo, Pageable pageable, String achieveListId) {

        return achieveListRecordDao.findAll(new Specification<AchieveListRecord>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<AchieveListRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField = root.get("createTime");
                Path<String> listIdField = root.get("listId");
                Path<String> identifierField = root.get("identifier");
                Path<String> appidField = root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();

                list.add(cb.equal(appidField, achieveListRecord.getAppid()));
                //创建时间
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                //达标名单id
                if (StrUtil.isNotBlank(achieveListId)) {
                    list.add(cb.equal(listIdField, achieveListId.trim()));
                }

                AchieveList achieveList = achieveListService.get(achieveListId);
                if (achieveList != null) {
                    if (!achieveList.getListType().equals(NameListType.ADVANCED)) {
                        //白名单用户标识
                        if (StrUtil.isNotBlank(achieveListRecord.getIdentifier())) {
                            String searchIdentifierField = achieveListRecord.getIdentifier();
                            if (achieveList.getListType() == NameListType.PHONE && achieveList.getIsEncryption().intValue() == 0) {
                                searchIdentifierField = AESUtil.encrypt(searchIdentifierField);
                                list.add(cb.equal(identifierField, searchIdentifierField));
                            } else {
                                list.add(cb.like(identifierField, "%" + StringUtils.trim(achieveListRecord.getIdentifier()) + "%"));
                            }
                        }
                    } else {
                        if (StrUtil.isNotBlank(achieveListRecord.getIdentifier())) {
                            list.add(cb.like(identifierField, "%" + StringUtils.trim(achieveListRecord.getIdentifier()) + "%"));
                        }
                    }
                }

                if (!achieveListRecord.getListType().equals(NameListType.ADVANCED)) {
                    //达标名单用户标识
                    if (StrUtil.isNotBlank(achieveListRecord.getIdentifier())) {
                        list.add(cb.equal(identifierField, achieveListRecord.getIdentifier().trim()));
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
    public NameListValidateResultVo validateByCache(AchieveList achieveList, String identifier) {
        NameListValidateResultVo resultVo = new NameListValidateResultVo();
        String cacheKey = "AchieveListRecord:" + achieveList.getId();
        // 加密
        if (achieveList.getIsEncryption() == 1) {
            // 加盐
            String salt = achieveList.getDataSalt();
            if(achieveList.getIsDataAddSalt() == 1 && StrUtil.isNotEmpty(salt)){
                switch (achieveList.getDataSaltMethod()) {
                    case 0: {
                        //数据前后加盐
                        if(achieveList.getDataSaltPosition()==0){
                            identifier = salt + identifier;
                        }else if(achieveList.getDataSaltPosition()==1){
                            identifier = identifier + salt;
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }

            switch (achieveList.getEncryptionMethod()) {
                case 0: {
                    //MD5
                    identifier = MD5Util.md5(identifier);
                    break;
                }
                case 1: {
                    //AES
                    identifier = AESUtil.encrypt(identifier, achieveList.getEncryptionPassword());
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
        BigDecimal timesvalue= BigDecimal.ZERO;
        if (value != null) {
            Number num = Float.parseFloat(value.toString());
            times =num.intValue(); //Integer.parseInt(value.toString());
        }
        if (value != null) {
            timesvalue =new BigDecimal(value.toString());
        }
        if(achieveList.getIsTimes().intValue() == 0&&achieveList.getIsDifferentReward().intValue() == 0){
            resultVo.setTimes(0);
        }else{
            resultVo.setTimes(times);
            resultVo.setValue(timesvalue);
        }
        resultVo.setMatch(!times.equals(-1));
        resultVo.setFormId(achieveList.getFormId());
        String registerUrl = coreRootUrl + "/" + UserContext.getTenantId() + "/" + achieveList.getAppid() + "/user-login?id=" + achieveList.getFormId();
        resultVo.setRegisterUrl(registerUrl);
        return resultVo;
    }

    @Override
    public NameListValidateResultVo verify(String listId, String recordIdentifier, String openId) {
        AchieveList achieveList = achieveListService.get(listId);
        NameListValidateResultVo resultVo = new NameListValidateResultVo();
        resultVo.setMatch(false);
        resultVo.setTimes(0);
        if (achieveList == null) {
            return resultVo;
        }
        String linkId = achieveList.getLinkId();
        if (StrUtil.isNotEmpty(linkId)) {
            Byte linkType = achieveList.getLinkType();
            switch (linkType) {
                case LinkTypeConstant.WHITE:
                    return whiteListRecordService.verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.LIMIT:
                    return limitListRecordService.verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.ACHIEVE:
                    return verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.API:
                    return dictDataApiListService.verify(linkId, recordIdentifier, openId);
            }
        }
        //优先从缓存中查询
        resultVo = validateByCache(achieveList, recordIdentifier);
        if (resultVo.isMatch()) {
            return resultVo;
        }
        Integer listType = achieveList.getListType();
        if (NameListType.PHONE.equals(listType)) {
            Account account = accountService.get(recordIdentifier);
            if (account == null) {
                resultVo = new NameListValidateResultVo();
                resultVo.setMatch(false);
                return resultVo;
            }
            String phone = account.getPhone();
            if (achieveList.getIsEncryption() == 0) {
                phone = AESUtil.encrypt(phone);
                //phone = AESUtil.comEncrypt(phone);
            }
            resultVo = validateByCache(achieveList, phone);
            return resultVo;
        }
        if(NameListType.ACTACCOUNTID.equals(listType)){
            // 没有名单不包含资格次数的可以查名单中有无关联账户
            if(achieveList.getIsTimes().intValue() == 0 ){
                ActAccount actAccount = actAccountService.findByActAccountId(recordIdentifier);
                if(null != actAccount){
                    List<ActAccount> actAccounts = actAccountService.findByCoreAccountId(actAccount.getCoreAccountId());
                    for(ActAccount actAcc:actAccounts ){
                        String actAccountId = actAcc.getActAccountId();
                        resultVo = validateByCache(achieveList, actAccountId);
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
        if (achieveList.getListType().equals(NameListType.ADVANCED)) {
            //高级校验字段清单
            String validateFields = achieveList.getValidateFields();
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
                resultVo = validateByCache(achieveList, identifier);
            }
        }
        return resultVo;
    }

    @Override
    public void updateCacheTime(AchieveList achieveList) {
        String cacheKey = "AchieveListRecord:" + achieveList.getId();
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        boundHashOperations.expireAt(achieveList.getExpireDate());
    }

    @Override
    public void loadCache(String listId) {
        Date d1 = new Date();
        AchieveList achieveList = achieveListService.get(listId);
        String cacheKey = "AchieveListRecord:" + listId;
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
            List<AchieveListRecord> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
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
            Map<String, BigDecimal> map = new HashMap<>();
            for (AchieveListRecord record : listRecords) {
                List<AchieveListExtendRecord> listExtendRecords = achieveListExtendRecordService.findByListIdAndRecordId(listId, record.getId());
                record.setExtendInfo(listExtendRecords);
                Map mapFields = new HashMap<>();
                if(ListTypeConstant.ADVANCED.equals(achieveList.getListType())){
                    for (String metaId : achieveList.getValidateFields().split(",")) {
                        if(StrUtil.isNotEmpty(metaId)){
                            AchieveListExtendRecord extendRecord = CollectionUtil.findOne(record.getExtendInfo(), achieveListExtendRecord -> achieveListExtendRecord.getFormMetaId().equals(metaId));
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
        boundHashOperations.expireAt(achieveList.getExpireDate());
        Date d2 = new Date();
        System.out.println(d2.getTime() - d1.getTime());
    }
    @Override
    public void loadSingleCache(String listId, AchieveListRecord record) {
        AchieveList achieveList = achieveListService.get(listId);
        String cacheKey = "AchieveListRecord:" + listId;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        List<AchieveListExtendRecord> listExtendRecords = achieveListExtendRecordService.findByListIdAndRecordId(listId, record.getId());
        record.setExtendInfo(listExtendRecords);
        Map mapFields = new HashMap<>();
        Map<String, BigDecimal> map = new HashMap<>();
        if(ListTypeConstant.ADVANCED.equals(achieveList.getListType())){
            for (String metaId : achieveList.getValidateFields().split(",")) {
                if(StrUtil.isNotEmpty(metaId)){
                    AchieveListExtendRecord extendRecord = CollectionUtil.findOne(record.getExtendInfo(), achieveListExtendRecord -> achieveListExtendRecord.getFormMetaId().equals(metaId));
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
        boundHashOperations.expireAt(achieveList.getExpireDate());
    }

    @Override
    public boolean existsByListIdAndIdentifier(String listId, String recordIdentifier) {
        return achieveListRecordDao.existsByListIdAndIdentifier(listId,recordIdentifier);
    }

    @Override
    public AchieveListRecord findByListIdAndId(String listId, String id) {
        return achieveListRecordDao.findByListIdAndId(listId,id);
    }

    @Override
    public AchieveListRecord findByListIdAndIdentifier(String listId, String identifier) {
        return achieveListRecordDao.findByListIdAndIdentifier(listId,identifier);
    }

    @Override
    public void handlePushAchieveTimes(String listId, String record, String times) {
        if (StrUtil.isNotBlank(times) && Integer.valueOf(times) > 0) {
            //加个锁：listId + record
            String lockKey = "handlePushAchieveTimes:" + listId + record;
            lockTemplate.execute(lockKey, 3000, new Callback() {
                @Override
                public Object onGetLock() throws InterruptedException {
                    AchieveList achieveList = achieveListService.get(listId);
                    BigDecimal actTimes = new BigDecimal(times);
                    String identifier = record;
                    if (achieveList.getIsEncryption() == 0 && achieveList.getListType() == 2) {
                        identifier = AESUtil.encrypt(identifier);
                    }
                    // 加密，转大写
                    if(achieveList.getIsEncryption() == 1){
                        identifier = identifier.toUpperCase();
                    }
                    //先去缓存找，没有则塞DB、加缓冲；如果有则判断参数是否相等
                    NameListValidateResultVo nameListValidateResultVo = achieveListRecordService.verify(listId, identifier, null);
                    if (!nameListValidateResultVo.isMatch()) {
                        //缓存中没找到，塞DB，重新加载缓存
                        AchieveListRecord achieveListRecord = new AchieveListRecord();
                        achieveListRecord.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                        achieveListRecord.setListId(listId);
                        achieveListRecord.setCreateTime(new Date());
                        achieveListRecord.setUpdateTime(new Date());
                        achieveListRecord.setCreateBy("");
                        achieveListRecord.setUpdateBy("");
                        achieveListRecord.setTimes(actTimes);
                        achieveListRecord.setIdentifier(identifier);
                        achieveListRecordService.save(achieveListRecord);
                        achieveListRecordService.loadSingleCache(achieveList.getId(), achieveListRecord);
                        //发送mq达标用户导入后推送到act用户
                        MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<AchieveListRecord>();
                        mqMessageAchieveListRecord.setAppid(UserContext.getAppid());
                        mqMessageAchieveListRecord.setTenantId(UserContext.getTenantId());
                        mqMessageAchieveListRecord.setContent(achieveListRecord);
                        rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);
                    } else {
                        //校验缓存，跟新DB并重新加载缓存
                        Integer redisTimes = nameListValidateResultVo.getTimes();
                        if (Integer.valueOf(times) > redisTimes) {
                            AchieveListRecord achieveListRecord = achieveListRecordService.findByListIdAndIdentifier(listId, identifier);
                            achieveListRecord.setTimes(actTimes);
                            achieveListRecordService.update(achieveListRecord);
                            achieveListRecordService.loadSingleCache(achieveList.getId(), achieveListRecord);
                            //发送mq达标用户导入后推送到act用户
                            MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<AchieveListRecord>();
                            mqMessageAchieveListRecord.setAppid(UserContext.getAppid());
                            mqMessageAchieveListRecord.setTenantId(UserContext.getTenantId());
                            mqMessageAchieveListRecord.setContent(achieveListRecord);
                            rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);
                        }
                    }
                    return null;
                }

                @Override
                public Object onTimeout() throws InterruptedException {
                    handlePushAchieveTimes(listId, record, times);
                    return null;
                }
            });
        }
    }

    @Override
    public void handlePushAchieve(String listId, String record) {
        //加个锁：listId + record
        String lockKey = "handlePushAchieveTimes:" + listId + record;
        lockTemplate.execute(lockKey, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                AchieveList achieveList = achieveListService.get(listId);
                String identifier = record;
                if (achieveList.getIsEncryption() == 0 && achieveList.getListType() == 2) {
                    identifier = AESUtil.encrypt(identifier);
                }
                // 加密，转大写
                if(achieveList.getIsEncryption() == 1){
                    identifier = identifier.toUpperCase();
                }
                //先去缓存找，没有则塞DB、加缓冲；如果有则判断参数是否相等
                NameListValidateResultVo nameListValidateResultVo = achieveListRecordService.verify(listId, identifier, null);
                if (!nameListValidateResultVo.isMatch()) {
                    //缓存中没找到，塞DB，重新加载缓存
                    AchieveListRecord achieveListRecord = new AchieveListRecord();
                    achieveListRecord.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    achieveListRecord.setListId(listId);
                    achieveListRecord.setCreateTime(new Date());
                    achieveListRecord.setUpdateTime(new Date());
                    achieveListRecord.setCreateBy("");
                    achieveListRecord.setUpdateBy("");
                    achieveListRecord.setIdentifier(identifier);
                    achieveListRecordService.save(achieveListRecord);
                    achieveListRecordService.loadSingleCache(achieveList.getId(), achieveListRecord);
                    //发送mq达标用户导入后推送到act用户
                    MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<AchieveListRecord>();
                    mqMessageAchieveListRecord.setAppid(UserContext.getAppid());
                    mqMessageAchieveListRecord.setTenantId(UserContext.getTenantId());
                    mqMessageAchieveListRecord.setContent(achieveListRecord);
                    rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);
                }
                return null;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                handlePushAchieve(listId, record);
                return null;
            }
        });
    }


    @Override
    public List<AchieveListRecordDataVo> findByAchieveListIdAndNextId(String achieveListId, String nextId) {
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
        final String finalId = id;
        List<AchieveListRecordDataVo> achieveListRecordDataVoList = new ArrayList<>();
        List<AchieveListRecord> achieveListRecordList = new ArrayList<>();
        achieveListRecordList = getRepository().findOnePage((root, query, criteriaBuilder) -> {
            List<Predicate> list = new ArrayList<Predicate>();
            Path<String> listIdField = root.get("listId");
            Path<Integer> statusField = root.get("status");
            Path<String> idField = root.get("id");
            Path<Date> updateTimeField = root.get("updateTime");
            //白名单Id
            list.add(criteriaBuilder.equal(listIdField, achieveListId));
            //状态
            list.add(criteriaBuilder.equal(statusField, 0));
            //更新日期
            list.add(criteriaBuilder.greaterThan(updateTimeField, DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -1))));
            //主键id，提交效率
            list.add(criteriaBuilder.greaterThan(idField, finalId));
            Predicate[] arr = new Predicate[list.size()];
            if (list.size() > 0) {
                query.where(list.toArray(arr));
            }
            return null;
        }, pageable);
        if (CollUtil.isNotEmpty(achieveListRecordList)) {
            for (AchieveListRecord achieveListRecord : achieveListRecordList) {
                AchieveListRecordDataVo achieveListRecordDataVo = new AchieveListRecordDataVo();
                BeanUtil.copyProperties(achieveListRecord, achieveListRecordDataVo);
                achieveListRecordDataVoList.add(achieveListRecordDataVo);
            }
        }
        return achieveListRecordDataVoList;
    }

    private AchieveListExtendRecord findAchieveListExtendRecord(AchieveListRecord record, String fieldId) {
        List<AchieveListExtendRecord> extendInfo = record.getExtendInfo();
        for (AchieveListExtendRecord info : extendInfo) {
            if (info.getFormMetaId().equals(fieldId)) {
                return info;
            }
        }
        return null;
    }

    @Override
    public List<AchieveListRecord> findByListIdAndIsDeleted(String achieveListId, Boolean i) {
        return getRepository().findByListIdAndIsDeleted(achieveListId, i);
    }

    @Override
    public String getMd5identifier(AchieveList achieveList, AchieveListRecord achieveListRecord) {
        if (achieveListRecord == null || achieveListRecord.getExtendInfo() == null || achieveListRecord.getExtendInfo().size() == 0) {
            return null;
        }
//        String validateFields = achieveList.getValidateFields();
//        String[] fields = StringUtils.split(validateFields, ",");
//        if (fields == null || fields.length == 0) {
//            return null;
//        }
        Map paramMap = new HashMap<>();
        Map<String, AchieveListExtendRecord> extendRecordMap = new HashMap<>();
        List<AchieveListExtendRecord> extendRecords = achieveListRecord.getExtendInfo();
        for (AchieveListExtendRecord extendRecord : extendRecords) {
            AccountFormMeta accountFormMeta = accountFormMetaService.get(extendRecord.getFormMetaId());
            extendRecordMap.put(accountFormMeta.getMetaType(), extendRecord);
        }
//        for (int i = 0; i < fields.length; i++) {
//            AchieveListExtendRecord extendRecord = extendRecordMap.get(fields[i]);
//            if (extendRecord != null) {
//                paramMap.put(fields[i], AESUtil.decrypt(extendRecord.getRecord()));
//            }
//        }
        Boolean isIdentifierForm = true;
        //根据appid设置成身份识别表单的主键
        AccountForm accountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(), isIdentifierForm);
        List<AccountFormMeta> accountFormMetas = accountForm.getAccountFormMetas();
        for (AccountFormMeta accountFormMeta : accountFormMetas) {
            AchieveListExtendRecord extendRecord = extendRecordMap.get(accountFormMeta.getMetaType());
            if (extendRecord != null) {
                String md5 =  AESUtil.decrypt(extendRecord.getRecord());
                paramMap.put(accountFormMeta.getId(), AESUtil.decrypt(extendRecord.getRecord()));
            }
        }
        return SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
    }

    @Override
    public void removeCache(String limitid, List<String> removeMd5s) {
        String cacheKey = "AchieveListRecord:" + limitid;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        boundHashOperations.delete(removeMd5s);
    }

    @Override
    public List<List<String>> toWrite(String listId) {
        List<List<String>> rows = new ArrayList<>();
        AchieveList achieveList = achieveListService.get(listId);
        if (achieveList == null) {
            return rows;
        }
        String listTypeName = "";
        if(achieveList.getListType() == 0){
            listTypeName = "高级校验";
        } else if (achieveList.getListType() == 1){
            listTypeName = "微信用户openid";
        }else if (achieveList.getListType() == 2){
            listTypeName = "手机号";
        }else if (achieveList.getListType() == 3){
            listTypeName = "小核心账户";
        }else if (achieveList.getListType() == 4){
            listTypeName = "活动平台账户";
        }else if (achieveList.getListType() == 5){
            listTypeName = "帐号openid";
        }
        Byte isTimes = achieveList.getIsTimes();
        Byte isDifferentReward = achieveList.getIsDifferentReward();
        int pageSize = 5000;
        PageVo page = new PageVo();
        page.setPageNumber(0);
        page.setPageSize(pageSize);
        page.setOrder(Sort.Direction.ASC.name());
        page.setSort("id");
        int listType = achieveList.getListType();
        String id = "0";
        Pageable pageable = PageUtil.initPage(page);
        if (NameListType.ADVANCED.equals(listType)) {
            String validateFields = achieveList.getValidateFields();
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
                if(isDifferentReward.intValue() == 1){
                    headList.add("奖励值");
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
                    List<Map<String, Object>> listRecords = iAchieveListExtendRecordService.findTransformDate(findMap);
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
            if(isDifferentReward.intValue() == 1){
                headList.add("奖励值");
            }
            headList.add("类型");
            headList.add("创建时间");
            headList.add("修改时间");
            rows.add(headList);
            while (true) {
                final String finalId = id;
                List<AchieveListRecord> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
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
                for (AchieveListRecord record : listRecords) {
                    String identifier = record.getIdentifier();
                    if (NameListType.PHONE.equals(listType) && achieveList.getIsEncryption() == 0) {
                        identifier = AESUtil.decrypt(identifier);
                    }
                    BigDecimal times = record.getTimes();
                    List<String> row = new ArrayList<>();
                    row.add(identifier);
                    if(isTimes.intValue() == 1){
                        row.add(times == null ? "0" : times.toString());
                    }
                    if(isDifferentReward.intValue() == 1){
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
    public WhiteListResultVo findByAchieveListAndNextId(AchieveList linkAchieveList, String nextId) {
        WhiteListResultVo whiteListResultVo = new WhiteListResultVo();
        String appid = UserContext.getAppid();
        Integer listType = linkAchieveList.getListType();
        whiteListResultVo.setListType(listType);
        String listId = linkAchieveList.getId();
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
        List<AchieveListRecord> achieveListRecordList = new ArrayList<>();
        achieveListRecordList = getRepository().findOnePage((root, query, criteriaBuilder) -> {
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
        if (achieveListRecordList.size() < pageSize) {
            whiteListResultVo.setNextId("");
        } else {
            whiteListResultVo.setNextId(achieveListRecordList.get(achieveListRecordList.size() - 1).getId());
        }
        //根据listType进行转换
        switch (listType) {
            case 1: //openId和account的情况下，可以直接复制identifier字段就行，act平台会根据listType进行区分
            case 3:
                for (AchieveListRecord achieveListRecord : achieveListRecordList) {
                    String identifier = achieveListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(identifier)) {
                        recordList.add(identifier);
                    }
                }
                break;
            case 4:
                for (AchieveListRecord achieveListRecord : achieveListRecordList) {
                    String identifier = achieveListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(identifier)) {
                        recordList.add(identifier);
                    }
                }
                break;
            case 2: //手机号码，需要根据identifier字段中的手机号从account表中取到拥有该手机号用户的accountId
                for (AchieveListRecord achieveListRecord : achieveListRecordList) {
                    String phone = achieveListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(phone)) {
                        List<Account> accountList = accountService.findByAppidAndPhone(appid, phone);
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
            int rows = achieveListRecordMapper.batchDeleteByListId(listId);
            if (rows < 3000) {
                break;
            }
        }
        while (true) {
            int rows = achieveListExtendRecordMapper.batchDeleteByListId(listId);
            if (rows < 3000) {
                break;
            }
        }
        String cacheKey = "AchieveListRecord:" + listId;
        redisTemplate.unlink(cacheKey);
    }

    @Override
    public void achieveListPushAct(AchieveList achieveList) {
        String lockId = "achieveListPushAct-" + achieveList.getId();
        lockTemplate.execute(lockId, 10000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                AchieveListPushActOutVo achieveListPushActOutVo = new AchieveListPushActOutVo();
                String listId = achieveList.getId();
                Integer listType = achieveList.getListType();
                achieveListPushActOutVo.setListId(listId);
                achieveListPushActOutVo.setListType(listType);
                int pageSize = 1000;
                PageVo page = new PageVo();
                page.setPageNumber(0);
                page.setPageSize(pageSize);
                page.setOrder(Sort.Direction.ASC.name());
                Pageable pageable = PageUtil.initPage(page);
                List<AchieveListRecordVo> recordList = new ArrayList<>();
                while (true) {
                    List<AchieveListRecord> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
                        List<Predicate> list = new ArrayList<Predicate>();
                        Path<String> listIdField = root.get("listId");
                        Path<Boolean> pushActField = root.get("pushAct");
                        list.add(criteriaBuilder.equal(listIdField, listId));
                        list.add(criteriaBuilder.equal(pushActField, false));
                        Predicate[] arr = new Predicate[list.size()];
                        if (list.size() > 0) {
                            query.where(list.toArray(arr));
                        }
                        return null;
                    }, pageable);
                    if (listType == 0) {
                        //为0表示组合校验
                        for (AchieveListRecord record : listRecords) {
                            record.setPushAct(true);
                            if (StrUtil.isNotBlank(record.getIdentifier())) {
                                Account account = accountService.findByidentifier(record.getIdentifier());
                                if (null != account) {
                                    AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                                    achieveListRecordVo.setIdentifier(account.getId());
                                    achieveListRecordVo.setTimes(record.getTimes());
                                    achieveListRecordVo.setFound(true);
                                    recordList.add(achieveListRecordVo);
                                }
                                else
                                {
                                    AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                                    achieveListRecordVo.setIdentifier(record.getIdentifier());
                                    achieveListRecordVo.setTimes(record.getTimes());
                                    achieveListRecordVo.setFound(false);
                                    recordList.add(achieveListRecordVo);
                                }
                            }
                        }
                    }
                    if (listType == 1 || listType == 5) {
                        //为1时表示用户识别类型为 openid
                        for (AchieveListRecord record : listRecords) {
                            record.setPushAct(true);
                            AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                            achieveListRecordVo.setIdentifier(record.getIdentifier());
                            achieveListRecordVo.setTimes(record.getTimes());
                            achieveListRecordVo.setFound(true);
                            recordList.add(achieveListRecordVo);
                        }
                    }
                    if (listType == 2) {
                        Map<String, BigDecimal> aesPhoneTimes = new HashMap<>();
                        for (AchieveListRecord record : listRecords) {
                            //为1时表示用户识别类型为phone
                            List<String> aesPhones = new ArrayList<>();
                            //修改状态
                            record.setPushAct(true);
                            aesPhones.add(record.getIdentifier());
                            aesPhoneTimes.put(record.getIdentifier(), record.getTimes());
                            //根据手机号码查询客户
                            List<Account> accounts = accountService.findByPhones(aesPhones);
                            if (null != accounts && accounts.size() > 0) {
                                for (Account account : accounts) {
                                    AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                                    achieveListRecordVo.setIdentifier(account.getId());
                                    achieveListRecordVo.setTimes(aesPhoneTimes.get(account.getPhone()));
                                    achieveListRecordVo.setFound(true);
                                    recordList.add(achieveListRecordVo);
                                }
                            }
                            else
                            {
                                AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                                achieveListRecordVo.setIdentifier(record.getIdentifier());
                                achieveListRecordVo.setTimes(record.getTimes());
                                achieveListRecordVo.setFound(false);
                                recordList.add(achieveListRecordVo);
                            }
                        }

                    }
                    if (listType == 3) {
                        for (AchieveListRecord record : listRecords) {
                            //修改状态
                            record.setPushAct(true);
                            AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                            achieveListRecordVo.setIdentifier(record.getIdentifier());
                            achieveListRecordVo.setTimes(record.getTimes());
                            achieveListRecordVo.setFound(true);
                            recordList.add(achieveListRecordVo);
                        }
                    }
                    if (listType == 4) {
                        for (AchieveListRecord record : listRecords) {
                            //修改状态
                            record.setPushAct(true);
                            AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                            achieveListRecordVo.setIdentifier(record.getIdentifier());
                            achieveListRecordVo.setTimes(record.getTimes());
                            achieveListRecordVo.setFound(true);
                            recordList.add(achieveListRecordVo);
                        }
                    }
                    achieveListPushActOutVo.setRecordList(recordList);
                    activityApiUtil.achieveListPushAct(achieveListPushActOutVo);
                    iAchieveListRecordService.updateBatchById(listRecords, 1000);
                    //saveOrUpdateAll(listRecords);
                    if (listRecords.size() != pageSize) {
                        break;
                    }
                    recordList.clear();
                }
                return null;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                return null;
            }
        });
    }
    @Override
    public void achieveListRecordPushAct(AchieveListRecord record) {
        AchieveList achieveList = achieveListService.get(record.getListId());
        String lockId = "achieveListPushAct-" + achieveList.getId();
        lockTemplate.execute(lockId, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                AchieveListPushActOutVo achieveListPushActOutVo = new AchieveListPushActOutVo();
                String listId = achieveList.getId();
                Integer listType = achieveList.getListType();
                achieveListPushActOutVo.setListId(listId);
                achieveListPushActOutVo.setListType(listType);
                List<AchieveListRecordVo> recordList = new ArrayList<>();

                if (listType == 0) {
                    //为0表示组合校验
                    record.setPushAct(true);
                    if (StrUtil.isNotBlank(record.getIdentifier())) {
                        Account account = accountService.findByidentifier(record.getIdentifier());
                        if (null != account) {
                            AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                            achieveListRecordVo.setIdentifier(account.getId());
                            achieveListRecordVo.setTimes(record.getTimes());
                            achieveListRecordVo.setFound(true);
                            recordList.add(achieveListRecordVo);
                        }
                        else
                        {
                            AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                            achieveListRecordVo.setIdentifier(record.getIdentifier());
                            achieveListRecordVo.setTimes(record.getTimes());
                            achieveListRecordVo.setFound(false);
                            recordList.add(achieveListRecordVo);
                        }
                    }
                }
                if (listType == 1 || listType == 5) {
                    //为1时表示用户识别类型为 openid
                    record.setPushAct(true);
                    AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                    achieveListRecordVo.setIdentifier(record.getIdentifier());
                    achieveListRecordVo.setTimes(record.getTimes());
                    achieveListRecordVo.setFound(true);
                    recordList.add(achieveListRecordVo);

                }
                if (listType == 2) {
                    //为1时表示用户识别类型为phone
                    List<String> aesPhones = new ArrayList<>();
                    //修改状态
                    record.setPushAct(true);
                    aesPhones.add(record.getIdentifier());

                    //根据手机号码查询客户
                    List<Account> accounts = accountService.findByPhones(aesPhones);
                    if (null != accounts && accounts.size() > 0) {
                        for (Account account : accounts) {
                            AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                            achieveListRecordVo.setIdentifier(account.getId());
                            achieveListRecordVo.setTimes(record.getTimes());
                            achieveListRecordVo.setFound(true);
                            recordList.add(achieveListRecordVo);
                        }
                    }
                    else
                    {
                        AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                        achieveListRecordVo.setIdentifier(record.getIdentifier());
                        achieveListRecordVo.setTimes(record.getTimes());
                        achieveListRecordVo.setFound(false);
                        recordList.add(achieveListRecordVo);
                    }
                }
                if (listType == 3) {
                    //修改状态
                    record.setPushAct(true);
                    AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                    achieveListRecordVo.setIdentifier(record.getIdentifier());
                    achieveListRecordVo.setTimes(record.getTimes());
                    achieveListRecordVo.setFound(true);
                    recordList.add(achieveListRecordVo);
                }
                if (listType == 4) {
                    //为4时表示用户识别类型为活动平台accountid
                    record.setPushAct(true);
                    AchieveListRecordVo achieveListRecordVo = new AchieveListRecordVo();
                    achieveListRecordVo.setIdentifier(record.getIdentifier());
                    achieveListRecordVo.setTimes(record.getTimes());
                    achieveListRecordVo.setFound(true);
                    recordList.add(achieveListRecordVo);

                }
                achieveListPushActOutVo.setRecordList(recordList);
                activityApiUtil.achieveListPushAct(achieveListPushActOutVo);
                List<AchieveListRecord> listRecords = new ArrayList<>();
                listRecords.add(record);
                iAchieveListRecordService.updateBatchById(listRecords, 1000);
                return null;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                achieveListRecordPushAct(record);
                return null;
            }
        });
    }

}