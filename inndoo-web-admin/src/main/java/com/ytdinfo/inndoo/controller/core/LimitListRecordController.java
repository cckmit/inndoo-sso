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
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.utils.excel.WaterExcelUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.AccountFormMetaService;
import com.ytdinfo.inndoo.modules.core.service.LimitListExtendRecordService;
import com.ytdinfo.inndoo.modules.core.service.LimitListRecordService;
import com.ytdinfo.inndoo.modules.core.service.LimitListService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILimitListExtendRecordService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILimitListRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
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
@Api(description = "受限名单清单管理接口")
@RequestMapping("/limitlistrecord")
public class LimitListRecordController extends BaseController<LimitListRecord, String> {

    @Autowired
    private LimitListRecordService limitListRecordService;

    @Autowired
    private LimitListService limitListService;

    @Autowired
    private AccountFormMetaService accountFormMetaService;

    @Autowired
    private LimitListExtendRecordService limitListExtendRecordService;

    @Autowired
    private ILimitListRecordService iLimitListRecordService;

    @Autowired
    private ILimitListExtendRecordService iLimitListExtendRecordService;

    @Autowired
    private WaterExcelUtil waterExcelUtil;

    @Override
    public LimitListRecordService getService() {
        return limitListRecordService;
    }


    @RequestMapping(value = "/listByCondition/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<LimitListRecord>> listByCondition(@ModelAttribute LimitListRecord limitListRecord,
                                                         @ModelAttribute SearchVo searchVo,
                                                         @ModelAttribute PageVo pageVo,
                                                         @PathVariable String id) {
        String appid = UserContext.getAppid();
        limitListRecord.setAppid(appid);
        Page<LimitListRecord> page = limitListRecordService.
                findByCondition(limitListRecord, searchVo, PageUtil.initPage(pageVo), id);
        List<String> identifiers =new ArrayList<>();
        if (page.getTotalElements() > 0) {
            LimitList limitList = limitListService.get(id);
            for (int i = 0; i < page.getContent().size(); i++) {
                LimitListRecord limitListRecord1= page.getContent().get(i);
                if (limitList.getListType() == 2) {
                    if(limitList.getIsEncryption() == 0){
                        page.getContent().get(i).setIdentifier(AESUtil.decrypt(limitListRecord1.getIdentifier()) );
                    }else {
                        page.getContent().get(i).setIdentifier(limitListRecord1.getIdentifier());
                    }
                }
                if(limitList.getListType() == 0){
                    identifiers.add(limitListRecord1.getIdentifier());
                }
                page.getContent().get(i).setListType(limitList.getListType());
            }
            if(limitList.getListType() == 0 && identifiers.size()>0){
                List<LimitListExtendRecord>  records = limitListExtendRecordService.findByListIdAndIdentifierIn(id, identifiers);
                List<LimitListExtendRecord> limitListExtendRecords = new ArrayList<>();
                limitListExtendRecords.addAll(records);
                Map<String,List<LimitListExtendRecord>> map = new HashMap<>();
                for( LimitListExtendRecord limitListExtendRecord: limitListExtendRecords){
                    List<LimitListExtendRecord>  tempList=  map.get(limitListExtendRecord.getIdentifier());
                    if(tempList == null){
                        tempList = new ArrayList<>();
                    }
                    limitListExtendRecord.setRecord(AESUtil.decrypt(limitListExtendRecord.getRecord()));
                    tempList.add(limitListExtendRecord);
                    map.put(limitListExtendRecord.getIdentifier(),tempList);
                }

                for (int i = 0; i < page.getContent().size(); i++) {
                    String tempIdentifier =  page.getContent().get(i).getIdentifier();
                    List<LimitListExtendRecord>  tempList=  map.get(tempIdentifier);
                    if( tempList !=null){
                        page.getContent().get(i).setExtendInfo(tempList);
                    }
                }
            }
        }
        return new ResultUtil<Page<LimitListRecord>>().setData(page);
    }

