package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.constant.LinkTypeConstant;
import com.ytdinfo.inndoo.common.constant.ListTypeConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.enums.EncryptionMethodType;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.DateUtils;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListService;
import com.ytdinfo.inndoo.vo.NameListVo;
import com.ytdinfo.inndoo.vo.SpecialNameListVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
@Api(description = "白名单接口")
@RequestMapping("/whitelist")
@APIModifier(APIModifierType.PRIVATE)
public class WhiteListController {

    @Autowired
    private WhiteListService whiteListService;

    @Autowired
    private LimitListService limitListService;

    @Autowired
    private AchieveListService achieveListService;

    @Autowired
    private WhiteListRecordService whiteListRecordService;

    @Autowired
    private LimitListRecordService limitListRecordService;

    @Autowired
    private WhiteListExtendRecordService whiteListExtendRecordService;
    @Autowired
    private AchieveListExtendRecordService achieveListExtendRecordService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private IWhiteListService iWhiteListService;

    @Autowired
    private ActAccountService actAccountService;

    @RequestMapping(value = "/querySpecial/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<SpecialNameListVo> querySpecial(@PathVariable String id) {
        WhiteList entity = whiteListService.get(id);
        if(entity == null){
            return new ResultUtil<SpecialNameListVo>().setData(null);
        }
        SpecialNameListVo specialNameListVo =  new SpecialNameListVo();
        BeanUtil.copyProperties(entity,specialNameListVo);
        return new ResultUtil<SpecialNameListVo>().setData(specialNameListVo);
    }


    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id删除数据")
    @SystemLog(description = "通过id删除数据")
    public Result<Object> deleteById(@PathVariable String id) {
        whiteListService.delete(id);
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }

