package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.ExcelConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.utils.excel.WaterExcelUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListExtendRecordService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "??????????????????????????????")
@RequestMapping("/achievelistrecord")
public class AchieveListRecordController extends BaseController<AchieveListRecord, String> {
    @Autowired
    private AchieveListService achieveListService;

    @Autowired
    private AchieveListRecordService achieveListRecordService;

    @Autowired
    private AchieveListExtendRecordService achieveListExtendRecordService;

    @Autowired
    private IAchieveListRecordService iAchieveListRecordService;

    @Autowired
    private IAchieveListExtendRecordService iAchieveListExtendRecordService;

    @Autowired
    private AccountFormMetaService accountFormMetaService;

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private ExceptionLogService exceptionLogService;

    @Autowired
    private WaterExcelUtil waterExcelUtil;
    @Override
    public AchieveListRecordService getService() {
        return achieveListRecordService;
    }

    @RequestMapping(value = "/listByCondition/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????")
    @SystemLog(description = "?????????????????????")
    public Result<Page<AchieveListRecord>> listByCondition(@ModelAttribute AchieveListRecord achieveListRecord,
                                                           @ModelAttribute SearchVo searchVo,
                                                           @ModelAttribute PageVo pageVo,
                                                           @PathVariable String id) {
        String appid = UserContext.getAppid();
        achieveListRecord.setAppid(appid);
        Page<AchieveListRecord> page = achieveListRecordService.
                findByCondition(achieveListRecord, searchVo, PageUtil.initPage(pageVo), id);
        List<String> identifiers =new ArrayList<>();
        if (page.getTotalElements() > 0) {
            AchieveList achieveList = achieveListService.get(id);
            for (int i = 0; i < page.getContent().size(); i++) {
                AchieveListRecord achieveListRecord1= page.getContent().get(i);
                if (achieveList.getListType() == 2) {
                    if(achieveList.getIsEncryption() == 0){
                        page.getContent().get(i).setIdentifier(AESUtil.decrypt(achieveListRecord1.getIdentifier()) );
                    }else {
                        page.getContent().get(i).setIdentifier(achieveListRecord1.getIdentifier());
                    }
                }
                if(achieveList.getListType() == 0){
                    identifiers.add(achieveListRecord1.getIdentifier());
                }
                page.getContent().get(i).setListType(achieveList.getListType());
            }
            if(achieveList.getListType() == 0 && identifiers.size()>0){
                List<AchieveListExtendRecord>  records = achieveListExtendRecordService.findByListIdAndIdentifierIn(id, identifiers);
                List<AchieveListExtendRecord> achieveListExtendRecords = new ArrayList<>();
                achieveListExtendRecords.addAll(records);
                Map<String,List<AchieveListExtendRecord>> map = new HashMap<>();
                for( AchieveListExtendRecord achieveListExtendRecord1: achieveListExtendRecords){
                    AchieveListExtendRecord achieveListExtendRecord = new AchieveListExtendRecord();
                    BeanUtils.copyProperties(achieveListExtendRecord1,achieveListExtendRecord);
                    List<AchieveListExtendRecord>  tempList=  map.get(achieveListExtendRecord.getIdentifier());
                    if(tempList == null){
                        tempList = new ArrayList<>();
                    }
                    achieveListExtendRecord.setRecord(AESUtil.decrypt(achieveListExtendRecord.getRecord()));
                    tempList.add(achieveListExtendRecord);
                    map.put(achieveListExtendRecord.getIdentifier(),tempList);
                }

                for (int i = 0; i < page.getContent().size(); i++) {
                    String tempIdentifier =  page.getContent().get(i).getIdentifier();
                    List<AchieveListExtendRecord>  tempList=  map.get(tempIdentifier);
                    if( tempList !=null){
                        page.getContent().get(i).setExtendInfo(tempList);
                    }
                }
            }
        }
        return new ResultUtil<Page<AchieveListRecord>>().setData(page);
    }

