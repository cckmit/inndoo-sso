package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.ListTypeConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.enums.EncryptionMethodType;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.AccountFormMetaService;
import com.ytdinfo.inndoo.modules.core.service.AchieveListExtendRecordService;
import com.ytdinfo.inndoo.modules.core.service.AchieveListRecordService;
import com.ytdinfo.inndoo.modules.core.service.AchieveListService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListRecordService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListService;
import com.ytdinfo.inndoo.vo.NameListVo;
import com.ytdinfo.inndoo.vo.SpecialNameListVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "达标名单接口")
@RequestMapping("/achievelist")

@APIModifier(APIModifierType.PRIVATE)
public class AchieveListController {

    @Autowired
    private AchieveListService achieveListService;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private AchieveListExtendRecordService achieveListExtendRecordService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private IAchieveListService iAchieveListService;
    @Autowired
    private RabbitUtil rabbitUtil;
    @Autowired
    private IAchieveListRecordService iAchieveListRecordService;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/querySpecial/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<SpecialNameListVo> querySpecial(@PathVariable String id) {
        AchieveList entity = achieveListService.get(id);
        if(entity == null){
            return new ResultUtil<SpecialNameListVo>().setData(null);
        }
        SpecialNameListVo specialNameListVo =  new SpecialNameListVo();
        BeanUtil.copyProperties(entity,specialNameListVo);
        return new ResultUtil<SpecialNameListVo>().setData(specialNameListVo);
    }
    @RequestMapping(value = "/getTotal/{listId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<Integer> getTotal(@PathVariable String listId){

        String cacheKey = "AchieveListRecord:" + listId;
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cacheKey);
        Integer total = 0;
        if ( null != boundHashOperations ) {
            total = boundHashOperations.values().size();
        }
        return new ResultUtil<Integer>().setData(total);
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id删除数据")
    @SystemLog(description = "通过id删除数据")
    public Result<Object> deleteById(@PathVariable String id) {
        achieveListService.delete(id);
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }


