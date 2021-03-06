package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
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
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.utils.excel.WaterExcelUtil;
import com.ytdinfo.inndoo.common.utils.excel.WaterMarkHandler;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.AccountFormMetaService;
import com.ytdinfo.inndoo.modules.core.service.WhiteListExtendRecordService;
import com.ytdinfo.inndoo.modules.core.service.WhiteListRecordService;
import com.ytdinfo.inndoo.modules.core.service.WhiteListService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListExtendRecordService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "???????????????????????????")
@RequestMapping("/whitelistrecord")
public class WhiteListRecordController extends BaseController<WhiteListRecord, String> {

    @Autowired
    private WhiteListRecordService whiteListRecordService;

    @Autowired
    private IWhiteListRecordService iWhiteListRecordService;

    @Autowired
    private IWhiteListExtendRecordService iWhiteListExtendRecordService;

    @Autowired
    private WhiteListService whiteListService;

    @Autowired
    private WhiteListExtendRecordService whiteListExtendRecordService;

    @Autowired
    private AccountFormMetaService accountFormMetaService;

    @Autowired
    private WaterExcelUtil waterExcelUtil;

    @Override
    public WhiteListRecordService getService() {
        return whiteListRecordService;
    }

    @RequestMapping(value = "/listByCondition/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????")
    @SystemLog(description = "?????????????????????")
    public Result<Page<WhiteListRecord>> listByCondition(@ModelAttribute WhiteListRecord whiteListRecord,
                                                         @ModelAttribute SearchVo searchVo,
                                                         @ModelAttribute PageVo pageVo,
                                                         @PathVariable String id) {
        String appid = UserContext.getAppid();
        whiteListRecord.setAppid(appid);
        Page<WhiteListRecord> page = whiteListRecordService.
                findByCondition(whiteListRecord, searchVo, PageUtil.initPage(pageVo), id);
        List<String> identifiers =new ArrayList<>();
        if (page.getTotalElements() > 0) {
            WhiteList whiteList = whiteListService.get(id);
            for (int i = 0; i < page.getContent().size(); i++) {
                WhiteListRecord whiteListRecord1= page.getContent().get(i);
                if (whiteList.getListType() == 2) {
                    if(whiteList.getIsEncryption() == 0){
                        page.getContent().get(i).setIdentifier(AESUtil.decrypt(whiteListRecord1.getIdentifier()));
                    }else {
                        page.getContent().get(i).setIdentifier(whiteListRecord1.getIdentifier());
                    }
                }
                if(whiteList.getListType() == 0){
                    identifiers.add(whiteListRecord1.getIdentifier());
                }
                page.getContent().get(i).setListType(whiteList.getListType());
            }
            if(whiteList.getListType() == 0 && identifiers.size()>0){
                List<WhiteListExtendRecord>  records = whiteListExtendRecordService.findByListIdAndIdentifierIn(id, identifiers);
                List<WhiteListExtendRecord> whiteListExtendRecords = new ArrayList<>();
                whiteListExtendRecords.addAll(records);
                Map<String,List<WhiteListExtendRecord>> map = new HashMap<>();
                for( WhiteListExtendRecord whiteListExtendRecord: whiteListExtendRecords){
                    List<WhiteListExtendRecord>  tempList=  map.get(whiteListExtendRecord.getIdentifier());
                    if(tempList == null){
                        tempList = new ArrayList<>();
                    }
                    whiteListExtendRecord.setRecord(AESUtil.decrypt(whiteListExtendRecord.getRecord()));
                    tempList.add(whiteListExtendRecord);
                    map.put(whiteListExtendRecord.getIdentifier(),tempList);
                }

                for (int i = 0; i < page.getContent().size(); i++) {
                    String tempIdentifier =  page.getContent().get(i).getIdentifier();
                    List<WhiteListExtendRecord>  tempList=  map.get(tempIdentifier);
                    if( tempList !=null){
                        page.getContent().get(i).setExtendInfo(tempList);
                    }
                }
            }
        }
        return new ResultUtil<Page<WhiteListRecord>>().setData(page);
    }