    /**
     * ??????OfflineRecordingInputVo
     * @return
     */
    @RequestMapping(value = "/getOfflineRecordingInputVo", method = RequestMethod.GET)
    @ApiOperation(value = "????????????????????????")
    public Result<OfflineRecordingInputVo> getOfflineRecordingInputVo( @RequestParam String listId ){
        AchieveList achieveList = achieveListService.get(listId);
        if(null == achieveList){
            return new ResultUtil<OfflineRecordingInputVo>().setErrorMsg("?????????????????????");
        }
        OfflineRecordingInputVo offlineRecordingInputVo = new OfflineRecordingInputVo();
        offlineRecordingInputVo.setListId(listId);
        offlineRecordingInputVo.setListType(achieveList.getListType().toString());
        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = new ArrayList<>();
        if(achieveList.getListType() == 1 || achieveList.getListType() == 5){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(achieveList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("openid");
            offlineRecordingFieldVo.setMetaName("openid");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if(achieveList.getListType() == 2){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(achieveList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("phone");
            offlineRecordingFieldVo.setMetaName("?????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if (achieveList.getListType() == 3 ){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(achieveList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("accountid");
            offlineRecordingFieldVo.setMetaName("???????????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        }  if (achieveList.getListType() == 4 ){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(achieveList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("actAccountid");
            offlineRecordingFieldVo.setMetaName("??????????????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if (achieveList.getListType() == 0 ) {
            String validateFields = achieveList.getValidateFields();
            String[] validateFieldes = validateFields.split(",");
            for (String validateField : validateFieldes){
                if(StrUtil.isNotBlank(validateField)){
                    AccountFormMeta accountFormMeta = accountFormMetaService.get(validateField);
                    if(null != accountFormMeta) {
                        OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
                        offlineRecordingFieldVo.setMetaId(validateField);
                        offlineRecordingFieldVo.setListId(listId);
                        offlineRecordingFieldVo.setListType(achieveList.getListType().toString());
                        offlineRecordingFieldVo.setFieldData("");
                        offlineRecordingFieldVo.setMetaType(accountFormMeta.getMetaType());
                        offlineRecordingFieldVo.setMetaName(accountFormMeta.getTitle());
                        offlineRecordingFieldVos.add(offlineRecordingFieldVo);
                    }
                }
            }
        }
        if(achieveList.getIsTimes().intValue() == 1){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(achieveList.getListType().toString());
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setMetaType("times");
            offlineRecordingFieldVo.setMetaName("????????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        }
        if(achieveList.getIsDifferentReward().intValue() == 1){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(achieveList.getListType().toString());
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setMetaType("times");
            offlineRecordingFieldVo.setMetaName("?????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        }


        offlineRecordingInputVo.setOfflineRecordingFieldVos(offlineRecordingFieldVos);
        return new ResultUtil<OfflineRecordingInputVo>().setData(offlineRecordingInputVo);
    }


    @RequestMapping(value = "/saveListRecord", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "??????????????????")
    @Transactional(rollbackFor = Exception.class)
    public Result<AchieveListRecord> saveLimitListRecord(@RequestBody OfflineRecordingInputVo offlineRecordingInputVo){
        AchieveList achieveList = achieveListService.get(offlineRecordingInputVo.getListId());
        if(null == achieveList){
            return new ResultUtil<AchieveListRecord>().setErrorMsg("???????????????");
        }
        AchieveListRecord w = new AchieveListRecord();

        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = offlineRecordingInputVo.getOfflineRecordingFieldVos();
        for (OfflineRecordingFieldVo offlineRecordingFieldVo: offlineRecordingFieldVos){
            if (StrUtil.isBlank(offlineRecordingFieldVo.getFieldData()))  {
                return new ResultUtil<AchieveListRecord>().setErrorMsg(offlineRecordingFieldVo.getMetaName()+"??????????????????");
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
        // ??????????????????
        if(achieveList.getIsEncryption() == 1){
            identifier = identifier.toUpperCase();
        }
        BigDecimal times=BigDecimal.ZERO;

        if(achieveList.getIsTimes() == 1 || achieveList.getIsDifferentReward() == 1) {
            String timesStr = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("times")).collect(Collectors.toList()).get(0).getFieldData();
            Boolean isnum = isNumeric(timesStr);
            if (!isnum) {
                return new ResultUtil<AchieveListRecord>().setErrorMsg("?????????????????????????????????0??????????????????????????????????????????");
            }
            String [] array= timesStr.split("\\.");
            if (array.length>1)
            {
                if( array[1].length()>2) {
                    return new ResultUtil<AchieveListRecord>().setErrorMsg("?????????????????????????????????0??????????????????????????????????????????");
                }
            }
            BigDecimal basevalue= BigDecimal.ZERO;
            if(StringUtils.isNotEmpty(timesStr)){
                //times = Integer.valueOf(timesStr);
                times =new BigDecimal(timesStr);
            }
            if (times.compareTo(basevalue)<1)
            {
                return new ResultUtil<AchieveListRecord>().setErrorMsg("?????????????????????????????????0??????????????????????????????????????????");
            }
        }

        if ( achieveList.getIsTimes() == 1 ){
            String timesStr = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("times")).collect(Collectors.toList()).get(0).getFieldData();
            //if(StringUtils.isNumeric(timesStr)){
            if(StringUtils.isNotEmpty(timesStr)){
               // times = Integer.valueOf(timesStr);
                times =new BigDecimal(timesStr);
            }
        }
        if ( achieveList.getIsDifferentReward() == 1 ){
            String timesStr = offlineRecordingFieldVos.stream()
                    .filter(item -> item.getMetaType().equals("times")).collect(Collectors.toList()).get(0).getFieldData();
            //if(StringUtils.isNumeric(timesStr)){
            if(StringUtils.isNotEmpty(timesStr)){
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
                achieveListExtendRecord.setRecord(AESUtil.encrypt(offlineRecordingFieldVo.getFieldData()));
                achieveListExtendRecords.add(achieveListExtendRecord);
            }
            w.setExtendInfo(achieveListExtendRecords);
            identifier = achieveListRecordService.getMd5identifier(achieveList,w);
        }
        w.setIdentifier(identifier);
        Boolean existence = achieveListRecordService.existsByListIdAndIdentifier(w.getListId(),w.getIdentifier());
        if(existence){
            return new ResultUtil<AchieveListRecord>().setErrorMsg("???????????????");
        }
        achieveListRecordService.save(w);
        if(CollectionUtil.isNotEmpty(achieveListExtendRecords)){
            for(AchieveListExtendRecord achieveListExtendRecord:achieveListExtendRecords){
                achieveListExtendRecord.setIdentifier(identifier);
            }
            achieveListExtendRecordService.saveOrUpdateAll(achieveListExtendRecords);
        }
        achieveListRecordService.loadSingleCache(achieveList.getId(),w);
        //??????mq??????????????????????????????act??????
        MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<AchieveListRecord>();
        mqMessageAchieveListRecord.setAppid(UserContext.getAppid());
        mqMessageAchieveListRecord.setTenantId(UserContext.getTenantId());
        mqMessageAchieveListRecord.setContent(w);
        rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);
        return new ResultUtil<AchieveListRecord>().setData(w);
    }

    // ????????????????????????
    public class ExcelListener extends AnalysisEventListener<ExcelRow> {
        private List<ExcelRow> datas = new ArrayList<>();
        @Override
        public void invoke(ExcelRow excelRow, AnalysisContext analysisContext) {
            if (excelRow != null) {
                datas.add(excelRow);
            }
        }
        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        }

        public List<ExcelRow> getDatas() {
            return this.datas;
        }
    }


    @ResponseBody
    @RequestMapping(value = "/importData")
    @ApiOperation(value = "????????????????????????")
    @SystemLog(description = "????????????????????????")
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                      HttpServletRequest request, HttpSession session,
                                      @RequestParam(value = "formId", required = true) String accountFormId,
                                      Integer listType, String achieveListId, String validateFields) throws SQLException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("???????????????????????????????????????????????????");
        }
        /*1.???Excel, ??????????????????
          2.?????????????????????????????????????????????????????????????????????????????????????????????????????????
          2.????????????
         */
        if(file == null){
            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
        }
        String fileName=file.getOriginalFilename();
        if(StringUtils.isBlank(fileName) ){
            return new ResultUtil<Object>().setErrorMsg("?????????xlsx???xls??????");
        }
        String suffix = StringUtils.substring(fileName,StringUtils.lastIndexOf(fileName,"."));
        if(!StringUtils.equalsAnyIgnoreCase(suffix,".xlsx") &&  !StringUtils.equalsAnyIgnoreCase(suffix,".xls") ){
            return new ResultUtil<Object>().setErrorMsg("?????????xlsx???xls??????");
        }
        long start = System.currentTimeMillis();
        System.out.println(start);
        List<String> listString = Arrays.asList(validateFields.split(","));
        // ??????????????????
        String filePath = ReadExcelUtil.FilePath(file, request);
        // excel????????????
        ExcelListener excelListener = new ExcelListener();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
        }
        List<ExcelRow> excelRows = excelListener.getDatas();
        AchieveList achieveList = achieveListService.get(achieveListId);
        if (achieveList == null) {
            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
        }
        List<String> list = new ArrayList();
        if(achieveList.getListType() != 0){
            list.add("???????????????");
        }
        for (String s : listString) {
            if (!StrUtil.isBlank(s)) {
                AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                list.add(formMeta.getTitle());
            }
        }
        if(achieveList.getIsTimes() == 1){
            list.add("????????????");
        }
        // add by ml 2020-12-29
        if(achieveList.getIsDifferentReward() == 1){
            list.add("?????????");
        }
        long redtime = (System.currentTimeMillis()-start)/1000;
        long preTime = 0;
        long writeTime1 = 0;
        long writeTime2 = 0;
        long writeTime3 = 0;
        long writeTime4 = 0;
        // ?????????
        if (listType != 0) {
            if (excelRows != null && excelRows.size() > 0) {
                // ??????excel??????
                ExcelRow excelHead = excelRows.get(0);
                if (excelHead == null) {
                    return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????");
                }
                if (!ExcelConstant.IDENTIFER_EXCEL_COLUMN.equals(excelHead.getColum0())) {
                    return new ResultUtil<Object>().setErrorMsg("???????????????????????????,?????????????????????");
                }
                if (achieveList.getIsTimes() == 1&& !ExcelConstant.LEFT_TIMES_COLUMN.equals(excelHead.getColum1())) {
                    return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
                }
                for(int i=0;i< list.size();i++ ){
                    String[] heand = excelHead.getColums();
                    List<String> heandList = new ArrayList();
                    for(String ser:heand){
                        if(StrUtil.isNotBlank(ser)){
                            heandList.add(ser);
                        }
                    }
                    if(!list.get(i).toString().equals(heand[i])){
                        return new ResultUtil<Object>().setErrorMsg("?????????"+ list.get(i) +"?????????????????????");
                    }
                    if(list.size() != heandList.size()){
                        return new ResultUtil<Object>().setErrorMsg("?????????????????????");
                    }
                }

                excelRows.remove(0);
                // ????????????
                List<AchieveListRecord> achieveListRecords = new ArrayList<>();
                List<String> deleteIdentifers = new ArrayList<>();
                for (ExcelRow temp : excelRows) {
                    AchieveListRecord w = new AchieveListRecord();
                    if(StrUtil.isBlank(temp.getColum0())){
                        continue;
                    }
                    String identifier = temp.getColum0().trim();
                    if (achieveList.getIsEncryption() == 0 && achieveList.getListType() == 2) {
                        identifier = PhoneUtil.dealPhoneNumber(identifier.trim());
                        Boolean isPhone = PhoneUtil.isMobileNO(identifier);
                        if(!isPhone){
                            return new ResultUtil<Object>().setErrorMsg("????????????"+identifier + "???????????????");
                        }
                        identifier = AESUtil.encrypt(identifier);
                    }
                    // ??????????????????
                    if(achieveList.getIsEncryption() == 1){
                        identifier = identifier.toUpperCase();
                    }
                    BigDecimal times=new BigDecimal(1) ;
                    String timesStr = temp.getColum1();
                    if(achieveList.getIsTimes() == 1 || achieveList.getIsDifferentReward() == 1) {

                        if(StringUtils.isBlank(timesStr)) {
                            return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????0??????????????????????????????????????????");
                        }

                        Boolean isnum = isNumeric(timesStr);
                        if (!isnum) {
                            return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????0??????????????????????????????????????????");
                        }
                        String [] array= timesStr.split("\\.");
                        if (array.length>1)
                        {
                           if( array[1].length()>2) {
                               return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????0??????????????????????????????????????????");
                           }
                        }

                    }
                    if(StringUtils.isNotEmpty(timesStr)){
                        //times = Integer.valueOf(timesStr);
                        times =new BigDecimal(timesStr);
                    }
                    BigDecimal basevalue= BigDecimal.ZERO;
                    if (times.compareTo(basevalue)<1)
                    {
                        return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????0??????????????????????????????????????????");
                    }
                    w.setListType(listType);
                    w.setIdentifier(identifier);
                    w.setListId(achieveListId);
                    w.setCreateTime(new Date());
                    w.setUpdateTime(new Date());
                    w.setTimes(times);
                    if(achieveList.getSuperimposed().intValue() == 0){
                        w.setTimes(times);
                    }else {
                        if(achieveList.getSuperimposed().intValue() == 1 && achieveList.getIsTimes().intValue() == 1){
                            AchieveListRecord oldAchieveListRecord = achieveListRecordService.findByListIdAndIdentifier(achieveList.getId(),identifier);
                            if(null != oldAchieveListRecord){
                                w.setTimes(oldAchieveListRecord.getTimes().add(times));
                            }
                        }
                    }
                    w.setCreateBy("");
                    w.setUpdateBy("");
                    deleteIdentifers.add(identifier);
                    achieveListRecords.add(w);
                }
                preTime = (System.currentTimeMillis()-start)/1000 - redtime;
                if(deleteIdentifers.size()>0){
                    iAchieveListRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,3000,achieveListId);
                }
                writeTime1 = (System.currentTimeMillis()-start)/1000 - preTime;
                //iAchieveListRecordService.saveBatchWithIgnore(achieveListRecords, 10000);
                insertBatch(achieveListRecords);

                writeTime2 = (System.currentTimeMillis()-start)/1000 - writeTime1;
            }
        } else {
            if (excelRows != null && excelRows.size() > 0) {
                // ????????????
                ExcelRow tempHead = excelRows.get(0);
                if (tempHead == null) {
                    return new ResultUtil<Object>().setErrorMsg("?????????????????????");
                }
                //??????????????????
                List<AchieveListExtendRecord> extendList = new ArrayList<>();
                List<AccountFormMeta> formMetas = new ArrayList<>();
                for (String s : listString) {
                    AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                    formMetas.add(formMeta);
                }
                AccountFormMeta timesformMeta = new AccountFormMeta();
                timesformMeta.setTitle("????????????");
                formMetas.add(timesformMeta);
                String[] heads = tempHead.getColums();
                for (int i = 0; i < heads.length; i++) {
                    if (StringUtils.isNotBlank(heads[i]) && !StringUtils.equals(heads[i], formMetas.get(i).getTitle())) {
                        return new ResultUtil<Object>().setErrorMsg("?????????????????????");
                    }
                }
                excelRows.remove(0);
                // ????????????list
                List<String> deleteIdentifers = new ArrayList<>();
                List<AchieveListRecord> all = multipleHandle(accountFormId, achieveList, listType, excelRows, formMetas,extendList,deleteIdentifers);
                preTime = (System.currentTimeMillis()-start)/1000 - redtime;
//                for (AchieveListRecord w : all) {
//                    w.setListId(achieveListId);
//                    w.setCreateTime(new Date());
//                    w.setUpdateTime(new Date());
//                    for (AchieveListExtendRecord newExtend : w.getExtendInfo()) {
//                        extendList.add(newExtend);
//                    }
//                    deleteIdentifers.add(w.getIdentifier());
//                }
                writeTime1 = (System.currentTimeMillis()-start)/1000 - preTime;
                if(deleteIdentifers.size()>0){
                    iAchieveListRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,3000,achieveListId);
                }
                writeTime2 = (System.currentTimeMillis()-start)/1000 - writeTime1;
                boolean b1 = iAchieveListRecordService.saveBatchWithIgnore(all, 3000);
//                List<AchieveListRecord> records = achieveListRecordService.findByListIdAndIsDeleted(achieveListId, false);
//                List<String> recordIds = new ArrayList<>();
//                for (AchieveListRecord a : records) {
//                    recordIds.add(a.getId());
//                }
//                for (AchieveListExtendRecord newExtend : extendList) {
//                        newExtend.setCreateTime(new Date());
//                        newExtend.setUpdateTime(new Date());
//                }

                writeTime3 = (System.currentTimeMillis()-start)/1000 - writeTime2;
                if(deleteIdentifers.size()>0){
                    iAchieveListExtendRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,achieveListId,3000);
                }
                writeTime4 = (System.currentTimeMillis()-start)/1000 - writeTime3;
                iAchieveListExtendRecordService.saveBatchWithIgnore(extendList, 10000);

            }
        }
        achieveListRecordService.loadCache(achieveListId);
        System.out.println("?????????"+(System.currentTimeMillis()-start)/1000);
        //??????mq??????????????????????????????act??????
        MQMessage<AchieveList> mqMessageAchieveList = new MQMessage<AchieveList>();
        mqMessageAchieveList.setAppid(UserContext.getAppid());
        mqMessageAchieveList.setTenantId(UserContext.getTenantId());
        mqMessageAchieveList.setContent(achieveList);
        rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_PUSHACT_MSG), mqMessageAchieveList);