    @RequestMapping(value = "/createSpecial", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @SystemLog(description = "保存数据")
    public Result<WhiteList> create(@RequestBody WhiteList entity) {
        String appid = UserContext.getAppid();
        Integer version = 1;
        String name = entity.getName();
        String newName =  entity.getName();
        long num = iWhiteListService.countByAppidAndName(appid, entity.getName());
        while (num > 0   && version <1000){
            newName = version+"-"+name;
            if(StrUtil.length(newName) >50){
                newName = StrUtil.subWithLength(newName ,newName.length()-50, 50);
            }
            version++;
            num = iWhiteListService.countByAppidAndName(appid, newName);
        }
        if (num > 0) {
            return new ResultUtil<WhiteList>().setErrorMsg("白名单名称已被占用！");
        }
        entity.setName(newName);
        if(!checkAndResetEncryptionPassword(entity)){
            return new ResultUtil<WhiteList>().setErrorMsg("加密密钥不能为空");
        }
        WhiteList e = whiteListService.save(entity);
        return new ResultUtil<WhiteList>().setData(e);
    }

    private boolean checkAndResetEncryptionPassword(WhiteList entity){
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
        WhiteList entity = whiteListService.get(id);
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
        return new ResultUtil<NameListVo>().setData(vo);
    }

    @RequestMapping(value = "/queryByName", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过name获取")
    public Result<NameListVo> queryByName(@RequestParam String name) {
        WhiteList entity = whiteListService.findByName(name);
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
        List<WhiteList> whiteLists = iWhiteListService.queryByMap(map);
        List<NameListVo> nameListVos = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(whiteLists)){
            for(WhiteList entity: whiteLists){
                NameListVo vo = new NameListVo();
                vo.setName(entity.getName());
                vo.setId(entity.getId());
                vo.setListType(entity.getListType());
                vo.setFormId(entity.getFormId());
                vo.setLinkType(entity.getLinkType());
                vo.setIsTimes(entity.getIsTimes());
                vo.setValidateFields(entity.getValidateFields());
                nameListVos.add(vo);
            }
        }
        return new ResultUtil<List<NameListVo>>().setData(nameListVos);
    }

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<NameListValidateResultVo> validate(@RequestParam String listId, @RequestParam String record, @RequestParam(required = false) String openId) {
        NameListValidateResultVo result = whiteListRecordService.verify(listId, record, openId);
        return new ResultUtil<NameListValidateResultVo>().setData(result);
    }

    @RequestMapping(value = "/validateRecord", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<NameListValidateResultVo> validateRecord(@RequestParam String listId, @RequestParam String record){
        WhiteList entity = whiteListService.get(listId);
        if(null == entity){
            return new ResultUtil<NameListValidateResultVo>().setErrorMsg("名单不存在");
        }
        NameListValidateResultVo result = whiteListRecordService.validateByCache(entity,record);
        return new ResultUtil<NameListValidateResultVo>().setData(result);
    }

    @RequestMapping(value = "/saveListRecord", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加白名单")
    @Transactional(rollbackFor = Exception.class)
    public Result<WhiteListRecord> saveWhiteListRecord(@RequestBody OfflineRecordingInputVo offlineRecordingInputVo){
        WhiteList whiteList = whiteListService.get(offlineRecordingInputVo.getListId());
        if(null == whiteList){
            return new ResultUtil<WhiteListRecord>().setErrorMsg("名单不存在");
        }
        WhiteListRecord w = new WhiteListRecord();

        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = offlineRecordingInputVo.getOfflineRecordingFieldVos();
        String identifier = "";
        if(whiteList.getListType() == 1 || whiteList.getListType() == 5){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("openid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(whiteList.getListType() == 2){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("phone")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(whiteList.getListType() == 3){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("accountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(whiteList.getListType() == 4){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("actAccountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if (whiteList.getIsEncryption() == 0 && whiteList.getListType() == 2) {
            identifier = AESUtil.encrypt(identifier);
        }
        // 加密，转大写
        if(whiteList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        Integer times=0;
        if ( whiteList.getIsTimes() == 1 ){
            String timesStr = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("times")).collect(Collectors.toList()).get(0).getFieldData();
            if(StringUtils.isNumeric(timesStr)){
                times = Integer.valueOf(timesStr);
            }
        }
        w.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
        w.setListId(whiteList.getId());
        w.setCreateTime(new Date());
        w.setUpdateTime(new Date());
        w.setTimes(times);
        List<WhiteListExtendRecord> whiteListExtendRecords = new ArrayList<>();
        if( whiteList.getListType() == 0 ){
            List<AccountFormMeta> accountFormMetas = accountFormMetaService.findListByAccountFormId(whiteList.getFormId());
            String validateFields = whiteList.getValidateFields();
            String[] validateFieldes = validateFields.split(",");
            for(String metaId: validateFieldes ){
                OfflineRecordingFieldVo offlineRecordingFieldVo = offlineRecordingFieldVos.stream() .filter(item -> item.getMetaId().equals(metaId)).collect(Collectors.toList()).get(0);
                WhiteListExtendRecord whiteListExtendRecord = new WhiteListExtendRecord();
                AccountFormMeta accountFormMeta = accountFormMetas.stream() .filter(item -> item.getId().equals(offlineRecordingFieldVo.getMetaId())).collect(Collectors.toList()).get(0);
                whiteListExtendRecord.setListId(whiteList.getId());
                whiteListExtendRecord.setFormMetaId(offlineRecordingFieldVo.getMetaId());
                whiteListExtendRecord.setMetaCode(accountFormMeta.getMetaType());
                whiteListExtendRecord.setMetaTitle(accountFormMeta.getTitle());
                whiteListExtendRecord.setRecordId(w.getId());
                whiteListExtendRecord.setRecord(AESUtil.encrypt(offlineRecordingFieldVo.getFieldData().trim()));
                whiteListExtendRecords.add(whiteListExtendRecord);
            }
            w.setExtendInfo(whiteListExtendRecords);
            identifier = whiteListRecordService.getMd5identifier(whiteList,w);

        }
        w.setIdentifier(identifier);
        if(!offlineRecordingInputVo.getMultipleAllowed()){
            Boolean existence = whiteListRecordService.existsByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            if(existence){
                return new ResultUtil<WhiteListRecord>().setErrorMsg("已在名单内");
            }
        }else {
            WhiteListRecord whiteListRecord = whiteListRecordService.findByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            if(null != whiteListRecord) {
                w.setTimes(times + whiteListRecord.getTimes());
                w.setId(whiteListRecord.getId());
            }
        }
        whiteListRecordService.update(w);
        if(CollectionUtil.isNotEmpty(whiteListExtendRecords)){
            for(WhiteListExtendRecord whiteListExtendRecord: whiteListExtendRecords){
                whiteListExtendRecord.setRecordId(w.getId());
                whiteListExtendRecord.setIdentifier(identifier);
            }
            whiteListExtendRecordService.deleteByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            whiteListExtendRecordService.saveOrUpdateAll(whiteListExtendRecords);
        }
        whiteListRecordService.loadSingleCache(whiteList.getId(),w);
        return new ResultUtil<WhiteListRecord>().setData(w);
    }


    @RequestMapping(value = "/deleteByListIdAndIdentifier", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过ListId和identifier删除")
    @Transactional(rollbackFor = Exception.class)
    @SystemLog(description = "通过ListId和identifier删除")
    public Result<Object> deleteByListIdAndIdentifier(@RequestParam String listId,@RequestParam String identifier) {
        List<String> md5 = new ArrayList<>();
        WhiteList whiteList = whiteListService.get(listId);
        if(null == whiteList){
            return new ResultUtil<Object>().setErrorMsg("名单不存在");
        }
        if (whiteList.getIsEncryption() == 0 && whiteList.getListType() == 2) {
            identifier = AESUtil.encrypt(identifier);
        }
        // 加密，转大写
        if(whiteList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        WhiteListRecord record = whiteListRecordService.findByListIdAndIdentifier(listId,identifier);
        md5.add(record.getIdentifier());
        String recordId = record.getId();
        whiteListRecordService.delete(record);
        List<WhiteListExtendRecord> extrecords = whiteListExtendRecordService.findByListIdAndRecordId(listId, recordId);
        if (extrecords.size() > 0) {
            String[] deleteIds = new String[extrecords.size()];
            for(int i = 0 ;i<extrecords.size() ;i++){
                deleteIds[i] = extrecords.get(i).getId();
            }
            whiteListExtendRecordService.delete(deleteIds);
        }

        if(StringUtils.isNotBlank(listId)&&md5.size()>0 ){
            whiteListRecordService.removeCache(listId,md5);
        }
        return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据")
    public Result<List<NameListVo>> listAll() {
        List<WhiteList> list = whiteListService.findList(UserContext.getAppid());
        List<NameListVo> listVos = new ArrayList<>();
        list.forEach(entity -> {
            NameListVo vo = new NameListVo();
            vo.setName(entity.getName());
            vo.setId(entity.getId());
            vo.setListType(entity.getListType());
            vo.setFormId(entity.getFormId());
            vo.setLinkType(entity.getLinkType());
            vo.setIsTimes(entity.getIsTimes());
            vo.setValidateFields(entity.getValidateFields());
            listVos.add(vo);
        });
        Collator collator = Collator.getInstance(Locale.CHINESE);
        listVos.sort((o1, o2) -> collator.compare(o1.getName(), o2.getName()));
        return new ResultUtil<List<NameListVo>>().setData(listVos);
    }

    @RequestMapping(value = "/recordList", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据白名单Id，获取白名单清单")
    public Result<WhiteListResultVo> getRecordListByWhiteListId(@RequestParam String whiteListId, @RequestParam(required = false) String nextId){
        WhiteListResultVo whiteListResultVo = new WhiteListResultVo();
        WhiteList whiteList = whiteListService.get(whiteListId);
        if (BeanUtil.isEmpty(whiteList)) {
            whiteListResultVo.setErrMsg("白名单不存在");
        } else if (whiteList.getExpireDate().before(new Date())) {
            whiteListResultVo.setErrMsg("白名单已过期");
        } else {
            Byte linkType = whiteList.getLinkType();
            String linkId = whiteList.getLinkId();
            if (linkType == 0){
                whiteListResultVo = whiteListRecordService.findByWhiteListAndNextId(whiteList, nextId);
            }else if (linkType == LinkTypeConstant.WHITE){
                WhiteList linkWhiteList = whiteListService.get(linkId);
                if (BeanUtil.isEmpty(linkWhiteList)){
                    whiteListResultVo.setErrMsg("关联的白名单不存在");
                }else if (linkWhiteList.getExpireDate().before(new Date())){
                    whiteListResultVo.setErrMsg("关联的白名单已过期");
                }else {
                    whiteListResultVo = whiteListRecordService.findByWhiteListAndNextId(linkWhiteList, nextId);
                }
            }else if (linkType == LinkTypeConstant.LIMIT){
                LimitList linkLimitList = limitListService.get(linkId);
                if (BeanUtil.isEmpty(linkLimitList)){
                    whiteListResultVo.setErrMsg("关联的限制名单不存在");
                }else if (linkLimitList.getExpireDate().before(new Date())){
                    whiteListResultVo.setErrMsg("关联的限制名单已过期");
                }else {
                    whiteListResultVo = limitListRecordService.findByLimitListAndNextId(linkLimitList, nextId);
                }
            }else if (linkType == LinkTypeConstant.ACHIEVE){
                AchieveList linkAchieveList = achieveListService.get(linkId);
                if (BeanUtil.isEmpty(linkAchieveList)){
                    whiteListResultVo.setErrMsg("关联的达标名单不存在");
                }else if (linkAchieveList.getExpireDate().before(new Date())){
                    whiteListResultVo.setErrMsg("关联的达标名单已过期");
                }else {
                    whiteListResultVo = achieveListRecordService.findByAchieveListAndNextId(linkAchieveList, nextId);
                }
            }
        }
        return new ResultUtil<WhiteListResultVo>().setData(whiteListResultVo);
    }

    @RequestMapping(value = "/deleteRecord", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "删除白名单类型为accountId的名单")
    public Result<Boolean> deleteRecord(@RequestParam String listId, @RequestParam String actAccountId){
        actAccountId = AESUtil.decrypt(actAccountId,AESUtil.WXLOGIN_PASSWORD);
        WhiteList whiteList = whiteListService.get(listId);
        if(whiteList.getListType() != 3){
            return new ResultUtil<Boolean>().setErrorMsg("只能删除白名单类型为accountId的白名单");
        }
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if(null == actAccount){
            return new ResultUtil<Boolean>().setErrorMsg("您的账户未注册");
        }
        String identifier = actAccount.getCoreAccountId();
        if (whiteList.getIsEncryption() == 0 && whiteList.getListType() == 2) {
            identifier = AESUtil.encrypt(identifier);
        }
        // 加密，转大写
        if(whiteList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        WhiteListRecord whiteListRecord = whiteListRecordService.findByListIdAndIdentifier(listId,identifier);
        whiteListRecordService.delete(whiteListRecord);
        whiteListExtendRecordService.deleteByListIdAndIdentifier(listId,identifier);
        List<String> identifiers = new ArrayList<>();
        identifiers.add(identifier);
        whiteListRecordService.removeCache(listId,identifiers);
        return new ResultUtil<Boolean>().setData(true);
    }

    @RequestMapping(value = "/pushListRecord", method = RequestMethod.GET)
    @ApiOperation(value = "推送白名单record信息")
    @ResponseBody
    public Result<String> pushAchieveInfo(@RequestParam String listId, @RequestParam String record, @RequestParam(required = false) String times) {
        //往白名单里面塞数据
        WhiteList whiteList = whiteListService.get(listId);
        if (null == whiteList) {
            return new ResultUtil<String>().setErrorMsg("白名单不存在");
        }
        Integer listType = whiteList.getListType();
        if ((ListTypeConstant.OPENID.equals(listType) || ListTypeConstant.ACCOUNT_OPENID.equals(listType)) && StrUtil.isBlank(whiteList.getLinkId()) && whiteList.getIsTimes().equals((byte) 0)) {
            //将下面的代码封成一个方法
            whiteListRecordService.handlePushListRecord(listId, record, times);

        }
        if (ListTypeConstant.PHONE.equals(listType))
        {
            if (StringUtils.isNotBlank(times))
            {
                whiteListRecordService.handlePushListRecord(listId, record, times);
            }
            else {
                //将下面的代码封成一个方法
                whiteListRecordService.handlePushListRecord(listId, record, "0");
            }
        }
        if (ListTypeConstant.ACTACCOUNTID.equals(listType)) {
            if (!whiteListRecordService.existsByListIdAndIdentifier(listId,record)) {
                //将下面的代码封成一个方法
                whiteListRecordService.handlePushListRecord(listId, record, "0");
            }
        }
        return new ResultUtil<String>().setData("SUCCESS");
    }


    @RequestMapping(value = "/addUpTimes", method = RequestMethod.GET)
    @ApiOperation(value = "白名单次数累加")
    @ResponseBody
    public Result<String> addUpTimes(@RequestParam String listId, @RequestParam String record, @RequestParam String times) {
        //往白名单里面塞数据
        WhiteList whiteList = whiteListService.get(listId);
        if (null == whiteList) {
            return new ResultUtil<String>().setErrorMsg("白名单不存在");
        }
        Integer listType = whiteList.getListType();
        if (ListTypeConstant.OPENID.equals(listType) || ListTypeConstant.ACCOUNT_OPENID.equals(listType) || ListTypeConstant.PHONE.equals(listType)) {
            //将下面的代码封成一个方法
            whiteListRecordService.AddUpTimesPushListRecord(listId, record, times);
        }
        return new ResultUtil<String>().setData("SUCCESS");
    }



    @RequestMapping(value = "/saveWhiteList", method = RequestMethod.GET)
    @ApiOperation(value = "选择标签推送消息存到小核心的白名单中")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveWhiteList(@RequestParam String tagKeyType, @RequestParam String reqId, @RequestParam String name) {
        if(StrUtil.isEmpty(tagKeyType) || StrUtil.isEmpty(reqId)){
            return new ResultUtil<String>().setErrorMsg("tagKeyType或reqId未传值");
        }
        //看看reqId能不能查到白名单
        WhiteList whiteList = whiteListService.get(reqId);
        if (null == whiteList) {
            WhiteList whiteListNew = new WhiteList();
            whiteListNew.setName(name+"-白名单推送");
            if(Integer.parseInt(tagKeyType) == 2){//OpenId
                whiteListNew.setListType(1);
            }
            if(Integer.parseInt(tagKeyType) == 1){//Phone大小的MD5加密
                whiteListNew.setListType(2);
                whiteListNew.setIsEncryption((byte)1);
                whiteListNew.setEncryptionMethod(EncryptionMethodType.MD5.getValue());
            }
            whiteListNew.setExpireDate(DateUtils.getAfterTime(30));
            whiteListNew.setFormId("");
            whiteListNew.setId(reqId);
            whiteListNew.setCreateBy("");
            whiteListNew.setUpdateBy("");
            whiteListService.save(whiteListNew);
        }else{
            whiteList.setName(name+"-白名单推送");
            if(Integer.parseInt(tagKeyType) == 2){//OpenId
                whiteList.setListType(1);
            }
            if(Integer.parseInt(tagKeyType) == 1){//Phone大小的MD5加密
                whiteList.setListType(2);
                whiteList.setIsEncryption((byte)1);
                whiteList.setEncryptionMethod(EncryptionMethodType.MD5.getValue());
            }
            whiteListService.update(whiteList);
        }
        return new ResultUtil<String>().setData("OK");
    }

    //获取密码口令信息
    @RequestMapping(value = "/getPasswordLogin", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取密码口令信息")
    public Result<NameListVo> getPasswordLogin( @RequestParam String listId, @RequestParam String password) {
        WhiteList whiteList = whiteListService.get(listId);
        Integer listType  =whiteList.getListType();
        //1：openid，2：phone，3：小核心accountid，4:活动平台accountid"
        String aecpassword="";
        if (listType.equals(2))
        {
            aecpassword= AESUtil.encrypt(password);
        }
        else
        {
            aecpassword=password;
        }
        WhiteListRecord  whiteListRecord= whiteListRecordService.findByListIdAndIdentifier(listId,aecpassword);
        NameListVo vo = new NameListVo();
        if(whiteListRecord!=null)
        {
            if (listType.equals(2)) {
                vo.setName(AESUtil.decrypt(whiteListRecord.getIdentifier()));
            }
            else
            {
                vo.setName(whiteListRecord.getIdentifier());
            }
            vo.setListType(whiteListRecord.getTimes());
        }
        return new ResultUtil<NameListVo>().setData(vo);
    }

}
