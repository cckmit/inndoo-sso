package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.constant.LinkTypeConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.enums.EncryptionMethodType;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILimitListService;
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

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "受限名单接口")
@RequestMapping("/limitlist")

@APIModifier(APIModifierType.PRIVATE)
public class LimitListController {

    @Autowired
    private LimitListService limitListService;

    @Autowired
    private LimitListRecordService limitListRecordService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private LimitListExtendRecordService limitListExtendRecordService;
    @Autowired
    private ILimitListService iLimitListService;

    @RequestMapping(value = "/querySpecial/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<SpecialNameListVo> querySpecial(@PathVariable String id) {
        LimitList entity = limitListService.get(id);
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
        limitListService.delete(id);
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }


    @RequestMapping(value = "/createSpecial", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @SystemLog(description = "保存数据")
    public Result<LimitList> create(@RequestBody LimitList entity) {
        String appid = UserContext.getAppid();
        Integer version = 1;
        String name = entity.getName();
        String newName =  entity.getName();
        long num = iLimitListService.countByAppidAndName(appid, entity.getName());
        while (num > 0   && version <1000){
            newName = version+"-"+name;
            if(StrUtil.length(newName) >20){
                newName = StrUtil.subWithLength(newName ,newName.length()-20, 20);
            }
            version++;
            num = iLimitListService.countByAppidAndName(appid, newName);
        }
        if (num > 0) {
            return new ResultUtil<LimitList>().setErrorMsg("白名单名称已被占用！");
        }
        entity.setName(newName);
        if(!checkAndResetEncryptionPassword(entity)){
            return new ResultUtil<LimitList>().setErrorMsg("加密密钥不能为空");
        }
        LimitList e = limitListService.save(entity);
        return new ResultUtil<LimitList>().setData(e);
    }

    private boolean checkAndResetEncryptionPassword(LimitList entity){
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
        LimitList entity = limitListService.get(id);
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
        List<LimitList> limitLists = iLimitListService.queryByMap(map);
        List<NameListVo> nameListVos = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(limitLists)){
            for(LimitList entity: limitLists){
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
        NameListValidateResultVo result = limitListRecordService.verify(listId, record, openId);
        return new ResultUtil<NameListValidateResultVo>().setData(result);
    }

    @RequestMapping(value = "/validateRecord", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<NameListValidateResultVo> validateRecord(@RequestParam String listId, @RequestParam String record){
        LimitList entity = limitListService.get(listId);
        if(null == entity){
            return new ResultUtil<NameListValidateResultVo>().setErrorMsg("名单不存在");
        }
        NameListValidateResultVo result = limitListRecordService.validateByCache(entity,record);
        return new ResultUtil<NameListValidateResultVo>().setData(result);
    }
    @RequestMapping(value = "/saveListRecord", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加限制名单名单")
    @Transactional(rollbackFor = Exception.class)
    public Result<LimitListRecord> saveLimitListRecord(@RequestBody OfflineRecordingInputVo offlineRecordingInputVo){
        LimitList limitList = limitListService.get(offlineRecordingInputVo.getListId());
        LimitListRecord w = new LimitListRecord();
        if(null == limitList){
            return new ResultUtil<LimitListRecord>().setErrorMsg("名单不存在");
        }
        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = offlineRecordingInputVo.getOfflineRecordingFieldVos();
        String identifier = "";
        if(limitList.getListType() == 1 || limitList.getListType() == 5){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("openid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(limitList.getListType() == 2){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("phone")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(limitList.getListType() == 3){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("accountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if(limitList.getListType() == 4){
            identifier = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("actAccountid")).collect(Collectors.toList()).get(0).getFieldData();
        }
        if (limitList.getIsEncryption() == 0 && limitList.getListType() == 2) {
            identifier = AESUtil.encrypt(identifier);
        }
        // 加密，转大写
        if(limitList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        Integer times=0;
        if ( limitList.getIsTimes() == 1 ){
            String timesStr = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("times")).collect(Collectors.toList()).get(0).getFieldData();
            if(StringUtils.isNumeric(timesStr)){
                times = Integer.valueOf(timesStr);
            }
        }
        w.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
        w.setListId(limitList.getId());
        w.setCreateTime(new Date());
        w.setUpdateTime(new Date());
        w.setTimes(times);
        List<LimitListExtendRecord> limitListExtendRecords = new ArrayList<>();
        if( limitList.getListType() == 0 ){
            List<AccountFormMeta> accountFormMetas = accountFormMetaService.findListByAccountFormId(limitList.getFormId());
            String validateFields = limitList.getValidateFields();
            String[] validateFieldes = validateFields.split(",");

            for(String metaId: validateFieldes ){
                OfflineRecordingFieldVo offlineRecordingFieldVo = offlineRecordingFieldVos.stream() .filter(item -> item.getMetaId().equals(metaId)).collect(Collectors.toList()).get(0);
                LimitListExtendRecord limitListExtendRecord = new LimitListExtendRecord();
                AccountFormMeta accountFormMeta = accountFormMetas.stream() .filter(item -> item.getId().equals(offlineRecordingFieldVo.getMetaId())).collect(Collectors.toList()).get(0);
                limitListExtendRecord.setListId(limitList.getId());
                limitListExtendRecord.setFormMetaId(offlineRecordingFieldVo.getMetaId());
                limitListExtendRecord.setMetaCode(accountFormMeta.getMetaType());
                limitListExtendRecord.setMetaTitle(accountFormMeta.getTitle());
                limitListExtendRecord.setRecordId(w.getId());
                limitListExtendRecord.setRecord(AESUtil.encrypt(offlineRecordingFieldVo.getFieldData().trim()));
                limitListExtendRecords.add(limitListExtendRecord);
            }
            w.setExtendInfo(limitListExtendRecords);
            identifier = limitListRecordService.getMd5identifier(limitList,w);
        }
        w.setIdentifier(identifier);
        if(!offlineRecordingInputVo.getMultipleAllowed()){
            Boolean existence = limitListRecordService.existsByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            if(existence){
                return new ResultUtil<LimitListRecord>().setErrorMsg("已在名单内");
            }
        } else {
            LimitListRecord  limitListRecord = limitListRecordService.findByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            if(null != limitListRecord){
                w.setTimes(times + limitListRecord.getTimes());
                w.setId(limitListRecord.getId());
            }
        }
        limitListRecordService.update(w);
        if(CollectionUtil.isNotEmpty(limitListExtendRecords)){
            for(LimitListExtendRecord limitListExtendRecord:limitListExtendRecords){
                limitListExtendRecord.setRecordId(w.getId());
                limitListExtendRecord.setIdentifier(identifier);
            }
            limitListExtendRecordService.deleteByListIdAndIdentifier(w.getListId(),w.getIdentifier());
            limitListExtendRecordService.saveOrUpdateAll(limitListExtendRecords);
        }
        limitListRecordService.loadSingleCache(limitList.getId(),w);
        return new ResultUtil<LimitListRecord>().setData(w);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据")
    public Result<List<NameListVo>> listAll() {
        List<LimitList> list = limitListService.findList(UserContext.getAppid());
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
    @ApiOperation(value = "根据受限名单Id，获取受限名单清单")
    public Result<LimitListResultVo> getRecordListByWhiteListId(@RequestParam String limitListId, @RequestParam(required = false) String nextId){
        LimitListResultVo limitListResultVo = new LimitListResultVo();
        LimitList limitList = limitListService.get(limitListId);
        if (BeanUtil.isEmpty(limitList)) {
            limitListResultVo.setErrMsg("受限名单不存在");
        } else if (limitList.getExpireDate().before(new Date())) {
            limitListResultVo.setErrMsg("受限名单已过期");
        } else {
            Byte linkType = limitList.getLinkType();
            if (linkType == 0){
                limitListResultVo = limitListRecordService.findByLimitListAndNextId2(limitList, nextId);
            }
        }
        return new ResultUtil<LimitListResultVo>().setData(limitListResultVo);
    }

}
