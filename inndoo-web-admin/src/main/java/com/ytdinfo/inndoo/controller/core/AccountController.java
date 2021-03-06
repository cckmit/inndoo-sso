package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.ExcelConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.enums.ThreadPoolType;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.utils.excel.WaterExcelUtil;
import com.ytdinfo.inndoo.common.utils.excel.WaterMarkHandler;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.base.service.FileService;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.entity.export.AccountExport;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountFormFieldService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountService;
import com.ytdinfo.inndoo.utils.PrivacyUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "????????????????????????")
@RequestMapping("/account")
public class AccountController extends BaseController<Account, String> {

    @Autowired
    private AccountService accountService;

    @Override
    public AccountService getService() {
        return accountService;
    }

    @Autowired
    private AccountFormFieldService accountFormFieldService;

    @Autowired
    private IAccountService iAccountService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @Autowired
    private ActAccountService actAccountService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private FileService fileService;
    @Autowired
    private IAccountFormFieldService iAccountFormFieldService;

    @Autowired
    private RabbitUtil rabbitUtil;
    @Autowired
    private AccountFormService accountFormService;

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????")
    @SystemLog(description = "?????????????????????")
    public Result<Object> listByCondition(@ModelAttribute Account account,
                                             @ModelAttribute SearchVo searchVo,
                                             @ModelAttribute PageVo pageVo) {
        String appid = UserContext.getAppid();
        account.setAppid(appid);
        Map<String,Object> map =new HashMap<>();
        Page<Account> page = accountService.findByCondition(account, searchVo, PageUtil.initPage(pageVo));
        List<Map<String,Object>> accountOutVos = new ArrayList<>();
        // account ??????
        List<Account> accounts = page.getContent();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        AccountForm accountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(),true);
        List<AccountFormMeta> identifierFormMetas = accountForm.getAccountFormMetas();
        for(Account item:accounts){

            List<ActAccount>  actAccounts = actAccountService.findByCoreAccountId(item.getId());
            Account copyaccount = new Account();
            BeanUtils.copyProperties(item,copyaccount);
            String json = JSONUtil.toJsonPrettyStr(accountService.decryptAccount(copyaccount));
            Map<String,Object> jsonMap = JSONUtil.toBean(json,Map.class);
            Map<String,Object> resulTMap = new HashMap<>();
            for(AccountFormMeta accountFormMeta: identifierFormMetas){
                String value=jsonMap.get(accountFormMeta.getMetaType()).toString();
                if (accountFormMeta.getMetaType().equals("phone"))
                {
                    value=  PrivacyUtil.phoneEncrypt(value);
                }
               else if (accountFormMeta.getMetaType().equals("idcard"))
                {
                    value=  PrivacyUtil.formatToMask(value);
                }
                resulTMap.put(accountFormMeta.getMetaType(),value);
            }
            resulTMap.put("id",item.getId());
            resulTMap.put("createTime",formatter.format(item.getCreateTime()));
            resulTMap.put("isStaff",item.getIsStaff());
            if(CollectionUtil.isEmpty(actAccounts)) {
                resulTMap.put("isbind",false);
            }else {
                resulTMap.put("isbind",true);
            }
            accountOutVos.add(resulTMap);
        }
        map.put("total",page.getTotalElements());