    @RequestMapping(value = "/createSpecial", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @SystemLog(description = "保存数据")
    public Result<AchieveList> create(@RequestBody AchieveList entity) {
        String appid = UserContext.getAppid();
        Integer version = 1;
        String name = entity.getName();
        String newName =  entity.getName();
        long num = iAchieveListService.countByAppidAndName(appid, entity.getName());
        while (num > 0   && version <1000){
            newName = version+"-"+name;
            if(StrUtil.length(newName) >20){
                newName = StrUtil.subWithLength(newName ,newName.length()-20, 20);
            }
            version++;
            num = iAchieveListService.countByAppidAndName(appid, newName);
        }
        if (num > 0) {
            return new ResultUtil<AchieveList>().setErrorMsg("白名单名称已被占用！");
        }
        entity.setName(newName);
        if(!checkAndResetEncryptionPassword(entity)){
            return new ResultUtil<AchieveList>().setErrorMsg("加密密钥不能为空");
        }
        AchieveList e = achieveListService.save(entity);
        return new ResultUtil<AchieveList>().setData(e);
    }

    private boolean checkAndResetEncryptionPassword(AchieveList entity){
        if (entity.getIsEncryption() == 0) {
            entity.setEncryptionPassword(StrUtil.EMPTY);
        } else {
            EncryptionMethodType type = EncryptionMethodType.getByValue(entity.getEncryptionMethod());
            if (type != null ){
                if(type.getPasswordRequired()) {
                    if(StrUtil.EMPTY.equals(entity.getEncryptionPassword())){
                        return false;
                    }
                }else {
                    entity.setEncryptionPassword(StrUtil.EMPTY);
                }
            }
        }
        return true;
    }

    @RequestMapping(value = "/query/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<NameListVo> query(@PathVariable String id) {
        AchieveList entity = achieveListService.get(id);
        NameListVo vo = new NameListVo();
        if(entity == null){
            return new ResultUtil<NameListVo>().setData(vo);
        }
        vo.setName(entity.getName());
        vo.setId(entity.getId());
        vo.setListType(entity.getListType());
        vo.setFormId(entity.getFormId());
        vo.setLinkType(entity.getLinkType());
        vo.setIsTimes(entity.getIsTimes());
        vo.setValidateFields(entity.getValidateFields());
        vo.setIsDifferentReward(entity.getIsDifferentReward());
        return new ResultUtil<NameListVo>().setData(vo);
    }

    @RequestMapping(value = "/queryByNameListVo", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "多条件获取")
    public Result<List<NameListVo>> queryByNameListVo(@RequestBody  NameListVo nameListVo){
        Map<String,Object> map = new HashMap<>();
        map.put("appid",UserContext.getAppid());
        map.put("formId",nameListVo.getFormId());
        map.put("validateFields",nameListVo.getValidateFields());
        map.put("listType",nameListVo.getListType());
        map.put("linkType",nameListVo.getLinkType());
        map.put("isTimes",nameListVo.getIsTimes());
        List<AchieveList> achieveLists = iAchieveListService.queryByMap(map);
        List<NameListVo> nameListVos = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(achieveLists)){
            for(AchieveList entity: achieveLists){
                NameListVo vo = new NameListVo();
                vo.setName(entity.getName());
                vo.setId(entity.getId());
                vo.setListType(entity.getListType());
                vo.setFormId(entity.getFormId());
                vo.setLinkType(entity.getLinkType());
                vo.setIsTimes(entity.getIsTimes());
                vo.setValidateFields(entity.getValidateFields());
                vo.setIsDifferentReward(entity.getIsDifferentReward());
                nameListVos.add(vo);
            }
        }
        return new ResultUtil<List<NameListVo>>().setData(nameListVos);
    }

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<NameListValidateResultVo> validate(@RequestParam String listId, @RequestParam String record, @RequestParam(required = false) String openId) {
        NameListValidateResultVo result = achieveListRecordService.verify(listId, record, openId);
        return new ResultUtil<NameListValidateResultVo>().setData(result);
    }
    @RequestMapping(value = "/validateRecord", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<NameListValidateResultVo> validateRecord(@RequestParam String listId, @RequestParam String record){
        AchieveList entity = achieveListService.get(listId);
        if(null == entity){
            return new ResultUtil<NameListValidateResultVo>().setErrorMsg("名单不存在");
        }
        NameListValidateResultVo result = achieveListRecordService.validateByCache(entity,record);
        return new ResultUtil<NameListValidateResultVo>().setData(result);
    }

    @RequestMapping(value = "/saveListRecord", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加限制名单名单")
    @Transactional(rollbackFor = Exception.class)
    public Result<AchieveListRecord> saveLimitListRecord(@RequestBody OfflineRecordingInputVo offlineRecordingInputVo){
        AchieveList achieveList = achieveListService.get(offlineRecordingInputVo.getListId());
        if(null == achieveList){
            return new ResultUtil<AchieveListRecord>().setErrorMsg("名单不存在");
        }
        AchieveListRecord w = new AchieveListRecord();

        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = offlineRecordingInputVo.getOfflineRecordingFieldVos();
        String identifier = "";
        if(achieveList.getListType() == 1 || achieveList.getListType() == 5){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("openid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(achieveList.getListType() == 2){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("phone")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(achieveList.getListType() == 3){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("accountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(achieveList.getListType() == 4){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("actAccountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if (achieveList.getIsEncryption() == 0 && achieveList.getListType() == 2) {
            identifier = AESUtil.encrypt(identifier);
        }
        // 加密，转大写
        if(achieveList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        BigDecimal times=BigDecimal.ZERO;
        if ( achieveList.getIsTimes() == 1 ){
            String timesStr = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("times")).collect(Collectors.toList()).get(0).getFieldData();
            if(StringUtils.isNumeric(timesStr)){
                //times = Integer.valueOf(timesStr);
                times =new BigDecimal(timesStr);
            }
        }
        w.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
        w.setListId(achieveList.getId());
        w.setCreateTime(new Date());
        w.setUpdateTime(new Date());
        w.setCreateBy("");
        w.setUpdateBy("");
        w.setTimes(times);
        List<AchieveListExtendRecord> achieveListExtendRecords = new ArrayList<>();
        if( achieveList.getListType() == 0 ){
            List<AccountFormMeta> accountFormMetas = accountFormMetaService.findListByAccountFormId(achieveList.getFormId());
            String validateFields = achieveList.getValidateFields();
            String[] validateFieldes = validateFields.split(",");
            for(String metaId: validateFieldes ){
                OfflineRecordingFieldVo offlineRecordingFieldVo = offlineRecordingFieldVos.stream() .filter(item -> item.getMetaId().equals(metaId)).collect(Collectors.toList()).get(0);
                AchieveListExtendRecord achieveListExtendRecord = new AchieveListExtendRecord();
                AccountFormMeta accountFormMeta = accountFormMetas.stream() .filter(item -> item.getId().equals(offlineRecordingFieldVo.getMetaId())).collect(Collectors.toList()).get(0);
                achieveListExtendRecord.setListId(achieveList.getId());
                achieveListExtendRecord.setFormMetaId(offlineRecordingFieldVo.getMetaId());
                achieveListExtendRecord.setMetaCode(accountFormMeta.getMetaType());
                achieveListExtendRecord.setMetaTitle(accountFormMeta.getTitle());
                achieveListExtendRecord.setRecordId(w.getId());
                achieveListExtendRecord.setCreateBy("");
                achieveListExtendRecord.setUpdateBy("");
                achieveListExtendRecord.setCreateTime(new Date());
                achieveListExtendRecord.setUpdateTime(new Date());
                achieveListExtendRecord.setRecord(AESUtil.encrypt(offlineRecordingFieldVo.getFieldData().trim()));
                achieveListExtendRecords.add(achieveListExtendRecord);
            }
            w.setExtendInfo(achieveListExtendRecords);
            identifier = achieveListRecordService.getMd5identifier(achieveList,w);
        }
        w.setIdentifier(identifier);
        if(!offlineRecordingInputVo.getMultipleAllowed()){
            Boolean existence = achieveListRecordService.existsByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            if(existence){
                return new ResultUtil<AchieveListRecord>().setErrorMsg("已在名单内");
            }
        } else {
            AchieveListRecord achieveListRecord = achieveListRecordService.findByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            if(null != achieveListRecord) {
                //w.setTimes(times + achieveListRecord.getTimes());
                w.setTimes(times.add (achieveListRecord.getTimes()));
                w.setId(achieveListRecord.getId());
            }
        }
        achieveListRecordService.update(w);
        if(CollectionUtil.isNotEmpty(achieveListExtendRecords)){
            for(AchieveListExtendRecord achieveListExtendRecord:achieveListExtendRecords){
                achieveListExtendRecord.setIdentifier(identifier);
                achieveListExtendRecord.setRecordId(w.getId());
            }
            achieveListExtendRecordService.deleteByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            achieveListExtendRecordService.saveOrUpdateAll(achieveListExtendRecords);
        }
        achieveListRecordService.loadSingleCache(achieveList.getId(),w);
        //发送mq达标用户导入后推送到act用户
        MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<AchieveListRecord>();
        mqMessageAchieveListRecord.setAppid(UserContext.getAppid());
        mqMessageAchieveListRecord.setTenantId(UserContext.getTenantId());
        mqMessageAchieveListRecord.setContent(w);
        rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);
        return new ResultUtil<AchieveListRecord>().setData(w);
    }

    @RequestMapping(value = "/saveAchieveListRecord", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加达标名单,仅添加至达标名单")
    @Transactional(rollbackFor = Exception.class)
    public Result<AchieveListRecord> saveAchieveListRecord(@RequestBody OfflineRecordingInputVo offlineRecordingInputVo){
        AchieveList achieveList = achieveListService.get(offlineRecordingInputVo.getListId());
        if(null == achieveList){
            return new ResultUtil<AchieveListRecord>().setErrorMsg("名单不存在");
        }
        AchieveListRecord w = new AchieveListRecord();

        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = offlineRecordingInputVo.getOfflineRecordingFieldVos();
        for (OfflineRecordingFieldVo offlineRecordingFieldVo: offlineRecordingFieldVos){
            if (StrUtil.isBlank(offlineRecordingFieldVo.getFieldData()))  {
                return new ResultUtil<AchieveListRecord>().setErrorMsg(offlineRecordingFieldVo.getMetaName()+"值不能为空！");
            }
        }

        String identifier = "";
        if(achieveList.getListType() == 1 || achieveList.getListType() == 5){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("openid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(achieveList.getListType() == 2){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("phone")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(achieveList.getListType() == 3){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("accountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(achieveList.getListType() == 4){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("actAccountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if (achieveList.getIsEncryption() == 0 && achieveList.getListType() == 2) {
            identifier = AESUtil.encrypt(identifier);
        }
        // 加密，转大写
        if(achieveList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        BigDecimal times = BigDecimal.ONE;

        w.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
        w.setListId(achieveList.getId());
        w.setCreateTime(new Date());
        w.setUpdateTime(new Date());
        w.setCreateBy("");
        w.setUpdateBy("");
        w.setTimes(times);
        w.setIdentifier(identifier);

        achieveListRecordService.update(w);
        achieveListRecordService.loadSingleCache(achieveList.getId(),w);
        //发送mq达标用户导入后推送到act用户
        MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<AchieveListRecord>();
        mqMessageAchieveListRecord.setAppid(UserContext.getAppid());
        mqMessageAchieveListRecord.setTenantId(UserContext.getTenantId());
        mqMessageAchieveListRecord.setContent(w);
        rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);
        return new ResultUtil<AchieveListRecord>().setData(w);
    }

    @RequestMapping(value = "/deleteByListIdAndIdentifier", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过ListId和identifier删除")
    @Transactional(rollbackFor = Exception.class)
    @SystemLog(description = "通过ListId和identifier删除")
    public Result<Object> deleteByListIdAndIdentifier(@RequestParam String listId,@RequestParam String identifier) {
        List<String> md5 = new ArrayList<>();
        AchieveList achieveList = achieveListService.get(listId);
        if(null == achieveList){
            return new ResultUtil<Object>().setErrorMsg("名单不存在");
        }
        if (achieveList.getIsEncryption() == 0 && achieveList.getListType() == 2) {
            identifier = AESUtil.encrypt(identifier);
        }
        // 加密，转大写
        if(achieveList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        AchieveListRecord record = achieveListRecordService.findByListIdAndIdentifier(listId,identifier);
        md5.add(record.getIdentifier());
        String recordId = record.getId();
        achieveListRecordService.delete(record);
        List<AchieveListExtendRecord> extrecords = achieveListExtendRecordService.findByListIdAndRecordId(listId, recordId);
        if (extrecords.size() > 0) {
            String[] deleteIds = new String[extrecords.size()];
            for(int i = 0 ;i<extrecords.size() ;i++){
                deleteIds[i] = extrecords.get(i).getId();
            }
            achieveListExtendRecordService.delete(deleteIds);
        }

        if(StringUtils.isNotBlank(listId)&&md5.size()>0 ){
            achieveListRecordService.removeCache(listId,md5);
        }
        return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
    }


    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据")
    public Result<List<NameListVo>> listAll() {
        List<AchieveList> list = achieveListService.findList(UserContext.getAppid());
        List<NameListVo> listVos = new ArrayList<>();
        list.forEach(entity -> {
            NameListVo vo = new NameListVo();
            vo.setName(entity.getName());
            vo.setId(entity.getId());
            vo.setListType(entity.getListType());
            vo.setFormId(entity.getFormId());
            vo.setLinkType(entity.getLinkType());
            vo.setIsTimes(entity.getIsTimes());
            vo.setIsDifferentReward(entity.getIsDifferentReward());
            vo.setValidateFields(entity.getValidateFields());
            listVos.add(vo);
        });
        Collator collator = Collator.getInstance(Locale.CHINESE);
        listVos.sort((o1, o2) -> collator.compare(o1.getName(), o2.getName()));
        return new ResultUtil<List<NameListVo>>().setData(listVos);
    }




    @RequestMapping(value = "/getcardaccount/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<String> getcardaccount(@PathVariable String id) {
       // String cacheKey ="AchieveListRecord:" +id;
        Integer count=0;
        count=iAchieveListRecordService.stockByListId(id);
        if (count==null)
        {
            count=0;
        }
        return new ResultUtil<String>().setData(count.toString());
    }

    @RequestMapping(value = "/getredpackamount/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<String> getredpackamount(@PathVariable String id) {
        // String cacheKey ="AchieveListRecord:" +id;
        BigDecimal count=BigDecimal.ZERO;
        count=iAchieveListRecordService.getredpackamount(id);
        if (count==null)
        {
            count=BigDecimal.ZERO;
        }
        return new ResultUtil<String>().setData(count.toString());
    }

    @RequestMapping(value = "/pushAchieveInfo", method = RequestMethod.GET)
    @ApiOperation(value = "推送达标次数信息")
    @ResponseBody
    public Result<String> pushAchieveInfo(@RequestParam String listId, @RequestParam String record, @RequestParam String times) {
        //往达标名单里面塞数据
        AchieveList achieveList = achieveListService.get(listId);
        if (null == achieveList) {
            return new ResultUtil<String>().setErrorMsg("达标名单不存在");
        }
        Integer listType = achieveList.getListType();
        if ((ListTypeConstant.OPENID.equals(listType) || ListTypeConstant.ACCOUNT_OPENID.equals(listType) || ListTypeConstant.PHONE.equals(listType) || ListTypeConstant.ACCOUNTID.equals(listType) || ListTypeConstant.ACTACCOUNTID.equals(listType)) && StrUtil.isBlank(achieveList.getLinkId()) && achieveList.getIsDifferentReward().equals((byte) 0)) {
            //将下面的代码封成一个方法
            achieveListRecordService.handlePushAchieveTimes(listId, record, times);
            return new ResultUtil<String>().setData("OK");
        } else {
            return new ResultUtil<String>().setErrorMsg("不支持该类型达标名单");
        }
    }


    @RequestMapping(value = "/pushAchInfo", method = RequestMethod.GET)
    @ApiOperation(value = "推送达标用户信息")
    @ResponseBody
    public Result<String> pushAchInfo(@RequestParam String listId, @RequestParam String record) {
        //往达标名单里面塞数据
        AchieveList achieveList = achieveListService.get(listId);
        if (null == achieveList) {
            return new ResultUtil<String>().setErrorMsg("达标名单不存在");
        }
        Integer listType = achieveList.getListType();
        if ((ListTypeConstant.OPENID.equals(listType) || ListTypeConstant.ACCOUNT_OPENID.equals(listType) || ListTypeConstant.PHONE.equals(listType) || ListTypeConstant.ACCOUNTID.equals(listType) || ListTypeConstant.ACTACCOUNTID.equals(listType)) && StrUtil.isBlank(achieveList.getLinkId()) && achieveList.getIsDifferentReward().equals((byte) 0)) {
            //将下面的代码封成一个方法
            achieveListRecordService.handlePushAchieve(listId, record);
            return new ResultUtil<String>().setData("OK");
        } else {
            return new ResultUtil<String>().setErrorMsg("不支持该类型达标名单");
        }
    }


    @RequestMapping(value = "/recordListForSZBank", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据达标名单Id，获取达标名单清单")
    public Result<List<AchieveListRecordDataVo>> getRecordListByWhiteListId(@RequestParam String achieveListId, @RequestParam(required = false) String nextId) {
        AchieveList achieveList = achieveListService.get(achieveListId);
        Integer listType = achieveList.getListType();
        List<AchieveListRecordDataVo> achieveListRecordVoList = achieveListRecordService.findByAchieveListIdAndNextId(achieveListId, nextId);
        if(ListTypeConstant.PHONE.equals(listType)){
            //先解密，再加密
            if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
                for (AchieveListRecordDataVo vo : achieveListRecordVoList) {
                    vo.setIdentifier(AESUtil.comEncrypt(AESUtil.decrypt(vo.getIdentifier())));
                }
            }
        }
        return new ResultUtil<List<AchieveListRecordDataVo>>().setData(achieveListRecordVoList);
    }

}