    /**
     * 获取OfflineRecordingInputVo
     * @return
     */
    @RequestMapping(value = "/getOfflineRecordingInputVo", method = RequestMethod.GET)
    @ApiOperation(value = "拼接名单提交信息")
    public Result<OfflineRecordingInputVo> getOfflineRecordingInputVo(@RequestParam String listId ){
        LimitList limitList = limitListService.get(listId);
        if(null == limitList){
            return new ResultUtil<OfflineRecordingInputVo>().setErrorMsg("白名单不存在");
        }
        OfflineRecordingInputVo offlineRecordingInputVo = new OfflineRecordingInputVo();
        offlineRecordingInputVo.setListId(listId);
        offlineRecordingInputVo.setListType(limitList.getListType().toString());
        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = new ArrayList<>();
        if(limitList.getListType() == 1 || limitList.getListType() == 5){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(limitList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("openid");
            offlineRecordingFieldVo.setMetaName("openid");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if(limitList.getListType() == 2){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(limitList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("phone");
            offlineRecordingFieldVo.setMetaName("手机号");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if (limitList.getListType() == 3 ){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(limitList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("accountid");
            offlineRecordingFieldVo.setMetaName("小核心账号");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if (limitList.getListType() == 4 ){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(limitList.getListType().toString());
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setMetaType("actAccountid");
            offlineRecordingFieldVo.setMetaName("活动平台账号");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        } if (limitList.getListType() == 0 ) {
            String validateFields = limitList.getValidateFields();
            String[] validateFieldes = validateFields.split(",");
            for (String validateField : validateFieldes){
                if(StrUtil.isNotBlank(validateField)){
                    AccountFormMeta accountFormMeta = accountFormMetaService.get(validateField);
                    if(null != accountFormMeta) {
                        OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
                        offlineRecordingFieldVo.setMetaId(validateField);
                        offlineRecordingFieldVo.setListId(listId);
                        offlineRecordingFieldVo.setListType(limitList.getListType().toString());
                        offlineRecordingFieldVo.setFieldData("");
                        offlineRecordingFieldVo.setMetaType(accountFormMeta.getMetaType());
                        offlineRecordingFieldVo.setMetaName(accountFormMeta.getTitle());
                        offlineRecordingFieldVos.add(offlineRecordingFieldVo);
                    }
                }
            }
        }
        if(limitList.getIsTimes().intValue() == 1){
            OfflineRecordingFieldVo offlineRecordingFieldVo = new OfflineRecordingFieldVo();
            offlineRecordingFieldVo.setMetaId("");
            offlineRecordingFieldVo.setListId(listId);
            offlineRecordingFieldVo.setListType(limitList.getListType().toString());
            offlineRecordingFieldVo.setFieldData("");
            offlineRecordingFieldVo.setMetaType("times");
            offlineRecordingFieldVo.setMetaName("资格次数");
            offlineRecordingFieldVos.add(offlineRecordingFieldVo);
        }
        offlineRecordingInputVo.setOfflineRecordingFieldVos(offlineRecordingFieldVos);
        return new ResultUtil<OfflineRecordingInputVo>().setData(offlineRecordingInputVo);
    }

    @RequestMapping(value = "/saveListRecord", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加限制名单名单")
    public Result<LimitListRecord> saveLimitListRecord(@RequestBody OfflineRecordingInputVo offlineRecordingInputVo){
        LimitList limitList = limitListService.get(offlineRecordingInputVo.getListId());
        LimitListRecord w = new LimitListRecord();
        if(null == limitList){
            return new ResultUtil<LimitListRecord>().setErrorMsg("名单不存在");
        }
        List<OfflineRecordingFieldVo> offlineRecordingFieldVos = offlineRecordingInputVo.getOfflineRecordingFieldVos();
        for (OfflineRecordingFieldVo offlineRecordingFieldVo: offlineRecordingFieldVos){
            if (StrUtil.isBlank(offlineRecordingFieldVo.getFieldData()))  {
                return new ResultUtil<LimitListRecord>().setErrorMsg(offlineRecordingFieldVo.getMetaName()+"值不能为空！");
            }
        }
        String identifier = "";
        if(limitList.getListType() == 1 || limitList.getListType() == 5 ){
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
                limitListExtendRecord.setRecord(AESUtil.encrypt(offlineRecordingFieldVo.getFieldData()));
                limitListExtendRecords.add(limitListExtendRecord);
            }
            w.setExtendInfo(limitListExtendRecords);
            identifier = limitListRecordService.getMd5identifier(limitList,w);
        }
        w.setIdentifier(identifier);
        Boolean existence = limitListRecordService.existsByListIdAndIdentifier(w.getListId(),w.getIdentifier());
        if(existence){
            return new ResultUtil<LimitListRecord>().setErrorMsg("已在名单内");
        }
        limitListRecordService.save(w);
        if(CollectionUtil.isNotEmpty(limitListExtendRecords)){
            for(LimitListExtendRecord limitListExtendRecord:limitListExtendRecords){
                limitListExtendRecord.setIdentifier(identifier);
            }
            limitListExtendRecordService.saveOrUpdateAll(limitListExtendRecords);
        }
        limitListRecordService.loadSingleCache(limitList.getId(),w);
        return new ResultUtil<LimitListRecord>().setData(w);
    }

    // 私有化导入监听器
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
    @ApiOperation(value = "导入达标名单信息")
    @SystemLog(description = "导入达标名单信息")
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                      HttpServletRequest request, HttpSession session,
                                      @RequestParam(value = "formId", required = true) String accountFormId,
                                      Integer listType, String limitListId, String validateFields) throws SQLException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        /*1.读Excel, 检查是否存在
          2.数据处理（判定这个名单是否含高级校验），如果含高级校验组装扩展表的数据
          2.批量插入
         */
        if(file == null){
            return new ResultUtil<Object>().setErrorMsg("未能读取到文件");
        }
        String fileName=file.getOriginalFilename();
        if(StringUtils.isBlank(fileName) ){
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        String suffix = StringUtils.substring(fileName,StringUtils.lastIndexOf(fileName,"."));
        if(!StringUtils.equalsAnyIgnoreCase(suffix,".xlsx") &&  !StringUtils.equalsAnyIgnoreCase(suffix,".xls") ){
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        // TODO: 2019/10/16 读Excel, 检查是否存在
        long start = System.currentTimeMillis();
        List<String> listString = Arrays.asList(validateFields.split(","));
        String message = "";
        int count = 0;
        String filePath = ReadExcelUtil.FilePath(file, request);
        // excel读取方法
        ExcelListener excelListener = new ExcelListener();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            return new ResultUtil<Object>().setErrorMsg("文件读取异常！");
        }
        List<ExcelRow> excelRows = excelListener.getDatas();
        LimitList limitList = limitListService.get(limitListId);
        if (limitList == null) {
            return new ResultUtil<Object>().setErrorMsg("导入参数异常！");
        }
        long redtime = (System.currentTimeMillis()-start)/1000;
        long preTime = 0;
        long writeTime1 = 0;
        long writeTime2 = 0;
        long writeTime3 = 0;
        long writeTime4 = 0;
        // 总数据
        if (listType != 0) {
            if (excelRows != null && excelRows.size() > 0) {
                // 校验excel头部
                ExcelRow excelHead = excelRows.get(0);
                if (excelHead == null) {
                    return new ResultUtil<Object>().setErrorMsg("没有头列导入模板错误！");
                }
                if (!ExcelConstant.IDENTIFER_EXCEL_COLUMN.equals(excelHead.getColum0())) {
                    return new ResultUtil<Object>().setErrorMsg("未包含用户识别码列,导入模板错误！");
                }
                if (limitList.getIsTimes() == 1&& !ExcelConstant.LEFT_TIMES_COLUMN.equals(excelHead.getColum1())) {
                    return new ResultUtil<Object>().setErrorMsg("未包含资格次数列,导入模板错误！");
                }
                excelRows.remove(0);
                // 批量处理
                List<LimitListRecord> limitListRecords = new ArrayList<>();
                List<String> deleteIdentifers = new ArrayList<>();
                for (ExcelRow temp : excelRows) {
                    LimitListRecord w = new LimitListRecord();
                    if(StrUtil.isBlank(temp.getColum0())){
                        continue;
                    }
                    String identifier = temp.getColum0().trim();
                    if (limitList.getIsEncryption() == 0 && limitList.getListType() == 2) {
                        identifier = PhoneUtil.dealPhoneNumber(identifier);
                        Boolean isPhone = PhoneUtil.isMobileNO(identifier );
                        if(!isPhone){
                            return new ResultUtil<Object>().setErrorMsg("电话号码"+identifier + "格式不正确");
                        }
                        identifier = AESUtil.encrypt(identifier);
                    }
                    // 加密，转大写
                    if(limitList.getIsEncryption() == 1){
                        identifier = identifier.toUpperCase();
                    }
                    Integer times=1;
                    String timesStr = temp.getColum1();
                    if(StringUtils.isNumeric(timesStr)){
                        times = Integer.valueOf(timesStr);
                    }
                    w.setListType(listType);
                    w.setIdentifier(identifier);
                    w.setListId(limitListId);
                    w.setCreateTime(new Date());
                    w.setUpdateTime(new Date());
                    w.setTimes(times);
                    deleteIdentifers.add(identifier);
                    limitListRecords.add(w);
                }
                preTime = (System.currentTimeMillis()-start)/1000 - redtime;
                if(deleteIdentifers.size()>0){
                    iLimitListRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,3000,limitListId);
                }
                writeTime1 = (System.currentTimeMillis()-start)/1000 - preTime;
                iLimitListRecordService.saveBatchWithIgnore(limitListRecords, 10000);
                writeTime2 = (System.currentTimeMillis()-start)/1000 - writeTime1;
            }
        } else {
            if (excelRows != null && excelRows.size() > 0) {
                // 校验头部
                ExcelRow tempHead = excelRows.get(0);
                if (tempHead == null) {
                    return new ResultUtil<Object>().setErrorMsg("导入模板错误！");
                }
                //高级校验字段
                List<LimitListExtendRecord> extendList = new ArrayList<>();
                List<AccountFormMeta> formMetas = new ArrayList<>();
                for (String s : listString) {
                    AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                    formMetas.add(formMeta);
                }
                AccountFormMeta timesformMeta = new AccountFormMeta();
                timesformMeta.setTitle("资格次数");
                formMetas.add(timesformMeta);
                String[] heads = tempHead.getColums();
                for(int i =0 ;i<formMetas.size();i++){
                    if (StringUtils.isNotBlank(heads[i]) && !StringUtils.equals(heads[i], formMetas.get(i).getTitle())) {
                        return new ResultUtil<Object>().setErrorMsg("导入模板错误！");
                    }
                }
                excelRows.remove(0);
                // 组装插入list
                List<String> deleteIdentifers = new ArrayList<>();
                List<LimitListRecord> all = multipleHandle(accountFormId, limitList, listType, excelRows, formMetas,extendList,deleteIdentifers);
                preTime = (System.currentTimeMillis()-start)/1000 - redtime;
                writeTime1 = (System.currentTimeMillis()-start)/1000 - preTime;
                if(deleteIdentifers.size()>0){
                    iLimitListRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,3000,limitListId);
                }
                writeTime2 = (System.currentTimeMillis()-start)/1000 - writeTime1;
                boolean b1 = iLimitListRecordService.saveBatchWithIgnore(all, 10000);
                writeTime3 = (System.currentTimeMillis()-start)/1000 - writeTime2;
                if(deleteIdentifers.size()>0){
                    iLimitListExtendRecordService.deleteBatchByIdentifersAndListId(deleteIdentifers,limitListId,3000);
                }
                writeTime4 = (System.currentTimeMillis()-start)/1000 - writeTime3;
                iLimitListExtendRecordService.saveBatchWithIgnore(extendList, 10000);

            }
        }
        limitListRecordService.loadCache(limitListId);
        System.out.println("总耗时"+(System.currentTimeMillis()-start)/1000);
        return new ResultUtil<Object>().setSuccessMsg("导入成功");
//        return new ResultUtil<Object>().setSuccessMsg("导入成功, \\n"
//                +"读取耗时："+redtime
//                +" \\n组装耗时:"+preTime
//                +" \\n写入耗时1:"+writeTime1
//                +" \\n写入耗时2:"+writeTime2
//                +" \\n写入耗时3:"+writeTime3
//                +" \\n写入耗时4:"+writeTime4
//                +" \\n总耗时"+(System.currentTimeMillis()-start)/1000);
    }

    /**
     * excel头部数据
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
     * 数据去重
     *
     * @param all
     * @return
     */
    private List<LimitListRecord> deduplication(List<LimitListRecord> all) {
        return all.parallelStream().distinct()
                .filter(distinctByKey(b -> b.getIdentifier()))
                .collect(toList());
    }

    /**
     * 处理数据（没有高级校验）
     *
     * @param accountFormId
     * @param limitListId
     * @param listType
     * @param objects
     * @return
     */
    private List<LimitListRecord> singleHandle(String accountFormId, String limitListId, Integer listType, List<Object> objects) {
        List<LimitListRecord> all = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            if (i != 0) {
                String str = objects.get(i).toString();
                String demosub = str.substring(1, str.length() - 1);
                String[] demoArray = demosub.split(",");
                LimitListRecord record = new LimitListRecord();
                record.setIdentifier(demoArray[0].trim());
                record.setListType(listType);
                record.setListId(limitListId);
                all.add(record);
            }
        }
        return all;
    }

    /**
     * 处理数据（高级校验）
     *
     * @param accountFormId
     * @param limitList
     * @param listType
     * @param objects
     * @param formMetaMap
     * @return
     */
    private List<LimitListRecord> multipleHandle(String accountFormId, LimitList limitList, Integer listType,
                                                 List<ExcelRow> objects, List<AccountFormMeta> formMetaMap,
                                                 List<LimitListExtendRecord> extendList, List<String> deleteIdentifers) {
        List<LimitListRecord> all = new ArrayList();
        //将数据转成List<LimitListRecord>
        for (ExcelRow temp : objects) {
            LimitListRecord record = new LimitListRecord();
            record.setListType(listType);
            record.setListId(limitList.getId());
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            List<LimitListExtendRecord> newExtendRecords = new ArrayList<>();
            String[] colums = temp.getColums();
            Date date = new Date();
            for (int i = 0; i < colums.length; i++) {
                if (i <= formMetaMap.size() && StringUtils.isNotBlank(colums[i]) && i <formMetaMap.size() - 1 ) {
                    AccountFormMeta accountFormMeta = formMetaMap.get(i);
                    LimitListExtendRecord extendRecord = new LimitListExtendRecord();
                    String filedrecord = colums[i].trim();
                    //如果是电话号码判断电话号码是否正确
                    if(accountFormMeta.getMetaType().equals("phone")){
                        filedrecord = PhoneUtil.dealPhoneNumber(colums[i].trim());
                        Boolean isPhone = PhoneUtil.isMobileNO(filedrecord);
                        if(!isPhone){
                            throw new InndooException("电话号码"+colums[i] + "格式不正确");
                        }
                    }
                    extendRecord.setListId(limitList.getId());
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
                    Integer times=0;
                    String timesStr = colums[i];
                    if(StringUtils.isNumeric(timesStr)){
                        times = Integer.valueOf(timesStr);
                    }
                    record.setTimes(times);
                }
            }
            record.setExtendInfo(newExtendRecords);
            String identifier = limitListRecordService.getMd5identifier(limitList,record);
            for(LimitListExtendRecord extendRecord :newExtendRecords){
                extendRecord.setIdentifier(identifier);
            }
            record.setIdentifier(identifier);
            record.setExtendInfo(newExtendRecords);
            record.setCreateTime(new Date());
            deleteIdentifers.add(identifier);
            all.add(record);
        }
        return all;
    }

    /**
     * 根据String型时间，获取long型时间，单位毫秒
     *
     * @param inVal 时间字符串
     * @return long型时间
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
    @ApiOperation(value = "导出动态受限名单信息模板")
    @SystemLog(description = "导出动态受限名单信息模板")
    public void exportTemplate(@PathVariable String id, HttpServletResponse response)  throws IOException {
        LimitList limitList = limitListService.get(id);
        List<String> listString = Arrays.asList(limitList.getValidateFields().split(","));
        List<List<String>> rows = CollUtil.newArrayList();
        List<String> list = new ArrayList();
        if (limitList.getListType() != 0) {
            list.add("用户识别码");
        }
        for (String s : listString) {
            if (!StrUtil.isBlank(s)) {
                AccountFormMeta formMeta = accountFormMetaService.get(s.trim());
                list.add(formMeta.getTitle());
            }
        }
        if(limitList.getIsTimes() == 1){
            list.add("资格次数");
        }
        List<String> row1 = CollUtil.newArrayList(list);
        rows.add(row1);
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String path = jarFile.getParentFile().getPath();
//        String rootPath = path + File.separator + "static/ytdexports";
//        File dir = new File(rootPath);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
        String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFileName  = "受限名单导入模板_"+limitList.getName()+downloadDate+".xlsx";

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
            response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("下载模板失败")));
        }finally {
            out.flush();
            writer.finish();
            out.close();
        }

//        String fullFileName = rootPath + File.separator + excelFileName;
//        BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
//        // 一次性写出内容，使用默认样式
//        writer.write(rows);
//        // 关闭writer，释放内存
//        writer.close();
//        File file = new File(fullFileName);
//        ServletUtil.write(response, file);
//        file.delete();
//        return new ResultUtil<Object>().setSuccessMsg("OK");
    }

    @RequestMapping(value = "/export/{id}")
    @ApiOperation(value = "导出受限名单记录信息")
    @SystemLog(description = "导出受限名单记录信息")
    public void export(@ModelAttribute LimitListRecord limitListRecord,
                       @ModelAttribute SearchVo searchVo,
                       @ModelAttribute PageVo pageVo,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       @PathVariable String id) throws IOException {

        LimitList limitList = limitListService.get(id);
        List<List<String>> rows = limitListRecordService.toWrite(id);

        String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFileName  = "受限名单导出信息_"+limitList.getName()+downloadDate+".xlsx";
        waterExcelUtil.writeForList(response,excelFileName,rows);


//        ExcelWriter writer = null;
//        OutputStream out = null;
//        try {
////            String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
//            String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//            String excelFileName  = "受限名单导出信息_"+limitList.getName()+downloadDate+".xlsx";
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
//            // 重置response
//            response.reset();
//            response.setContentType("application/json");
//            response.setCharacterEncoding("utf-8");
//            response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("下载文件失败")));
//        }finally {
//            out.flush();
//            writer.finish();
//            out.close();
//        }
    }
    @RequestMapping(value = "/deleteByListId/{listId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过listId删除数据")
    @SystemLog(description = "通过listId删除数据")
    public Result<Object> deleteById(@PathVariable String listId){

        getService().deleteByListId(listId);
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }

    @Override
    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量删除")
    @Transactional(rollbackFor = Exception.class)
    @SystemLog(description = "通过id批量删除")
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids) {
        String limitid = "";

        List<String> md5 = new ArrayList<>();
        for (String id : ids) {
            LimitListRecord record = limitListRecordService.get(id);
            limitid = record.getListId();
            md5.add(record.getIdentifier());
            String recordId = record.getId();
            String listId = record.getListId();
            limitListRecordService.delete(id);
            List<LimitListExtendRecord> extrecords = limitListExtendRecordService.findByListIdAndRecordId(listId, recordId);
            if (extrecords.size() > 0) {
                String[] deleteIds = new String[extrecords.size()];
                for(int i = 0 ;i<extrecords.size() ;i++){
                    deleteIds[i] = extrecords.get(i).getId();
                }
                limitListExtendRecordService.delete(deleteIds);
            }
        }
        if(StringUtils.isNotBlank(limitid)&&md5.size()>0 ){
            limitListRecordService.removeCache(limitid,md5);
        }
        return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
    }

    @RequestMapping(value = "/queryCountByCondition/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "根据条件查询达标记录数量")
    @SystemLog(description = "根据条件查询达标记录数量")
    public Result<Long> queryCountByCondition(@ModelAttribute LimitListRecord limitListRecord,
                                              @ModelAttribute SearchVo searchVo,
                                              @ModelAttribute PageVo pageVo,
                                              @PathVariable String id) {
        long count = limitListRecordService.count(new Specification<LimitListRecord>() {
                                                      @Override
                                                      public javax.persistence.criteria.Predicate toPredicate(Root<LimitListRecord> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                                                          Path<String> listIdField = root.get("listId");
                                                          List<javax.persistence.criteria.Predicate> list = new ArrayList<javax.persistence.criteria.Predicate>();

                                                          //达标名单id
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