        return new ResultUtil<Object>().setSuccessMsg("????????????");
//        return new ResultUtil<Object>().setSuccessMsg("????????????, \\n"
//                +"???????????????"+redtime
//                +" \\n????????????:"+preTime
//                +" \\n????????????1:"+writeTime1
//                +" \\n????????????2:"+writeTime2
//                +" \\n????????????3:"+writeTime3
//                +" \\n????????????4:"+writeTime4
//                +" \\n?????????"+(System.currentTimeMillis()-start)/1000);
    }


    // ?????????????????????
    private void insertBatch(List<AchieveListRecord> listpua) {
        // ??????insert??????2???????????????
        int every = 20000;
        if (listpua.size() > 0) {
            if (listpua.size() <= every) {
                iAchieveListRecordService.batchInsert(listpua);
            } else {
                int totalNum = listpua.size();
                for (int i = 0; i <= totalNum / every; i++) {
                    List<AchieveListRecord> ins = new ArrayList<AchieveListRecord>();
                    if (i == totalNum / every) {
                        ins = listpua.subList(i * every, totalNum);
                    } else {
                        ins = listpua.subList(i * every, (i + 1) * every);
                    }
                    if (ins.size() > 0) {
                        iAchieveListRecordService.batchInsert(ins);
                    }
                }
            }
        }
    }


    public  boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        if(str.indexOf(".")>0){//????????????????????????
            if(str.indexOf(".")==str.lastIndexOf(".") && str.split("\\.").length==2){ //?????????????????????????????????
                return pattern.matcher(str.replace(".","")).matches();
            }else {
                return false;
            }
        }else {
            return pattern.matcher(str).matches();
        }
    }
   /* public  boolean isNumeric(String str){
           for (int i = str.length();--i>=0;){
                   if (!Character.isDigit(str.charAt(i))){
                       return false;
                   }
               }
               return true;
         }*/



    /**
     * excel????????????
     *
     * @param listString
     * @return
     */
    private List<String> getHeaders(List<String> listString) {
        List<String> head = new ArrayList<>();
        for (String s : listString) {
            AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
            head.add(formMeta.getTitle());
        }
        return head;
    }

    /**
     * ????????????
     *
     * @param all
     * @return
     */
    private List<AchieveListRecord> deduplication(List<AchieveListRecord> all) {
        return all.parallelStream().distinct()
                .filter(distinctByKey(b -> b.getIdentifier()))
                .collect(toList());
    }

    /**
     * ????????????????????????????????????
     *
     * @param accountFormId
     * @param achieveListId
     * @param listType
     * @param objects
     * @return
     */
    private List<AchieveListRecord> singleHandle(String accountFormId, String achieveListId, Integer listType, List<Object> objects) {
        List<AchieveListRecord> all = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            if (i != 0) {
                String str = objects.get(i).toString();
                String demosub = str.substring(1, str.length() - 1);
                String[] demoArray = demosub.split(",");
                AchieveListRecord record = new AchieveListRecord();
                record.setIdentifier(demoArray[0].trim());
                record.setListType(listType);
                record.setListId(achieveListId);
                all.add(record);
            }
        }
        return all;
    }

    /**
     * ??????????????????????????????
     *
     * @param accountFormId
     * @param achieveList
     * @param listType
     * @param objects
     * @param formMetaMap
     * @return
     */
    private List<AchieveListRecord> multipleHandle(String accountFormId, AchieveList achieveList, Integer listType,
                                                   List<ExcelRow> objects, List<AccountFormMeta> formMetaMap,
                                                   List<AchieveListExtendRecord> extendList, List<String> deleteIdentifers) {
        List<AchieveListRecord> all = new ArrayList();
        //???????????????List<AchieveListRecord>
        for (ExcelRow temp : objects) {
            BigDecimal times=BigDecimal.ZERO;
            AchieveListRecord record = new AchieveListRecord();
            record.setListType(listType);
            record.setListId(achieveList.getId());
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            List<AchieveListExtendRecord> newExtendRecords = new ArrayList<>();
            String[] colums = temp.getColums();
            Date date = new Date();
            for (int i = 0; i < colums.length; i++) {
                if (i <= formMetaMap.size() && StringUtils.isNotBlank(colums[i]) && i <formMetaMap.size() - 1 ) {
                    AccountFormMeta accountFormMeta = formMetaMap.get(i);
                    AchieveListExtendRecord extendRecord = new AchieveListExtendRecord();
                    String filedrecord = colums[i].trim();
                    //???????????????????????????????????????????????????
                    if(accountFormMeta.getMetaType().equals("phone")){
                        filedrecord = PhoneUtil.dealPhoneNumber(colums[i].trim());
                        Boolean isPhone = PhoneUtil.isMobileNO(filedrecord);
                        if(!isPhone){
                            throw new InndooException("????????????"+colums[i] + "???????????????");
                        }
                    }
                    extendRecord.setListId(achieveList.getId());
                    extendRecord.setFormMetaId(accountFormMeta.getId());
                    extendRecord.setRecordId(record.getId());
                    if(StringUtils.isNotBlank(filedrecord)){
                        extendRecord.setRecord(AESUtil.encrypt(filedrecord));
                    }else{
                        extendRecord.setRecord("");
                    }
                    extendRecord.setMetaCode(accountFormMeta.getMetaType());
                    extendRecord.setMetaTitle(accountFormMeta.getTitle());
                    extendRecord.setCreateTime(date);
                    newExtendRecords.add(extendRecord);
                    extendList.add(extendRecord);
                }
                if(i == formMetaMap.size() - 1){

                    String timesStr = colums[i];
                    //if(StringUtils.isNumeric(timesStr)){
                    if(StringUtils.isNotEmpty(timesStr)){
                        //times = Integer.valueOf(timesStr);
                        times = new BigDecimal (timesStr);

                    }
                    record.setTimes(times);
                }
            }

            record.setExtendInfo(newExtendRecords);
            String identifier = achieveListRecordService.getMd5identifier(achieveList,record);
            for(AchieveListExtendRecord extendRecord :newExtendRecords){
                extendRecord.setIdentifier(identifier);
            }
            record.setIdentifier(identifier);
            record.setExtendInfo(newExtendRecords);
            record.setCreateTime(new Date());
            if(achieveList.getSuperimposed().intValue() == 1 && achieveList.getIsTimes().intValue() == 1){
                AchieveListRecord oldAchieveListRecord = achieveListRecordService.findByListIdAndIdentifier(achieveList.getId(),record.getIdentifier());
                if(null != oldAchieveListRecord){
                    record.setTimes(oldAchieveListRecord.getTimes().add(times));
                }
            }
            deleteIdentifers.add(identifier);
            all.add(record);
        }
        return all;
    }

    /**
     * ??????String??????????????????long????????????????????????
     *
     * @param inVal ???????????????
     * @return long?????????
     */
    public static long fromDateStringToLong(String inVal) {
        Date date = null;
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
        try {
            date = inputFormat.parse(inVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @RequestMapping(value = "/exportTemplate/{id}")
    @ApiOperation(value = "????????????????????????????????????")
    public void exportTemplate(@PathVariable String id, HttpServletResponse response)  throws IOException {
        AchieveList achieveList = achieveListService.get(id);
        List<String> listString = Arrays.asList(achieveList.getValidateFields().split(","));
        List<List<String>> rows = CollUtil.newArrayList();
        List<String> list = new ArrayList();
        if(achieveList.getListType() != 0){
            list.add("???????????????");
        }
        for (String s : listString) {
            if (!StrUtil.isBlank(s)) {
                AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                list.add(formMeta.getTitle());
            }
        }
        if(achieveList.getIsTimes() == 1){
            list.add("????????????");
        }
        // add by ml 2020-12-29
        if(achieveList.getIsDifferentReward() == 1){
            list.add("?????????");
        }

        List<String> row1 = CollUtil.newArrayList(list);
        rows.add(row1);
//        ApplicationHome home = new ApplicationHome(getClass());
//        File jarFile = home.getSource();
//        String path = jarFile.getParentFile().getPath();
//        String rootPath = path + File.separator + "static/ytdexports";
//        File dir = new File(rootPath);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
        String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFileName  = "????????????????????????_"+achieveList.getName()+downloadDate+".xlsx";
//        String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
        OutputStream out = null;
        ExcelWriter writer = null;
        try {
            out = response.getOutputStream();
            response.setContentType("multipart/form-data");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + URLUtil.encode(excelFileName, StringUtil.UTF8));
            writer = new ExcelWriter(out, ExcelTypeEnum.XLSX, true);
            Sheet sheet =new Sheet(1,0);
            sheet.setAutoWidth(Boolean.TRUE);
            writer.write(rows,sheet);
            rows.clear();;
        } catch (IOException e) {
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("??????????????????")));
        }finally {
            out.flush();
            writer.finish();
            out.close();
        }
//        String fullFileName = rootPath + File.separator + excelFileName;
//        BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
//        // ??????????????????????????????????????????
//        writer.write(rows);
//        // ??????writer???????????????
//        writer.close();
//        File file = new File(fullFileName);
//        ServletUtil.write(response, file);
//        file.delete();
//        return new ResultUtil<Object>().setSuccessMsg("OK");
    }


    @RequestMapping(value = "/export/{id}")
    @ApiOperation(value = "??????????????????????????????")
    @SystemLog(description = "??????????????????????????????")
    public void export(@ModelAttribute AchieveListRecord achieveListRecord,
                       @ModelAttribute SearchVo searchVo,
                       @ModelAttribute PageVo pageVo,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       @PathVariable String id) throws IOException {
        AchieveList achieveList =achieveListService.get(id);
        List<List<String>> rows = achieveListRecordService.toWrite(id);

        String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFileName  = "????????????????????????_"+achieveList.getName()+downloadDate+".xlsx";
        waterExcelUtil.writeForList(response,excelFileName,rows);

//        ExcelWriter writer = null;
//        OutputStream out = null;
//        try {
////            String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
//            String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//            String excelFileName  = "????????????????????????_"+achieveList.getName()+downloadDate+".xlsx";
//            out = response.getOutputStream();
//            response.setContentType("multipart/form-data");
//            response.setCharacterEncoding("utf-8");
//            response.setHeader("Content-disposition", "attachment;filename="+ URLUtil.encode(excelFileName, StringUtil.UTF8) );
//            writer = new ExcelWriter(out, ExcelTypeEnum.XLSX, true);
//            Sheet sheet =new Sheet(1,0);
//            sheet.setAutoWidth(Boolean.TRUE);
//            writer.write(rows,sheet);
//            rows.clear();;
//        } catch (Exception e) {
//            // ??????response
//            response.reset();
//            response.setContentType("application/json");
//            response.setCharacterEncoding("utf-8");
//            response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("??????????????????")));
//        }finally {
//            out.flush();
//            writer.finish();
//            out.close();
//        }
    }

    @RequestMapping(value = "/deleteByListId/{listId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "??????listId????????????")
    @SystemLog(description = "??????listId????????????")
    public Result<Object> deleteById(@PathVariable String listId){

        getService().deleteByListId(listId);
        return new ResultUtil<Object>().setSuccessMsg("??????????????????");
    }

    @Override
    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "??????id????????????")
    @Transactional(rollbackFor = Exception.class)
    @SystemLog(description = "??????id????????????")
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids) {
        String achieveId = "";
        List<String> md5 = new ArrayList<>();
        for (String id : ids) {
            AchieveListRecord record = achieveListRecordService.get(id);
            achieveId = record.getListId();
            md5.add(record.getIdentifier());
            String recordId = record.getId();
            String listId = record.getListId();
            achieveListRecordService.delete(record);
            List<AchieveListExtendRecord> extrecords = achieveListExtendRecordService.findByListIdAndRecordId(listId, recordId);
            if (extrecords.size() > 0) {
                String[] deleteIds = new String[extrecords.size()];
                for(int i = 0 ;i<extrecords.size() ;i++){
                    deleteIds[i] = extrecords.get(i).getId();
                }
                achieveListExtendRecordService.delete(deleteIds);
            }
        }
        if(StringUtils.isNotBlank(achieveId)&&md5.size()>0 ){
            achieveListRecordService.removeCache(achieveId,md5);
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????????????????");
    }

    @RequestMapping(value = "/queryCountByCondition/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "????????????????????????????????????")
    @SystemLog(description = "????????????????????????????????????")
    public Result<Long> queryCountByCondition(@ModelAttribute AchieveListRecord achieveListRecord,
                                              @ModelAttribute SearchVo searchVo,
                                              @ModelAttribute PageVo pageVo,
                                              @PathVariable String id) {
        long count = achieveListRecordService.count(new Specification<AchieveListRecord>() {
                                                        @Override
                                                        public javax.persistence.criteria.Predicate toPredicate(Root<AchieveListRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                                                            Path<String> listIdField = root.get("listId");
                                                            List<javax.persistence.criteria.Predicate> list = new ArrayList<javax.persistence.criteria.Predicate>();

                                                            //????????????id
                                                            if (StrUtil.isNotBlank(id)) {
                                                                list.add(cb.equal(listIdField, id.trim()));
                                                            }

                                                            javax.persistence.criteria.Predicate[] arr = new javax.persistence.criteria.Predicate[list.size()];
                                                            if (list.size() > 0) {
                                                                cq.where(list.toArray(arr));
                                                            }
                                                            return null;
                                                        }
                                                    }
        );
        return new ResultUtil<Long>().setData(count);
    }
}