    /**
     * ??????OfflineRecordingInputVo
     * @return
     */
    @RequestMapping(value = "/getOfflineRecordingInputVo", method = RequestMethod.GET)
    @ApiOperation(value = "????????????????????????")
    public Result<OfflineRecordingInputVo> getOfflineRecordingInputVo(@RequestParam String listId ){
        WhiteList whiteList = whiteListService.get(listId);
        if(null == whiteList){
            return new ResultUtil<OfflineRecordingInputVo>().setErrorMsg("??????????????????");
        }
        OfflineRecordingInputVo offlineRecordingInputVo = new OfflineRecordingInputVo();
        offlineRecordingInputVo.setListId(listId);
        offlineRecordingInputVo.setListType(whiteList.getListType().toString());
        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = new ArrayList<>();
        if(whiteList.getListType() == 1 || whiteList.getListType() == 5){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(whiteList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("openid");
            offlineRecordingFieldVo.setMetaName("openid");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if(whiteList.getListType() == 2){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(whiteList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("phone");
            offlineRecordingFieldVo.setMetaName("?????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if (whiteList.getListType() == 3 ){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(whiteList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("accountid");
            offlineRecordingFieldVo.setMetaName("???????????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        }  if (whiteList.getListType() == 4 ){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(whiteList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("actAccountid");
            offlineRecordingFieldVo.setMetaName("??????????????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if (whiteList.getListType() == 0 ) {
            String validateFields = whiteList.getValidateFields();
            String[] validateFieldes = validateFields.split(",");
            for (String validateField : validateFieldes){
                if(StrUtil.isNotBlank(validateField)){
                    AccountFormMeta accountFormMeta = accountFormMetaService.get(validateField);
                    if(null != accountFormMeta) {
                        OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
                        offlineRecordingFieldVo.setMetaId(validateField);
                        offlineRecordingFieldVo.setListId(listId);
                        offlineRecordingFieldVo.setListType(whiteList.getListType().toString());
                        offlineRecordingFieldVo.setFieldData("");
                        offlineRecordingFieldVo.setMetaType(accountFormMeta.getMetaType());
                        offlineRecordingFieldVo.setMetaName(accountFormMeta.getTitle());
                        offlineRecordingFieldVos.add(offlineRecordingFieldVo);
                    }
                }
            }
        }
        if(whiteList.getIsTimes().intValue() == 1){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(whiteList.getListType().toString());
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setMetaType("times");
            offlineRecordingFieldVo.setMetaName("????????????");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        }
        offlineRecordingInputVo.setOfflineRecordingFieldVos(offlineRecordingFieldVos);
        return new ResultUtil<OfflineRecordingInputVo>().setData(offlineRecordingInputVo);
    }

    @RequestMapping(value = "/saveListRecord", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "???????????????")
    public Result<WhiteListRecord> saveWhiteListRecord(@RequestBody OfflineRecordingInputVo offlineRecordingInputVo){
        WhiteList whiteList = whiteListService.get(offlineRecordingInputVo.getListId());
        if(null == whiteList){
            return new ResultUtil<WhiteListRecord>().setErrorMsg("???????????????");
        }
        WhiteListRecord w = new WhiteListRecord();

        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = offlineRecordingInputVo.getOfflineRecordingFieldVos();
        for (OfflineRecordingFieldVo offlineRecordingFieldVo: offlineRecordingFieldVos){
            if (StrUtil.isBlank(offlineRecordingFieldVo.getFieldData()))  {
                return new ResultUtil<WhiteListRecord>().setErrorMsg(offlineRecordingFieldVo.getMetaName()+"??????????????????");
            }
        }
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
        // ??????????????????
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
                whiteListExtendRecord.setRecord(AESUtil.encrypt(offlineRecordingFieldVo.getFieldData()));
                whiteListExtendRecords.add(whiteListExtendRecord);
            }
            w.setExtendInfo(whiteListExtendRecords);
            identifier = whiteListRecordService.getMd5identifier(whiteList,w);

        }
        w.setIdentifier(identifier);
        Boolean existence = whiteListRecordService.existsByListIdAndIdentifier(w.getListId(),w.getIdentifier());
        if(existence){
            return new ResultUtil<WhiteListRecord>().setErrorMsg("???????????????");
        }
        whiteListRecordService.save(w);
        for(WhiteListExtendRecord whiteListExtendRecord: whiteListExtendRecords){
            whiteListExtendRecord.setIdentifier(identifier);
        }
        whiteListExtendRecordService.saveOrUpdateAll(whiteListExtendRecords);
        whiteListRecordService.loadSingleCache(whiteList.getId(),w);
        return new ResultUtil<WhiteListRecord>().setData(w);
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

    @Override
    @RequestMapping(value = "/deleteByListId/{listId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "??????listId????????????")
    @SystemLog(description = "??????listId????????????")
    public Result<Object> deleteById(@PathVariable String listId){

        getService().deleteByListId(listId);
        return new ResultUtil<Object>().setSuccessMsg("??????????????????");
    }

    @ResponseBody
    @RequestMapping(value = "/importData")
    @ApiOperation(value = "?????????????????????")
    @SystemLog(description = "?????????????????????")
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                      HttpServletRequest request, HttpSession session,
                                      @RequestParam(value = "formId", required = true) String accountFormId,
                                      Integer listType, String whiteListId, String validateFields) throws SQLException {
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
        WhiteList whiteList = whiteListService.get(whiteListId);
        if (whiteList == null) {
            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
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
                    return new ResultUtil<Object>().setErrorMsg("????????????,?????????????????????");
                }
                if (!ExcelConstant.IDENTIFER_EXCEL_COLUMN.equals(excelHead.getColum0())) {
                    return new ResultUtil<Object>().setErrorMsg("???????????????????????????,?????????????????????");
                }
                if (whiteList.getIsTimes() == 1&& !ExcelConstant.LEFT_TIMES_COLUMN.equals(excelHead.getColum1())) {
                    return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
                }
                excelRows.remove(0);
                // ????????????
                List<WhiteListRecord> whiteListRecords = new ArrayList<>();
                List<String> deleteIdentifers = new ArrayList<>();
                for (ExcelRow temp : excelRows) {
                    WhiteListRecord w = new WhiteListRecord();
                    if(StrUtil.isBlank(temp.getColum0())){
                        continue;
                    }
                    String identifier = temp.getColum0().trim();
                    if (whiteList.getIsEncryption() == 0 && whiteList.getListType() == 2) {
                        identifier = PhoneUtil.dealPhoneNumber(identifier.trim());
                        Boolean isPhone = PhoneUtil.isMobileNO(identifier);
                        if(!isPhone){
                            return new ResultUtil<Object>().setErrorMsg("????????????"+identifier + "???????????????");
                        }
                        identifier = AESUtil.encrypt(identifier);
                    }
                    // ??????????????????
                    if(whiteList.getIsEncryption() == 1){
                        identifier = identifier.toUpperCase();
                    }
                    Integer times=1;
                    String timesStr = temp.getColum1();
                    if(StringUtils.isNumeric(timesStr)){
                        times = Integer.valueOf(timesStr);
                    }
                    w.setListType(listType);
                    w.setIdentifier(identifier);
                    w.setListId(whiteListId);
                    w.setCreateTime(new Date());
                    w.setUpdateTime(new Date());
                    w.setTimes(times);
                    if(whiteList.getSuperimposed().intValue() == 0){
                        w.setTimes(times);
                    }else {
                        if(whiteList.getSuperimposed().intValue() == 1 && whiteList.getIsTimes().intValue() == 1){
                            WhiteListRecord oldWhiteListRecord = whiteListRecordService.findByListIdAndIdentifier(whiteListId,identifier);
                            if(null != oldWhiteListRecord){
                                w.setTimes(oldWhiteListRecord.getTimes() + times);
                            }
                        }
                    }
                    deleteIdentifers.add(identifier);
                    whiteListRecords.add(w);
                }
                preTime = (System.currentTimeMillis()-start)/1000 - redtime;
                if(deleteIdentifers.size()>0){
                    iWhiteListRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,3000,whiteListId);
                }
                writeTime1 = (System.currentTimeMillis()-start)/1000 - preTime;
                iWhiteListRecordService.saveBatchWithIgnore(whiteListRecords, 10000);
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
                List<WhiteListExtendRecord> extendList = new ArrayList<>();
                List<AccountFormMeta> formMetas = new ArrayList<>();
                for (String s : listString) {
                    AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                    formMetas.add(formMeta);
                }
                AccountFormMeta timesformMeta = new AccountFormMeta();
                timesformMeta.setTitle("????????????");
                formMetas.add(timesformMeta);
                String[] heads = tempHead.getColums();
                for(int i =0 ;i<formMetas.size();i++){
                    if (StringUtils.isNotBlank(heads[i]) && !StringUtils.equals(heads[i], formMetas.get(i).getTitle())) {
                        return new ResultUtil<Object>().setErrorMsg("?????????????????????");
                    }
                }

                excelRows.remove(0);
                // ????????????list
                List<String> deleteIdentifers = new ArrayList<>();
                List<WhiteListRecord> all = multipleHandle(accountFormId, whiteList, listType, excelRows, formMetas,extendList,deleteIdentifers);
                preTime = (System.currentTimeMillis()-start)/1000 - redtime;
                writeTime1 = (System.currentTimeMillis()-start)/1000 - preTime;
                if(deleteIdentifers.size()>0){
                    iWhiteListRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,3000,whiteListId);
                }
                writeTime2 = (System.currentTimeMillis()-start)/1000 - writeTime1;
                boolean b1 = iWhiteListRecordService.saveBatchWithIgnore(all, 10000);
                writeTime3 = (System.currentTimeMillis()-start)/1000 - writeTime2;
                if(deleteIdentifers.size()>0){
                    iWhiteListExtendRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,whiteListId,3000);
                }
                writeTime4 = (System.currentTimeMillis()-start)/1000 - writeTime3;
                iWhiteListExtendRecordService.saveBatchWithIgnore(extendList, 10000);

            }
        }
        whiteListRecordService.loadCache(whiteListId);
        System.out.println("?????????"+(System.currentTimeMillis()-start)/1000);
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
    private List<WhiteListRecord> deduplication(List<WhiteListRecord> all) {
        return all.parallelStream().distinct()
                .filter(distinctByKey(b -> b.getIdentifier()))
                .collect(toList());
    }

    /**
     * ????????????????????????????????????
     *
     * @param accountFormId
     * @param whiteListId
     * @param listType
     * @param objects
     * @return
     */
    private List<WhiteListRecord> singleHandle(String accountFormId, String whiteListId, Integer listType, List<Object> objects) {
        List<WhiteListRecord> all = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            if (i != 0) {
                String str = objects.get(i).toString();
                String demosub = str.substring(1, str.length() - 1);
                String[] demoArray = demosub.split(",");
                WhiteListRecord record = new WhiteListRecord();
                record.setIdentifier(demoArray[0].trim());
                record.setListType(listType);
                record.setListId(whiteListId);
                all.add(record);
            }
        }
        return all;
    }