        map.put("content",accountOutVos);
        return new ResultUtil<Object>().setData(map);
    }

    @RequestMapping(value = "/dataMigration", method = RequestMethod.GET)
    @ApiOperation(value = "1.0???????????????????????????2.0??????")
    @SystemLog(description = "1.0???????????????????????????2.0??????")
    public Result<Object> dataMigration(){
        activityApiUtil.dataMigration();
        return new ResultUtil<Object>().setSuccessMsg("????????????????????????????????????????????????");
    }

    @RequestMapping(value = "/export", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????")
    @SystemLog(description = "??????????????????")
    @ResponseBody
    public Result<Map<String, Object>> export(@ModelAttribute Account account,
                                 @ModelAttribute SearchVo searchVo,
                                 @ModelAttribute PageVo pageVo,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        Integer pageSize = 1000;
        String checkId = "";
        Boolean check = true;
        List<Account> list = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        if(StrUtil.isNotBlank(account.getActAccountId())){
            Account saccount = accountService.get(account.getActAccountId());
            if ( null != saccount ) {
                ids.add(saccount.getId());
            } else {
                ActAccount actAccount = actAccountService.findByActAccountId(account.getActAccountId().trim());
                //????????????????????????
                if (null != actAccount) {
                    ids.add(actAccount.getCoreAccountId().trim());
                }else {
                    ids.add("err");
                }
            }
        }
        if (StrUtil.isNotBlank(account.getAccountFormFieldValue())) {
            List<String> accountIds = iAccountFormFieldService.findAccountIdsByFieldData(account.getAccountFormFieldValue());
            if (CollectionUtil.isNotEmpty(accountIds)) {
                ids.addAll(accountIds);
            } else {
                //?????????????????????????????????
                ids.add("err");
            }
        }
        Integer page = 1;
        while(check){
            Map<String,Object> map = new HashMap<>();
            map.put("appid",UserContext.getAppid());
            if (StrUtil.isNotBlank(account.getPhone())) {
                map.put("phone",AESUtil.encrypt(account.getPhone()));
            }
            if (StrUtil.isNotBlank(account.getCustomerNo())) {
                map.put("customerNo",AESUtil.encrypt(account.getCustomerNo()));
            }
            if (StrUtil.isNotBlank(account.getName())) {
                map.put("name",AESUtil.encrypt(account.getName()));
            }
            if (StrUtil.isNotBlank(account.getIdcardNo())) {
                map.put("idcardNo",AESUtil.encrypt(account.getIdcardNo()));
            }
            if (StrUtil.isNotBlank(account.getBankcardNo())) {
                map.put("bankcardNo",AESUtil.encrypt(account.getBankcardNo()));
            }
            if (StrUtil.isNotBlank(account.getEmail())) {
                map.put("email",AESUtil.encrypt(account.getEmail()));
            }
            if (StrUtil.isNotBlank(account.getAddress())) {
                map.put("address",AESUtil.encrypt(account.getAddress()));
            }
            if (StrUtil.isNotBlank(account.getStaffNo()) ) {
                map.put("staffNo",AESUtil.encrypt(account.getStaffNo()));
            }
            if (StrUtil.isNotBlank(account.getDeptNo())) {
                map.put("deptNo",AESUtil.encrypt(account.getDeptNo()));
            }
            if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                map.put("startDate",searchVo.getStartDate());
                map.put("endDate",searchVo.getEndDate());
            }
            if (CollectionUtil.isNotEmpty(ids)) {
                map.put("accountIds",ids);
            }
            map.put("page",0);
            map.put("pageSize",pageSize);
            map.put("checkId",checkId);
            List<Account> volist = iAccountService.findByMap(map);
            if(CollectionUtil.isNotEmpty(volist)){
                list.addAll(volist);
                if(volist.size() < pageSize){
                    check = false;
                }else {
                    Account acc = volist.get(volist.size() - 1);
                    checkId = acc.getId();
                    page = page + 1;
                }

            }else {
                check = false;
            }
        }
     /*   for (Account a : list) {
            List<AccountFormField> accountFormFieldList = accountFormFieldService.findByAccountId( a.getId());
            if (accountFormFieldList.size() > 0) {
                a.setAccountFormFields(accountFormFieldList);
            }
        }*/
        //List<?> row1 = CollUtil.newArrayList("?????????", "??????","??????", "?????????",  "????????????", "????????????", "???????????????", "??????", "??????", "?????????????????????");
        // Vector<List<?>> rows = new Vector<>();
        //rows.add(row1);
        List<AccountExport> accountExportList = Collections.synchronizedList(new ArrayList<>());
        ThreadPoolExecutor pool1 = ThreadPoolUtil.createPool(ThreadPoolType.handleAccount);
        for (Account acc : list) {
            pool1.execute(() -> {
                //??????
                Account b = new Account();
                BeanUtils.copyProperties(acc,b);
                b = accountService.decryptAccount(b);
                // List<?> row = CollUtil.newArrayList(b.getCustomerNo(),b.getId(), b.getName(), b.getPhone(),  b.getBirthday(), b.getBankcardNo(), b.getIdcardNo(), b.getEmail(), b.getAddress(), b.getIdentifier());
                //rows.add(row);

                AccountExport accountExport = new AccountExport();
                BeanUtils.copyProperties(b,accountExport);
                accountExportList.add(accountExport);

            });
        }
        pool1.shutdown();
        while (!pool1.isTerminated()) {
            try {
                pool1.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String path = jarFile.getParentFile().getPath();
        String rootPath = path + File.separator + "static/ytdexports";
        File dir = new File(rootPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
        String fullFileName = rootPath + File.separator + excelFileName;

        EasyExcel.write(fullFileName, AccountExport.class)
                .inMemory(true) // ??????????????????????????????
                .registerWriteHandler(new WaterMarkHandler(WaterExcelUtil.waterRemark()))
                .sheet("sheet1")
                .doWrite(accountExportList);

//        BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
//        // ??????????????????????????????????????????
//        writer.write(rows);
//        // ??????writer???????????????
//        writer.close();
        File filee = new File(fullFileName);
        Map<String, Object> map = new HashMap<>();
        String contentType = ".xlsx";
        Result<Object> result = fileService.upload(filee, contentType);
        if (result.isSuccess()) {
            filee.delete();
            //   System.out.println(result.getResult().toString());
            map.put("url", result.getResult().toString());

        } else {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("??????????????????");
        }
        return new ResultUtil<Map<String, Object>>().setData(map);
    }

    @RequestMapping(value = "/untiedPhone/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "????????????")
    @SystemLog(description = "????????????")
    public Result<Object> untiedPhone(@PathVariable String[] ids) {
        List<String> list = new ArrayList<>();
        for (String id : ids) {
            list.add(id);
        }
        List<Account> accounts = accountService.listByIds(list);
        for (Account a : accounts) {
            a.setPhone("");
        }
        iAccountService.updateBatchById(accounts, 10000);
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/untied/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "????????????")
    @Transactional(rollbackFor = Exception.class)
    @SystemLog(description = "????????????")
    @ResponseBody
    public Result<Object> untied(@PathVariable String[] ids) {
        if(null == ids){
            return new ResultUtil<Object>().setErrorMsg("???????????????");
        }
        List<Account> accounts = new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String id : ids) {
            Account account = accountService.get(id);
            if (null != account ) {
                accounts.add(account);
            }
            list.add(id);
        }
        //?????????????????????act???core???????????????
        List<ActAccount> actAccounts = actAccountService.findByCoreAccountIds(list);
        if(null != actAccounts && actAccounts.size()>0 ){
            actAccountService.delete(actAccounts);
        }
        //??????????????????????????????
        List<Staff> staffs = staffService.findByAccountIds(list);
        for (Staff staff: staffs) {
            staffService.removeFromCache(staff.getAccountId());
            staff.setAccountId("");
        }
        if(null != staffs && staffs.size()>0){
            staffService.saveOrUpdateAll(staffs);
        }
        //?????????????????????????????????????????????
        if(CollectionUtil.isNotEmpty(accounts)){
            for (Account account: accounts){
                account = accountService.clearAccount(account);
                if(account.getIsStaff() == 1 ){
                    Integer isStaff = 0;
                    account.setIsStaff(isStaff);
                    account.setStaffNo("");
                }
                accountService.save(account);
            }
        }
        //??????act?????????????????????
        Boolean untiedAccount = activityApiUtil.untied(list);
        if(!untiedAccount ){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<Object>().setErrorMsg("act???????????????????????????");
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }


    @RequestMapping(value = "/getAccountByCoreAccountId", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "??????act????????????")
    @SystemLog(description = "??????act????????????")
    public Result<List<ActAccountVo>> getAccountByCoreAccountId(@RequestParam String coreAccountId){
        List<ActAccountVo> list = activityApiUtil.getCoreAccountId(coreAccountId);

        return new ResultUtil<List<ActAccountVo>>().setData(list);
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
    @ApiOperation(value = "??????????????????")
    @SystemLog(description = "??????????????????")
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                      HttpServletRequest request, HttpSession session) throws SQLException {
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
//        Map<String,Object> accountFormMap = new HashMap<>();
//        accountFormMap.put("name","???????????????????????????");
//        accountFormMap.put("formType",1);
//        accountFormMap.put("appid",UserContext.getAppid());
//        List<AccountForm> accountForms = accountFormService.findByMap(accountFormMap);
//        if(CollectionUtil.isEmpty(accountForms)){
//            return new ResultUtil<Object>().setErrorMsg("????????????????????????????????? ?????????????????????????????????????????????");
//        }
        // ??????????????????
        String filePath = ReadExcelUtil.FilePath(file, request);
        // excel????????????
        AccountController.ExcelListener excelListener = new AccountController.ExcelListener();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
        }
        List<ExcelRow> excelRows = excelListener.getDatas();
        List<Account> accountList = new ArrayList<>();
        if (excelRows != null && excelRows.size() > 0) {
            // ??????excel??????
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????");
            }
            if (!"??????".equals(excelHead.getColum0())) {
                return new ResultUtil<Object>().setErrorMsg("??????????????????,?????????????????????");
            }
            if (!"?????????".equals(excelHead.getColum1())) {
                return new ResultUtil<Object>().setErrorMsg("?????????????????????,?????????????????????");
            }

            excelRows.remove(0);
            for (ExcelRow temp : excelRows) {
                Account account = new Account();
                account.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                account.setPhone(temp.getColum1());
                account.setActAccountId(temp.getColum0());
                if(StrUtil.isBlank(temp.getColum0())){
                    return new ResultUtil<Object>().setErrorMsg("??????????????????");
                }
                if(StrUtil.isBlank(temp.getColum1())){
                    return new ResultUtil<Object>().setErrorMsg("?????????????????????");
                }
                accountList.add(account);

            }
        }
        if(CollectionUtil.isNotEmpty(accountList)){
            for (Account account : accountList) {
                //??????mq????????????
                MQMessage<Account> mqMessageAccount = new MQMessage<Account>();
                mqMessageAccount.setAppid(UserContext.getAppid());
                mqMessageAccount.setTenantId(UserContext.getTenantId());
                mqMessageAccount.setContent(account);
                rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACCOUNT_INPUT_MSG), mqMessageAccount);
            }
        }else {
            return new ResultUtil<Object>().setErrorMsg("excel?????????????????????");
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");

    }

    @RequestMapping(value = "/query/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "??????id??????")
    @Override
    public Result<Account> query(@PathVariable String id){
        Account entity = getService().get(id);
        entity.setTmphone(PrivacyUtil.phoneEncrypt(entity.getPhone()));
        entity.setTmname(PrivacyUtil.nameEncrypt(entity.getName()));
        entity.setTmidcardNo(PrivacyUtil.formatToMask(entity.getIdcardNo()));
        entity.setTmstaffNo(PrivacyUtil.formatToMask(entity.getStaffNo()));
        entity.setTmbankcardNo(PrivacyUtil.formatToMask(entity.getBankcardNo()));
        entity.setTmaddress(PrivacyUtil.formatToMask(entity.getAddress()));
        entity.setTmcustomerNo(PrivacyUtil.formatToMask(entity.getCustomerNo()));
        return new ResultUtil<Account>().setData(entity);
    }
}
