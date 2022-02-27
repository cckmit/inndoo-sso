package com.ytdinfo.inndoo.modules.core.serviceimpl;

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
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.dao.LimitListRecordDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.LimitListExtendRecordMapper;
import com.ytdinfo.inndoo.modules.core.dao.mapper.LimitListRecordMapper;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILimitListExtendRecordService;
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
 * 受限名单清单接口实现
 *
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "LimitListRecord")
public class LimitListRecordServiceImpl implements LimitListRecordService {

    @Autowired
    private LimitListRecordDao limitListRecordDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LimitListService limitListService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private AccountService accountService;
    @XxlConf("core.front.rooturl")
    private String coreRootUrl;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private WhiteListRecordService whiteListRecordService;
    @Autowired
    private LimitListExtendRecordService limitListExtendRecordService;
    @Autowired
    private ILimitListExtendRecordService iLimitListExtendRecordService;
    @Autowired
    private LimitListRecordMapper limitListRecordMapper;
    @Autowired
    private LimitListExtendRecordMapper limitListExtendRecordMapper;
    @Autowired
    private DictDataApiListService dictDataApiListService;
    @Autowired
    private ActAccountService actAccountService;
    @Override
    public LimitListRecordDao getRepository() {
        return limitListRecordDao;
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
    public LimitListRecord get(String id) {
        Optional<LimitListRecord> entity = getRepository().findById(id);
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
    public LimitListRecord save(LimitListRecord entity) {
        LimitListRecord limitListRecord = getRepository().save(entity);
        redisTemplate.opsForHash().put("LimitListRecord:" + entity.getListId(), entity.getIdentifier(), entity.getTimes());
        return limitListRecord;
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @Override
    public LimitListRecord update(LimitListRecord entity) {
        LimitListRecord limitListRecord = getRepository().saveAndFlush(entity);
        redisTemplate.opsForHash().put("LimitListRecord:" + entity.getListId(), entity.getIdentifier(), entity.getTimes());
        return limitListRecord;
    }

    /**
     * 删除
     *
     * @param entity
     */
    @Override
    public void delete(LimitListRecord entity) {
        getRepository().delete(entity);
        redisTemplate.opsForHash().delete("LimitListRecord:" + entity.getListId(), entity.getIdentifier());
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
    @Override
    public void delete(String id) {
        LimitListRecord entity = getRepository().getOne(id);
        getRepository().deleteById(id);
        redisTemplate.opsForHash().delete("LimitListRecord:" + entity.getListId(), entity.getIdentifier());
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<LimitListRecord> saveOrUpdateAll(Iterable<LimitListRecord> entities) {
        List<LimitListRecord> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (LimitListRecord entity : entities) {
            redisTemplate.opsForHash().delete("LimitListRecord:" + entity.getListId(), entity.getIdentifier());
        }
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String[] ids) {
        LimitListRecordDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<LimitListRecord> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        for (LimitListRecord entity : list4Delete) {
            redisTemplate.opsForHash().delete("LimitListRecord:" + entity.getListId(), entity.getIdentifier());
        }
    }

    /**
     * 批量删除
     *
     * @param entities
     */

    @Override
    public void delete(Iterable<LimitListRecord> entities) {
        getRepository().deleteAll(entities);
        for (LimitListRecord entity : entities) {
            redisTemplate.opsForHash().delete("LimitListRecord:" + entity.getListId(), entity.getIdentifier());
        }
    }

    @Override
    public Page<LimitListRecord> findByCondition(LimitListRecord limitListRecord, SearchVo searchVo, Pageable pageable, String limitListId) {

        return limitListRecordDao.findAll(new Specification<LimitListRecord>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<LimitListRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField = root.get("createTime");
                Path<String> listIdField = root.get("listId");
                Path<String> identifierField = root.get("identifier");
                Path<String> appidField = root.get("appid");
                List<Predicate> list = new ArrayList<Predicate>();

                list.add(cb.equal(appidField, limitListRecord.getAppid()));
                //创建时间
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                //受限名单id
                if (StrUtil.isNotBlank(limitListId)) {
                    list.add(cb.equal(listIdField, limitListId.trim()));
                }

                LimitList limitList = limitListService.get(limitListId);
                if (limitList != null) {
                    if (!limitList.getListType().equals(NameListType.ADVANCED)) {
                        //受限名单用户标识
                        if (StrUtil.isNotBlank(limitListRecord.getIdentifier())) {
                            String searchIdentifierField = limitListRecord.getIdentifier();
                            if (NameListType.PHONE.equals(limitList.getListType()) && limitList.getIsEncryption().intValue() == 0) {
                                searchIdentifierField = AESUtil.encrypt(searchIdentifierField);
                                list.add(cb.equal(identifierField, searchIdentifierField));
                            } else {
                                list.add(cb.like(identifierField, "%" + StringUtils.trim(searchIdentifierField) + "%"));
                            }
                        }
                    } else {
                        if (StrUtil.isNotBlank(limitListRecord.getIdentifier())) {
                            list.add(cb.like(identifierField, "%" + StringUtils.trim(limitListRecord.getIdentifier()) + "%"));
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
    public List<LimitListRecord> findByIdentifiers(List<String> identifiers) {
        return getRepository().findByIdentifierIn(identifiers);
    }
    @Override
    public NameListValidateResultVo validateByCache(LimitList limitList, String identifier) {
        NameListValidateResultVo resultVo = new NameListValidateResultVo();
        String cacheKey = "LimitListRecord:" + limitList.getId();
        // 加密
        if (limitList.getIsEncryption() == 1) {
            // 加盐
            String salt = limitList.getDataSalt();
            if(limitList.getIsDataAddSalt() == 1 && StrUtil.isNotEmpty(salt)){
                switch (limitList.getDataSaltMethod()) {
                    case 0: {
                        //数据前后加盐
                        if(limitList.getDataSaltPosition()==0){
                            identifier = salt + identifier;
                        }else if(limitList.getDataSaltPosition()==1){
                            identifier = identifier + salt;
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }

            switch (limitList.getEncryptionMethod()) {
                case 0: {
                    //MD5
                    identifier = MD5Util.md5(identifier);
                    break;
                }
                case 1: {
                    //AES
                    identifier = AESUtil.encrypt(identifier, limitList.getEncryptionPassword());
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
        if(limitList.getIsTimes().intValue() == 0){
            resultVo.setTimes(0);
        }else{
            resultVo.setTimes(times);
        }
        resultVo.setMatch(!times.equals(-1));
        resultVo.setFormId(limitList.getFormId());
        String registerUrl = coreRootUrl + "/" + UserContext.getTenantId() + "/" + limitList.getAppid() + "/user-login?id=" + limitList.getFormId();
        resultVo.setRegisterUrl(registerUrl);
        return resultVo;
    }

    @Override
    public NameListValidateResultVo verify(String listId, String recordIdentifier, String openId) {
        LimitList limitList = limitListService.get(listId);
        NameListValidateResultVo resultVo = new NameListValidateResultVo();
        resultVo.setMatch(false);
        resultVo.setTimes(0);
        if (limitList == null) {
            return resultVo;
        }
        String linkId = limitList.getLinkId();
        if (StrUtil.isNotEmpty(linkId)) {
            Byte linkType = limitList.getLinkType();
            switch (linkType) {
                case LinkTypeConstant.WHITE:
                    return whiteListRecordService.verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.LIMIT:
                    return verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.ACHIEVE:
                    return achieveListRecordService.verify(linkId, recordIdentifier, openId);
                case LinkTypeConstant.API:
                    return dictDataApiListService.verify(linkId, recordIdentifier, openId);
            }
        }

        //优先从缓存中查询
        resultVo = validateByCache(limitList, recordIdentifier);
        if (resultVo.isMatch()) {
            return resultVo;
        }
        Integer listType = limitList.getListType();
        if (NameListType.PHONE.equals(listType)) {
            Account account = accountService.get(recordIdentifier);
            if (account == null) {
                resultVo = new NameListValidateResultVo();
                resultVo.setMatch(false);
                return resultVo;
            }
            String phone = account.getPhone();
            if (limitList.getIsEncryption() == 0) {
                phone = AESUtil.encrypt(phone);
                //phone = AESUtil.comEncrypt(phone);
            }
            resultVo = validateByCache(limitList, phone);
            return resultVo;
        }
        if(NameListType.ACTACCOUNTID.equals(listType)){
            // 没有名单不包含资格次数的可以查名单中有无关联账户
            if(limitList.getIsTimes().intValue() == 0 ){
                ActAccount actAccount = actAccountService.findByActAccountId(recordIdentifier);
                if(null != actAccount){
                    List<ActAccount> actAccounts = actAccountService.findByCoreAccountId(actAccount.getCoreAccountId());
                    for(ActAccount actAcc:actAccounts ){
                        String actAccountId = actAcc.getActAccountId();
                        resultVo = validateByCache(limitList, actAccountId);
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
        if (limitList.getListType().equals(NameListType.ADVANCED)) {
            //高级校验字段清单
            String validateFields = limitList.getValidateFields();
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
                resultVo = validateByCache(limitList, identifier);
            }
        }
        return resultVo;
    }

    @Override
    public void updateCacheTime(LimitList limitList) {
        String cacheKey = "LimitListRecord:" + limitList.getId();
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        boundHashOperations.expireAt(limitList.getExpireDate());
    }


    @Override
    public void loadCache(String listId) {
        Date d1 = new Date();
        LimitList limitList = limitListService.get(listId);
        String cacheKey = "LimitListRecord:" + listId;
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
            List<LimitListRecord> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
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
            for (LimitListRecord record : listRecords) {
                List<LimitListExtendRecord> listExtendRecords = limitListExtendRecordService.findByListIdAndRecordId(listId, record.getId());
                record.setExtendInfo(listExtendRecords);
                Map mapFields = new HashMap<>();
                if(ListTypeConstant.ADVANCED.equals(limitList.getListType())){
                    for (String metaId : limitList.getValidateFields().split(",")) {
                        if(StrUtil.isNotEmpty(metaId)){
                            LimitListExtendRecord extendRecord = CollectionUtil.findOne(record.getExtendInfo(), limitListExtendRecord -> limitListExtendRecord.getFormMetaId().equals(metaId));
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
        boundHashOperations.expireAt(limitList.getExpireDate());
        Date d2 = new Date();
        System.out.println(d2.getTime() - d1.getTime());
    }

    @Override
    public void loadSingleCache(String listId, LimitListRecord record) {
        LimitList limitList = limitListService.get(listId);
        String cacheKey = "LimitListRecord:" + listId;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        List<LimitListExtendRecord> listExtendRecords = limitListExtendRecordService.findByListIdAndRecordId(listId, record.getId());
        record.setExtendInfo(listExtendRecords);
        Map mapFields = new HashMap<>();
        Map<String, Integer> map = new HashMap<>();
        if(ListTypeConstant.ADVANCED.equals(limitList.getListType())){
            for (String metaId : limitList.getValidateFields().split(",")) {
                if(StrUtil.isNotEmpty(metaId)){
                    LimitListExtendRecord extendRecord = CollectionUtil.findOne(record.getExtendInfo(), limitListExtendRecord -> limitListExtendRecord.getFormMetaId().equals(metaId));
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
        boundHashOperations.expireAt(limitList.getExpireDate());
    }

    @Override
    public boolean existsByListIdAndIdentifier(String listId, String recordIdentifier) {
        return limitListRecordDao.existsByListIdAndIdentifier(listId,recordIdentifier);
    }

    @Override
    public LimitListRecord findByListIdAndId(String listId, String id) {
        return limitListRecordDao.findByListIdAndId(listId,id);
    }

    @Override
    public LimitListRecord findByListIdAndIdentifier(String listId, String identifier) {
        return limitListRecordDao.findByListIdAndIdentifier(listId,identifier);
    }

    @Override
    public List<LimitListRecord> findByListIdAndIsDeleted(String limitListId, boolean b) {
        return getRepository().findByListIdAndIsDeleted(limitListId, b);
    }

    @Override
    public String getMd5identifier(LimitList limitList, LimitListRecord limitListRecord) {
        if (limitListRecord == null || limitListRecord.getExtendInfo() == null || limitListRecord.getExtendInfo().size() == 0) {
            return null;
        }
        String validateFields = limitList.getValidateFields();
        String[] fields = StringUtils.split(validateFields, ",");
        if (fields == null || fields.length == 0) {
            return null;
        }
        Map paramMap = new HashMap<>();
        Map<String, LimitListExtendRecord> extendRecordMap = new HashMap<>();
        List<LimitListExtendRecord> extendRecords = limitListRecord.getExtendInfo();
        for (LimitListExtendRecord extendRecord : extendRecords) {
            extendRecordMap.put(extendRecord.getFormMetaId(), extendRecord);
        }
        for (int i = 0; i < fields.length; i++) {
            LimitListExtendRecord extendRecord = extendRecordMap.get(fields[i]);
            if (extendRecord != null) {
                paramMap.put(fields[i], extendRecord.getRecord());
            }
        }
        return SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
    }

    @Override
    public void removeCache(String limitid, List<String> removeMd5s) {
        String cacheKey = "LimitListRecord:" + limitid;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        boundHashOperations.delete(removeMd5s);
    }

    @Override
    public List<List<String>> toWrite(String listId) {
        List<List<String>> rows = new ArrayList<>();
        LimitList limitList = limitListService.get(listId);
        if (limitList == null) {
            return rows;
        }
        String listTypeName = "";
        if(limitList.getListType() == 0){
            listTypeName = "高级校验";
        } else if (limitList.getListType() == 1){
            listTypeName = "微信用户openid";
        }else if (limitList.getListType() == 2){
            listTypeName = "手机号";
        }else if (limitList.getListType() == 3){
            listTypeName = "小核心账户";
        }else if (limitList.getListType() == 4){
            listTypeName = "活动平台账户";
        }else if (limitList.getListType() == 5){
            listTypeName = "帐号openid";
        }
        Byte isTimes = limitList.getIsTimes();
        int pageSize = 5000;
        PageVo page = new PageVo();
        page.setPageNumber(0);
        page.setPageSize(pageSize);
        page.setOrder(Sort.Direction.ASC.name());
        page.setSort("id");
        int listType = limitList.getListType();
        String id = "0";
        Pageable pageable = PageUtil.initPage(page);
        if (NameListType.ADVANCED.equals(listType)) {
            String validateFields = limitList.getValidateFields();
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
                    List<Map<String, Object>> listRecords = iLimitListExtendRecordService.findTransformDate(findMap);
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
                List<LimitListRecord> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
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
                for (LimitListRecord record : listRecords) {
                    String identifier = record.getIdentifier();
                    if (NameListType.PHONE.equals(listType) && limitList.getIsEncryption() == 0) {
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
    public WhiteListResultVo findByLimitListAndNextId(LimitList linkLimitList, String nextId) {
        WhiteListResultVo whiteListResultVo = new WhiteListResultVo();
        String appid = UserContext.getAppid();
        Integer listType = linkLimitList.getListType();
        whiteListResultVo.setListType(listType);
        String listId = linkLimitList.getId();
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
        List<LimitListRecord> limitListRecordList = new ArrayList<>();
        limitListRecordList = getRepository().findOnePage((root, query, criteriaBuilder) -> {
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
        if (limitListRecordList.size() < pageSize) {
            whiteListResultVo.setNextId("");
        } else {
            whiteListResultVo.setNextId(limitListRecordList.get(limitListRecordList.size() - 1).getId());
        }
        //根据listType进行转换
        switch (listType) {
            case 1: //openId和account的情况下，可以直接复制identifier字段就行，act平台会根据listType进行区分
            case 3:
                for (LimitListRecord limitListRecord : limitListRecordList) {
                    String identifier = limitListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(identifier)) {
                        recordList.add(identifier);
                    }
                }
                break;
            case 2: //手机号码，需要根据identifier字段中的手机号从account表中取到拥有该手机号用户的accountId
                for (LimitListRecord limitListRecord : limitListRecordList) {
                    String phone = limitListRecord.getIdentifier();
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
    public LimitListResultVo findByLimitListAndNextId2(LimitList limitList, String nextId) {
        LimitListResultVo limitListResultVo = new LimitListResultVo();
        String appid = UserContext.getAppid();
        Integer listType = limitList.getListType();
        limitListResultVo.setListType(listType);
        String listId = limitList.getId();
        List<String> recordList = new ArrayList<>();
        //数据量较大，使用JPA分页查询，循环处理
        int pageSize = 5000;
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
        List<LimitListRecord> limitListRecordList = new ArrayList<>();
        limitListRecordList = getRepository().findOnePage((root, query, criteriaBuilder) -> {
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
        if (limitListRecordList.size() < pageSize) {
            limitListResultVo.setNextId("");
        } else {
            limitListResultVo.setNextId(limitListRecordList.get(limitListRecordList.size() - 1).getId());
        }
        //根据listType进行转换
        switch (listType) {
            case 1: //openId和account的情况下，可以直接复制identifier字段就行，act平台会根据listType进行区分
            case 3:
            case 4: //新增支持ActAccount模式
                for (LimitListRecord limitListRecord : limitListRecordList) {
                    String identifier = limitListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(identifier)) {
                        recordList.add(identifier);
                    }
                }
                break;
            case 2: //手机号码，需要根据identifier字段中的手机号从account表中取到拥有该手机号用户的accountId
                for (LimitListRecord limitListRecord : limitListRecordList) {
                    String phone = limitListRecord.getIdentifier();
                    if (StrUtil.isNotBlank(phone)) {
                        List<Account> accountList = accountService.findByAppidAndPhone(appid, phone);
                        for (Account account : accountList) {
                            recordList.add(account.getId());
                        }
                    }
                }
                break;
        }
        limitListResultVo.setRecordList(recordList);
        return limitListResultVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByListId(String listId) {
        while (true) {
            int rows = limitListRecordMapper.batchDeleteByListId(listId);
            if (rows < 3000) {
                break;
            }
        }
        while (true) {
            int rows = limitListExtendRecordMapper.batchDeleteByListId(listId);
            if (rows < 3000) {
                break;
            }
        }
        String cacheKey = "LimitListRecord:" + listId;
        redisTemplate.unlink(cacheKey);
    }

}