    /**
     * ??????????????????????????????
     *
     * @param accountFormId
     * @param whiteList
     * @param listType
     * @param objects
     * @param formMetaMap
     * @return
     */
    private List<WhiteListRecord> multipleHandle(String accountFormId, WhiteList whiteList, Integer listType,
                                                 List<ExcelRow> objects, List<AccountFormMeta> formMetaMap,
                                                 List<WhiteListExtendRecord> extendList, List<String> deleteIdentifers) {
        List<WhiteListRecord> all = new ArrayList();
        //???????????????List<WhiteListRecord>
        for (ExcelRow temp : objects) {
            WhiteListRecord record = new WhiteListRecord();
            record.setListType(listType);
            record.setListId(whiteList.getId());
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            Integer times=0;
            List<WhiteListExtendRecord> newExtendRecords = new ArrayList<>();
            String[] colums = temp.getColums();
            Date date = new Date();
            for (int i = 0; i < colums.length; i++) {
                if (i <= formMetaMap.size() && StringUtils.isNotBlank(colums[i]) && i <formMetaMap.size() - 1 ) {
                    AccountFormMeta accountFormMeta = formMetaMap.get(i);
                    WhiteListExtendRecord extendRecord = new WhiteListExtendRecord();
                    String filedrecord = colums[i].trim();
                    //???????????????????????????????????????????????????
                    if(accountFormMeta.getMetaType().equals("phone")){
                        filedrecord = PhoneUtil.dealPhoneNumber(colums[i].trim());
                        Boolean isPhone = PhoneUtil.isMobileNO(filedrecord);
                        if(!isPhone){
                            throw new InndooException("????????????"+colums[i] + "???????????????");
                        }
                    }
                    extendRecord.setListId(whiteList.getId());
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
                    if(StringUtils.isNumeric(timesStr)){
                        times = Integer.valueOf(timesStr);
                    }
                    record.setTimes(times);
                }
            }

            record.setExtendInfo(newExtendRecords);
            String identifier = whiteListRecordService.getMd5identifier(whiteList,record);
            for(WhiteListExtendRecord extendRecord :newExtendRecords){
                extendRecord.setIdentifier(identifier);
            }
            record.setIdentifier(identifier);
            record.setExtendInfo(newExtendRecords);
            record.setCreateTime(new Date());
            deleteIdentifers.add(identifier);
            if(whiteList.getSuperimposed().intValue() == 1 && whiteList.getIsTimes().intValue() == 1){
                WhiteListRecord oldWhiteListRecord = whiteListRecordService.findByListIdAndIdentifier(whiteList.getId(),record.getIdentifier());
                if(null != oldWhiteListRecord){
                    record.setTimes(oldWhiteListRecord.getTimes() + times);
                }
            }
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
    @ApiOperation(value = "?????????????????????????????????")
    @SystemLog(description = "?????????????????????????????????")
    public void exportTemplate(@PathVariable String id, HttpServletResponse response)  throws IOException {
        WhiteList whiteList = whiteListService.get(id);
        List<String> listString = Arrays.asList(whiteList.getValidateFields().split(","));
        List<List<String>> rows = CollUtil.newArrayList();
        List<String> list = new ArrayList();
        if (whiteList.getListType() != 0) {
            list.add("???????????????");
        }
        for (String s : listString) {
            if (!StrUtil.isBlank(s)) {
                AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                list.add(formMeta.getTitle());
            }
        }
        if(whiteList.getIsTimes() == 1){
            list.add("????????????");
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
//        String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
        String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFileName  = "?????????????????????_"+whiteList.getName()+downloadDate+".xlsx";
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
    @ApiOperation(value = "???????????????????????????")
    @SystemLog(description = "???????????????????????????")
    public void export(@ModelAttribute WhiteListRecord whiteListRecord,
                       @ModelAttribute SearchVo searchVo,
                       @ModelAttribute PageVo pageVo,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       @PathVariable String id) throws IOException {

        WhiteList whiteList = whiteListService.get(id);
        List<List<String>> rows = whiteListRecordService.toWrite(id);
        String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFileName  = "?????????????????????_"+whiteList.getName()+downloadDate+".xlsx";
        waterExcelUtil.writeForList(response,excelFileName,rows);
//        ExcelWriter writer = null;
//        OutputStream out = null;
//        try {
////            response.setContentType("application/vnd.ms-excel");
////            response.setCharacterEncoding("utf-8");
////            String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
//            String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//            String excelFileName  = "?????????????????????_"+whiteList.getName()+downloadDate+".xlsx";
//            response.setHeader("Content-disposition", "attachment;filename=" + excelFileName );
//            // ??????????????????????????????
//            EasyExcel.write(response.getOutputStream()).autoCloseStream(Boolean.FALSE).sheet("??????")
//                    .doWrite(rows);
//            out = response.getOutputStream();
//            response.setContentType("multipart/form-data");
//            response.setCharacterEncoding("utf-8");
//            response.setHeader("Content-disposition", "attachment;filename="+ URLUtil.encode(excelFileName, StringUtil.UTF8) );
//            writer = new ExcelWriter(out, ExcelTypeEnum.XLSX, true);
//            Sheet sheet =new Sheet(1,0);
//            sheet.setAutoWidth(Boolean.TRUE);
//            writer.write(rows,sheet);
//            rows.clear();
//
//        } catch (Exception e) {
//            // ??????response
//            response.reset();
//            response.setContentType("application/json");
//            response.setCharacterEncoding("utf-8");
//            response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("??????????????????")));
//        } finally {
//            out.flush();
//            writer.finish();
//            out.close();
//        }
    }

    @Override
    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "??????id????????????")
    @Transactional(rollbackFor = Exception.class)
    @SystemLog(description = "??????id????????????")
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids) {
        String whilteid = "";

        List<String> md5 = new ArrayList<>();
        for (String id : ids) {
            WhiteListRecord record = whiteListRecordService.get(id);
            whilteid = record.getListId();
            md5.add(record.getIdentifier());
            String recordId = record.getId();
            String listId = record.getListId();
            whiteListRecordService.delete(record);
            List<WhiteListExtendRecord> extrecords = whiteListExtendRecordService.findByListIdAndRecordId(listId, recordId);
            if (extrecords.size() > 0) {
                String[] deleteIds = new String[extrecords.size()];
                for(int i = 0 ;i<extrecords.size() ;i++){
                    deleteIds[i] = extrecords.get(i).getId();
                }
                whiteListExtendRecordService.delete(deleteIds);
            }
        }
        if(StringUtils.isNotBlank(whilteid)&&md5.size()>0 ){
            whiteListRecordService.removeCache(whilteid,md5);
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????????????????");
    }

    @RequestMapping(value = "/queryCountByCondition/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "????????????????????????????????????")
    @SystemLog(description = "????????????????????????????????????")
    public Result<Long> queryCountByCondition(@ModelAttribute WhiteListRecord whiteListRecord,
                                              @ModelAttribute SearchVo searchVo,
                                              @ModelAttribute PageVo pageVo,
                                              @PathVariable String id) {
        long count = whiteListRecordService.count(new Specification<WhiteListRecord>() {
            @Override
            public javax.persistence.criteria.Predicate toPredicate(Root<WhiteListRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

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
        });
        return new ResultUtil<Long>().setData(count);
    }
}
