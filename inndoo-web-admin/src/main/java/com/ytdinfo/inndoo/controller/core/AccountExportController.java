package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.ThreadPoolType;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.utils.excel.WaterExcelUtil;
import com.ytdinfo.inndoo.common.utils.excel.WaterMarkHandler;
import com.ytdinfo.inndoo.common.vo.ExcelRow;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.entity.Dict;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.base.service.DictService;
import com.ytdinfo.inndoo.modules.base.service.FileService;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.vo.NameListVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.BeanUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author yaochangning
 */
@Slf4j
@RestController
@Api(description = "账号自定义导出信息接口")
@RequestMapping("/accountexport")
public class AccountExportController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private FileService fileService;

    @Autowired
    private CustomerInformationService customerInformationService;

    @Autowired
    private ActAccountService actAccountService;

    @Autowired
    private DictDataService dictDataService;

    @Autowired
    private AccountFormMetaService accountFormMetaService;

    @Autowired
    private DictService dictService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private WhiteListService whiteListService;
    @Autowired
    private LimitListService limitListService;
    @Autowired
    private AchieveListService achieveListService;
    @Autowired
    private WhiteListRecordService whiteListRecordService;
    @Autowired
    private WhiteListExtendRecordService whiteListExtendRecordService;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private AchieveListExtendRecordService achieveListExtendRecordService;
    @Autowired
    private LimitListRecordService limitListRecordService;
    @Autowired
    private LimitListExtendRecordService limitListExtendRecordService;

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

    @RequestMapping(value = "/queryAllNameList", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取名单信息")
    public Result<List<NameListVo>> queryAllNameList(@RequestParam String listType) {

        List<NameListVo> listVos = new ArrayList<>();
        if (listType.equals("whitelist")) {
            List<WhiteList> list = whiteListService.findList(UserContext.getAppid());
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
        }
        if (listType.equals("limitlist")) {
            List<LimitList> list = limitListService.findList(UserContext.getAppid());
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
        }
        if (listType.equals("achievelist")) {
            List<AchieveList> list = achieveListService.findList(UserContext.getAppid());
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
        }
        Collator collator = Collator.getInstance(Locale.CHINESE);
        listVos.sort((o1, o2) -> collator.compare(o1.getName(), o2.getName()));
        return new ResultUtil<List<NameListVo>>().setData(listVos);
    }

    @RequestMapping(value = "/getCustomerInformationFields", method = RequestMethod.GET)
    @ApiOperation(value = "获取客户信息属性列表")
    @SystemLog(description = "获取客户信息属性列表")
    public Result<List<Map<String, Object>>> getCustomerInformationFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", "phone");
        map1.put("value", "手机号码");
        list.add(map1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", "customerNo");
        map2.put("value", "客户号");
        list.add(map2);
        Map<String, Object> map3 = new HashMap<>();
        map3.put("key", "name");
        map3.put("value", "姓名");
        list.add(map3);
        Map<String, Object> map4 = new HashMap<>();
        map4.put("key", "idcardNo");
        map4.put("value", "身份证号码");
        list.add(map4);
        Map<String, Object> map5 = new HashMap<>();
        map5.put("key", "bankcardNo");
        map5.put("value", "银行卡号");
        list.add(map5);
        Map<String, Object> map6 = new HashMap<>();
        map6.put("key", "birthday");
        map6.put("value", "出生日期");
        list.add(map6);
        Map<String, Object> map7 = new HashMap<>();
        map7.put("key", "email");
        map7.put("value", "邮箱");
        list.add(map7);
        Map<String, Object> map8 = new HashMap<>();
        map8.put("key", "address");
        map8.put("value", "地址");
        list.add(map8);
        Map<String, Object> map9 = new HashMap<>();
        map9.put("key", "bankBranchNo");
        map9.put("value", "分行编码");
        list.add(map9);
        Map<String, Object> map10 = new HashMap<>();
        map10.put("key", "bankBranchName");
        map10.put("value", "分行名称");
        list.add(map10);
        Map<String, Object> map11 = new HashMap<>();
        map11.put("key", "institutionalCode");
        map11.put("value", "机构编号");
        list.add(map11);
        Map<String, Object> map12 = new HashMap<>();
        map12.put("key", "institutionalName");
        map12.put("value", "机构名称");
        list.add(map12);
        Map<String, Object> map13 = new HashMap<>();
        map13.put("key", "customerGroupCoding");
        map13.put("value", "客群编码");
        list.add(map13);
        //获取客户信息拓展字段
        List<DictData> dictDatas = customerInformationService.findDictData();
        if (CollectionUtil.isNotEmpty(dictDatas)) {
            for (DictData dictData : dictDatas) {
                Map<String, Object> map = new HashMap<>();
                map.put("key", dictData.getValue());
                map.put("value", dictData.getTitle());
                list.add(map);
            }
        }
        return new ResultUtil<List<Map<String, Object>>>().setData(list);
    }

    @RequestMapping(value = "/getStaffFields", method = RequestMethod.GET)
    @ApiOperation(value = "获取员工信息属性列表")
    @SystemLog(description = "获取员工信息属性列表")
    public Result<List<Map<String, Object>>> getStaffFields() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", "staffNo");
        map1.put("value", "员工号");
        list.add(map1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", "name");
        map2.put("value", "姓名");
        list.add(map2);
        Map<String, Object> map3 = new HashMap<>();
        map3.put("key", "phone");
        map3.put("value", "手机号");
        list.add(map3);
        Map<String, Object> map4 = new HashMap<>();
        map4.put("key", "deptNo");
        map4.put("value", "机构编码");
        list.add(map4);
        Map<String, Object> map5 = new HashMap<>();
        map5.put("key", "title");
        map5.put("value", "机构名称");
        list.add(map5);
        return new ResultUtil<List<Map<String, Object>>>().setData(list);
    }

    //账户的下标位置
    volatile Integer accountIndex = 0;

    //录入名单详细id的下标位置
    volatile Integer listIndex = 0;

    //查询出的客户信息匹配数据
    volatile Integer selectCustomerinformationInteger = 0;

    //查询出的员工信息匹配数据
    volatile Integer selectStaffInteger = 0;

    //查询出的表单信息匹配数据
    volatile Integer selectAccountFormMetaInteger = 0;

    //查询出的名单信息匹配数据
    volatile Integer selectListInteger = 0;

    @ResponseBody
    @RequestMapping(value = "/importData")
    @ApiOperation(value = "账号自定义导出信息")
    @SystemLog(description = "账号自定义导出信息")
    public Result<Map<String, Object>> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                                   @RequestParam String customerinformations, @RequestParam String listType,
                                                   @RequestParam(required = false) String listId,
                                                   @RequestParam String staffs, @RequestParam String accountFormMetas,
                                                   HttpServletRequest request, HttpSession session, HttpServletResponse response) throws IOException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        //每次进入初始化统计数据
        accountIndex = 0;
        listIndex = 0;
        selectCustomerinformationInteger = 0;
        selectStaffInteger = 0;
        selectAccountFormMetaInteger = 0;
        selectListInteger = 0;
        /*1.读Excel, 检查是否存在
          2.数据处理（判定这个名单是否含高级校验），如果含高级校验组装扩展表的数据
          2.批量插入
         */
        if (file == null) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("未能读取到文件");
        }
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("请上传xlsx，xls文件");
        }
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (!StringUtils.equalsAnyIgnoreCase(suffix, ".xlsx") && !StringUtils.equalsAnyIgnoreCase(suffix, ".xls")) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("请上传xlsx，xls文件");
        }
        String contentType = file.getContentType();
        // 导入文件地址
        String filePath = ReadExcelUtil.FilePath(file, request);
        // excel读取方法
        AccountExportController.ExcelListener excelListener = new AccountExportController.ExcelListener();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("文件读取异常！");
        }
        List<ExcelRow> excelRows = excelListener.getDatas();

        if (excelRows != null && excelRows.size() > 0) {
            Dict dict = dictService.findByType("customerInformationExtend");
            // 批量处理
            Vector<List<String>> rows = new Vector<>(excelRows.size()+1);
            // 校验excel头部
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("没有头列,导入模板错误！");
            }
            String[] heand = excelHead.getColums();
            List<String> titles = new ArrayList<>();
            //是否包含账户
            Boolean containAccount = false;
            if (StrUtil.isNotBlank(listId)) {
                boolean isContains = Arrays.asList(heand).contains("录入名单详细id");
                if (!isContains) {
                    return new ResultUtil<Map<String, Object>>().setErrorMsg("查询校验名单信息，excel中必须包含录入名单详细id");
                }
            }
            for (int index = 0; index < heand.length; index++) {
                String title = heand[index];
                if (StrUtil.isNotBlank(title)) {
                    titles.add(title);
                    if ("账户".equals(title.trim())) {
                        containAccount = true;
                        accountIndex = index;
                    }
                    if ("录入名单详细id".equals(title.trim())) {
                        listIndex = index;
                    }
                }
            }
            //获取原始列的数量
            Integer originalInteger = titles.size();
            if (!containAccount) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("表格信息中必须包含账户");
            }
            //拼接消息头

            //拼接客户信息
            Integer customerinformationLength = 0;
            if (StrUtil.isNotBlank(customerinformations)) {
                String[] customerinformationFileds = customerinformations.split(",");
                customerinformationLength = customerinformationFileds.length;
                for (String filed : customerinformationFileds) {
                    String title = getCustomerInformationTitleByFiled(filed);
                    titles.add(title);
                }
            }
            //拼接员工信息
            Integer staffLength = 0;
            if (StrUtil.isNotBlank(staffs)) {
                String[] staffFileds = staffs.split(",");
                staffLength = staffFileds.length;
                for (String filed : staffFileds) {
                    String title = getStaffTitleByFiled(filed);
                    titles.add(title);
                }
            }
            //拼接检验名单信息
            Integer listInteger = 0;
            if (StrUtil.isNotBlank(listId)) {
                if (listType.equals("whitelist")) {
                    WhiteList whitelist = whiteListService.get(listId);
                    if (null != whitelist) {
                        if (whitelist.getListType() == 1) {
                            listInteger = 1;
                            titles.add("检验名单-openid");
                        }
                        if (whitelist.getListType() == 2) {
                            listInteger = 1;
                            titles.add("检验名单-手机号");
                        }
                        if (whitelist.getListType() == 3) {
                            listInteger = 1;
                            titles.add("检验名单-小核心账户");
                        }
                        if (whitelist.getListType() == 0) {
                            String validateFields = whitelist.getValidateFields();
                            if (StrUtil.isNotBlank(validateFields)) {
                                String[] validateFieldes = validateFields.split(",");
                                for (String validateField : validateFieldes) {
                                    if (StrUtil.isNotBlank(validateField)) {
                                        listInteger = listInteger + 1;
                                        AccountFormMeta accountFormMeta = accountFormMetaService.get(validateField);
                                        if (null != accountFormMeta) {
                                            titles.add("检验名单-" + accountFormMeta.getTitle());
                                        }
                                    }
                                }
                            }
                        }
                        if (whitelist.getIsTimes().intValue() == 1) {
                            titles.add("检验名单-资格次数");
                        }
                    }
                }
                if (listType.equals("limitlist")) {
                    LimitList limitlist = limitListService.get(listId);
                    if (null != limitlist) {
                        if (limitlist.getListType() == 1) {
                            listInteger = 1;
                            titles.add("检验名单-openid");
                        }
                        if (limitlist.getListType() == 2) {
                            listInteger = 1;
                            titles.add("检验名单-手机号");
                        }
                        if (limitlist.getListType() == 3) {
                            listInteger = 1;
                            titles.add("检验名单-小核心账户");
                        }
                        if (limitlist.getListType() == 0) {
                            String validateFields = limitlist.getValidateFields();
                            if (StrUtil.isNotBlank(validateFields)) {
                                String[] validateFieldes = validateFields.split(",");
                                for (String validateField : validateFieldes) {
                                    if (StrUtil.isNotBlank(validateField)) {
                                        listInteger = listInteger + 1;
                                        AccountFormMeta accountFormMeta = accountFormMetaService.get(validateField);
                                        if (null != accountFormMeta) {
                                            titles.add("检验名单-" + accountFormMeta.getTitle());
                                        }
                                    }
                                }
                            }
                        }
                        if (limitlist.getIsTimes().intValue() == 1) {
                            titles.add("检验名单-资格次数");
                        }
                    }
                }
                if (listType.equals("achievelist")) {
                    AchieveList achievelist = achieveListService.get(listId);
                    if (null != achievelist) {
                        if (achievelist.getListType() == 1) {
                            listInteger = 1;
                            titles.add("检验名单-openid");
                        }
                        if (achievelist.getListType() == 2) {
                            listInteger = 1;
                            titles.add("检验名单-手机号");
                        }
                        if (achievelist.getListType() == 3) {
                            listInteger = 1;
                            titles.add("检验名单-小核心账户");
                        }
                        if (achievelist.getListType() == 0) {
                            String validateFields = achievelist.getValidateFields();
                            if (StrUtil.isNotBlank(validateFields)) {
                                String[] validateFieldes = validateFields.split(",");
                                for (String validateField : validateFieldes) {
                                    if (StrUtil.isNotBlank(validateField)) {
                                        listInteger = listInteger + 1;
                                        AccountFormMeta accountFormMeta = accountFormMetaService.get(validateField);
                                        if (null != accountFormMeta) {
                                            titles.add("检验名单-" + accountFormMeta.getTitle());
                                        }
                                    }
                                }
                            }
                        }
                        if (achievelist.getIsTimes().intValue() == 1) {
                            titles.add("检验名单-资格次数");
                        }
                    }
                }
            }
            List<AccountFormMeta> selectAccountFormMetas = new ArrayList<>();
            //拼接表单信息
            Integer accountFormMetaLength = 0;
            if (StrUtil.isNotBlank(accountFormMetas)) {
                String[] accountFormMetaIds = accountFormMetas.split(",");
                accountFormMetaLength = accountFormMetaIds.length;
                for (String accountFormMetaId : accountFormMetaIds) {
                    AccountFormMeta accountFormMeta = accountFormMetaService.get(accountFormMetaId);
                    if (null != accountFormMeta) {
                        String title = "表单信息-" + accountFormMeta.getTitle();
                        titles.add(title);
                        selectAccountFormMetas.add(accountFormMeta);
                    }
                }
            }
            excelRows.remove(0);
            rows.add(titles);
            ThreadPoolExecutor pool = ThreadPoolUtil.createPool(ThreadPoolType.handleAccount);
            int num = 300;
            Map<String, List<ExcelRow>> mapExcelRow = new HashMap<>();
            if (excelRows.size() < num) {
                mapExcelRow.put("1", excelRows);
            } else {
                int times = excelRows.size() / num;
                for (int i = 0; i < times; i++) {
                    List<ExcelRow> temp = excelRows.subList(i * num, (i + 1) * num);
                    mapExcelRow.put(i + 1 + "", temp);
                }
                List<ExcelRow> temp1 = excelRows.subList(times * num, excelRows.size());
                if(CollectionUtil.isNotEmpty(temp1)){
                    mapExcelRow.put(times + 1 + "", temp1);
                }

            }
            Map<String ,Vector<List<String>>> allMap = new HashMap<>();
            Set<String> keySet = mapExcelRow.keySet();
            for (String key : keySet) {
                pool.execute(() -> {
                    List<ExcelRow> mapexcelRows = mapExcelRow.get(key);
                    //所有的小核心账户信息
                    List<Account> allAccounts = new ArrayList<>();
                    //所有的活动平台的accountid
                    List<String> allAccountIds = new ArrayList<>();
                    //首先查出所有账户信息
                    for (ExcelRow temp : mapexcelRows) {
                        //获取每行excel原始上传数据
                        List<String> originalList = getoriginalList(originalInteger, temp);
                        //获取活动平台accountId
                        String accountId = originalList.get(accountIndex);
                        if (StrUtil.isNotBlank(accountId)) {
                            allAccountIds.add(accountId);
                        }
                    }
                    Integer sonnum = 300;
                    List<ActAccount> allActAccounts = actAccountService.findBatchByfindByActAccountIds(allAccountIds, sonnum);
                    //所有的活动平台的coreAccountid
                    List<String> allCoreAccountIds = new ArrayList<>();
                    if (CollectionUtil.isNotEmpty(allActAccounts)) {
                        for (ActAccount actAccount : allActAccounts) {
                            allCoreAccountIds.add(actAccount.getCoreAccountId());
                        }
                    }
                    if (CollectionUtil.isNotEmpty(allCoreAccountIds)) {
                        allAccounts = accountService.findBatchByfindByIds(allCoreAccountIds, sonnum);
                    }
                    List<CustomerInformation> allCustomerInformations = new ArrayList<>();
                    //查询是否需要获取全部客户信息
                    if (StrUtil.isNotBlank(customerinformations)) {
                        if (CollectionUtil.isNotEmpty(allAccounts)) {
                            List<String> identifiers = new ArrayList<>();
                            for (Account account : allAccounts) {
                                identifiers.add(account.getIdentifier());
                            }
                            if (CollectionUtil.isNotEmpty(identifiers)) {
                                allCustomerInformations = customerInformationService.findBatchByfindByIdentifiers(identifiers, sonnum);
                            }
                        }
                    }
                    //查询是否需要获取全部客户信息
                    List<Staff> allStaffs = new ArrayList<>();
                    if (StrUtil.isNotBlank(staffs)) {
                        List<String> accountIds = new ArrayList<>();
                        for (Account account : allAccounts) {
                            accountIds.add(account.getId());
                        }
                        allStaffs = staffService.findBatchfindByAccountIds(accountIds, sonnum);
                    }
                    Vector<List<String>> handlerows = new Vector<>(mapexcelRows.size());
                    //正式处理excel数据
                    for (ExcelRow temp : mapexcelRows) {

                        //每行的总数据
                        List<String> handlelist = new ArrayList<>();
                        //获取每行excel原始上传数据
                        List<String> originalList = getoriginalList(originalInteger, temp);
                        handlelist.addAll(originalList);
                        //获取客户信息数据
                        //获取accountId
                        String accountId = originalList.get(accountIndex);
                        ActAccount actAccount = new ActAccount();
                        if (StrUtil.isNotBlank(accountId)) {
                            //                    actAccount = actAccountService.findByActAccountId(accountId);
                            if (CollectionUtil.isNotEmpty(allActAccounts)) {
                                List<ActAccount> filters = allActAccounts.stream().filter(item -> item.getActAccountId().equals(accountId)).collect(Collectors.toList());
                                if (CollectionUtil.isNotEmpty(filters)) {
                                    actAccount = filters.get(0);
                                }
                            }
                        }
                        String coreAccoutId = "";
                        if (null != actAccount) {
                            coreAccoutId = actAccount.getCoreAccountId();
                        }
                        if (StrUtil.isNotBlank(coreAccoutId)) {
                            Account account = new Account();
                            if (CollectionUtil.isNotEmpty(allAccounts)) {
                                String id = coreAccoutId;
                                List<Account> filterAccounts = allAccounts.stream().filter(item -> item.getId().equals(id)).collect(Collectors.toList());
                                if (CollectionUtil.isNotEmpty(filterAccounts)) {
                                    Account filterAccount = filterAccounts.get(0);
                                    BeanUtils.copyProperties(filterAccount, account);
                                    account = accountService.decryptAccount(account);
                                }
                            }
                            if (null != account && StrUtil.isNotBlank(account.getIdentifier())) {
                                //处理客户信息数据
                                if (StrUtil.isNotBlank(customerinformations)) {
                                    //                            CustomerInformation customerInformation = customerInformationService.findByIdentifier(account.getIdentifier());
                                    CustomerInformation customerInformation = new CustomerInformation();
                                    if (CollectionUtil.isNotEmpty(allCustomerInformations)) {
                                        String identifier = account.getIdentifier();
                                        List<CustomerInformation> filers = allCustomerInformations.stream().filter(item -> item.getIdentifier().equals(identifier)).collect(Collectors.toList());
                                        if (CollectionUtil.isNotEmpty(filers)) {
                                            customerInformation = filers.get(0);
                                        }
                                    }
                                    if (null != customerInformation  && StrUtil.isNotBlank(customerInformation.getIdentifier()) ) {
                                        synchronized(this){
                                            // 查询出客户信息数据加1
                                            selectCustomerinformationInteger = selectCustomerinformationInteger + 1;
                                        }
                                        String[] customerinformationFileds = customerinformations.split(",");
                                        for (String filed : customerinformationFileds) {
                                            Object object = ReflectUtil.getFieldValue(customerInformation, filed);
                                            if (null != object) {
                                                handlelist.add(object.toString().trim());
                                            } else {
                                                List<CustomerInformationExtend> customerInformationExtends = customerInformation.getCustomerInformationExtends();
                                                if (null != dict) {
                                                    List<DictData> dictDatas = dictDataService.getByValueAndDictId(filed, dict.getId());
                                                    String title = "";
                                                    if (CollectionUtil.isNotEmpty(dictDatas)) {
                                                        title = dictDatas.get(0).getTitle();
                                                    }
                                                    handlelist.add(title);
                                                } else {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                    } else {
                                        String[] customerinformationFileds = customerinformations.split(",");
                                        //如果未查出数据需要补空字符串
                                        for (int i = 0; i < customerinformationFileds.length; i++) {
                                            handlelist.add("");
                                        }
                                    }
                                }
                                //处理员工信息数据
                                if (StrUtil.isNotBlank(staffs) ) {
                                    //                            Staff staff = staffService.findByAccountId(account.getId());
                                    Staff staff = new Staff();
                                    if (CollectionUtil.isNotEmpty(allStaffs)) {
                                        String id = account.getId();
                                        List<Staff> filters = allStaffs.stream().filter(item -> item.getAccountId().equals(id)).collect(Collectors.toList());
                                        if (CollectionUtil.isNotEmpty(filters)) {
                                            staff = filters.get(0);
                                        }
                                    }
                                    if (null != staff  && null != staff.getCreateTime()) {
                                        synchronized(this){
                                            // 查询出员工信息数据加1
                                            selectStaffInteger = selectStaffInteger + 1;
                                        }
                                        String[] staffFileds = staffs.split(",");
                                        for (String filed : staffFileds) {
                                            if (!filed.equals("title")) {
                                                Object object = ReflectUtil.getFieldValue(staff, filed);
                                                if (null != object) {
                                                    if (filed.equals("name") || filed.equals("phone")) {
                                                        handlelist.add(AESUtil.decrypt(object.toString().trim()));
                                                    } else if (filed.equals("deptNo")) {
                                                        if (StrUtil.isNotBlank(staff.getDeptNo())) {
                                                            Department department = departmentService.get(staff.getDeptNo());
                                                            if (null != department) {
                                                                handlelist.add(department.getDeptCode());
                                                            } else {
                                                                handlelist.add("");
                                                            }
                                                        } else {
                                                            handlelist.add("");
                                                        }
                                                    } else {
                                                        handlelist.add(object.toString().trim());
                                                    }
                                                } else {
                                                    handlelist.add("");
                                                }
                                            } else {
                                                //获取机构名称
                                                if (StrUtil.isNotBlank(staff.getDeptNo())) {
                                                    Department department = departmentService.get(staff.getDeptNo());
                                                    if (null != department) {
                                                        handlelist.add(department.getTitle());
                                                    } else {
                                                        handlelist.add("");
                                                    }
                                                } else {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                    } else {
                                        String[] staffFileds = staffs.split(",");
                                        //如果未查出数据需要补空字符串
                                        for (int i = 0; i < staffFileds.length; i++) {
                                            handlelist.add("");
                                        }
                                    }
                                }
                                //处理校验名单信息
                                if (StrUtil.isNotBlank(listId)) {
                                    String recordId = originalList.get(listIndex);
                                    if (listType.equals("whitelist")) {
                                        WhiteList whitelist = whiteListService.get(listId);
                                        WhiteListRecord whiteListRecord = whiteListRecordService.findByListIdAndId(listId, recordId);
                                        if (null != whitelist && whitelist.getListType() == 1) {
                                            if (null != whiteListRecord && StrUtil.isNotBlank(whiteListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(whiteListRecord.getIdentifier());
                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(whiteListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != whitelist && whitelist.getListType() == 2) {
                                            if (null != whiteListRecord && StrUtil.isNotBlank(whiteListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(AESUtil.decrypt(whiteListRecord.getIdentifier()));
                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(whiteListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != whitelist && whitelist.getListType() == 3) {
                                            if (null != whiteListRecord && StrUtil.isNotBlank(whiteListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(whiteListRecord.getIdentifier());
                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(whiteListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != whitelist && whitelist.getListType() == 0) {
                                            if (null != whiteListRecord) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                            }
                                            List<WhiteListExtendRecord> whiteListExtendRecords = whiteListExtendRecordService.findByListIdAndRecordId(listId, recordId);
                                            String validateFields = whitelist.getValidateFields();
                                            if (StrUtil.isNotBlank(validateFields)) {
                                                String[] validateFieldes = validateFields.split(",");
                                                for (String validateField : validateFieldes) {
                                                    if (StrUtil.isNotBlank(validateField)) {
                                                        if (CollectionUtil.isNotEmpty(whiteListExtendRecords)) {
                                                            List<WhiteListExtendRecord> filters = whiteListExtendRecords.stream().filter(item -> item.getFormMetaId().equals(validateField)).collect(Collectors.toList());
                                                            if (CollectionUtil.isNotEmpty(filters)) {
                                                                handlelist.add(AESUtil.decrypt(filters.get(0).getRecord()));
                                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                                    handlelist.add(whiteListRecord.getTimes() + "");
                                                                }
                                                            } else {
                                                                handlelist.add("");
                                                                if (whitelist.getIsTimes().intValue() == 1) {
                                                                    handlelist.add("");
                                                                }
                                                            }
                                                        } else {
                                                            handlelist.add("");
                                                            if (whitelist.getIsTimes().intValue() == 1) {
                                                                handlelist.add("");
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (listType.equals("limitlist")) {
                                        LimitList limitlist = limitListService.get(listId);
                                        LimitListRecord limitListRecord = limitListRecordService.findByListIdAndId(listId, recordId);
                                        if (null != limitlist && limitlist.getListType() == 1) {
                                            if (null != limitListRecord && StrUtil.isNotBlank(limitListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(limitListRecord.getIdentifier());
                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(limitListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != limitlist && limitlist.getListType() == 2) {
                                            if (null != limitListRecord && StrUtil.isNotBlank(limitListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(AESUtil.decrypt(limitListRecord.getIdentifier()));
                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(limitListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != limitlist && limitlist.getListType() == 3) {
                                            if (null != limitListRecord && StrUtil.isNotBlank(limitListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(limitListRecord.getIdentifier());
                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(limitListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != limitlist && limitlist.getListType() == 0) {
                                            if (null != limitListRecord && StrUtil.isNotBlank(limitListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                            }
                                            List<LimitListExtendRecord> limitListExtendRecords = limitListExtendRecordService.findByListIdAndRecordId(listId, recordId);
                                            String validateFields = limitlist.getValidateFields();
                                            if (StrUtil.isNotBlank(validateFields)) {
                                                String[] validateFieldes = validateFields.split(",");
                                                for (String validateField : validateFieldes) {
                                                    if (StrUtil.isNotBlank(validateField)) {
                                                        if (CollectionUtil.isNotEmpty(limitListExtendRecords)) {
                                                            List<LimitListExtendRecord> filters = limitListExtendRecords.stream().filter(item -> item.getFormMetaId().equals(validateField)).collect(Collectors.toList());
                                                            if (CollectionUtil.isNotEmpty(filters)) {
                                                                handlelist.add(AESUtil.decrypt(filters.get(0).getRecord()));
                                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                                    handlelist.add(limitListRecord.getTimes() + "");
                                                                }
                                                            } else {
                                                                handlelist.add("");
                                                                if (limitlist.getIsTimes().intValue() == 1) {
                                                                    handlelist.add("");
                                                                }
                                                            }
                                                        } else {
                                                            handlelist.add("");
                                                            if (limitlist.getIsTimes().intValue() == 1) {
                                                                handlelist.add("");
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (listType.equals("achievelist")) {
                                        AchieveList achievelist = achieveListService.get(listId);
                                        AchieveListRecord achieveListRecord = achieveListRecordService.findByListIdAndId(listId, recordId);
                                        if (null != achievelist && achievelist.getListType() == 1) {
                                            if (null != achieveListRecord && StrUtil.isNotBlank(achieveListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(achieveListRecord.getIdentifier());
                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(achieveListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != achievelist && achievelist.getListType() == 2) {
                                            if (null != achieveListRecord && StrUtil.isNotBlank(achieveListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(AESUtil.decrypt(achieveListRecord.getIdentifier()));
                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(achieveListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != achievelist && achievelist.getListType() == 3) {
                                            if (null != achieveListRecord && StrUtil.isNotBlank(achieveListRecord.getIdentifier())) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                                handlelist.add(achieveListRecord.getIdentifier());
                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add(achieveListRecord.getTimes() + "");
                                                }
                                            } else {
                                                handlelist.add("");
                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                    handlelist.add("");
                                                }
                                            }
                                        }
                                        if (null != achievelist && achievelist.getListType() == 0) {
                                            if (null != achieveListRecord) {
                                                synchronized(this){
                                                    selectListInteger = selectListInteger + 1;
                                                }
                                            }
                                            List<AchieveListExtendRecord> achieveListExtendRecords = achieveListExtendRecordService.findByListIdAndRecordId(listId, recordId);
                                            String validateFields = achievelist.getValidateFields();
                                            if (StrUtil.isNotBlank(validateFields)) {
                                                String[] validateFieldes = validateFields.split(",");
                                                for (String validateField : validateFieldes) {
                                                    if (StrUtil.isNotBlank(validateField)) {
                                                        if (CollectionUtil.isNotEmpty(achieveListExtendRecords)) {
                                                            List<AchieveListExtendRecord> filters = achieveListExtendRecords.stream().filter(item -> item.getFormMetaId().equals(validateField)).collect(Collectors.toList());
                                                            if (CollectionUtil.isNotEmpty(filters)) {
                                                                handlelist.add(AESUtil.decrypt(filters.get(0).getRecord()));
                                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                                    handlelist.add(achieveListRecord.getTimes() + "");
                                                                }
                                                            } else {
                                                                handlelist.add("");
                                                                if (achievelist.getIsTimes().intValue() == 1) {
                                                                    handlelist.add("");
                                                                }
                                                            }
                                                        } else {
                                                            handlelist.add("");
                                                            if (achievelist.getIsTimes().intValue() == 1) {
                                                                handlelist.add("");
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }
                                //处理表单信息数据
                                if (StrUtil.isNotBlank(accountFormMetas)) {
                                    Account copyAccount = new Account();
                                    BeanUtils.copyProperties(account, copyAccount);
                                    synchronized(this){
                                        //表单数据加1
                                        selectAccountFormMetaInteger = selectAccountFormMetaInteger + 1;
                                    }
                                    String[] accountFormMetaIds = accountFormMetas.split(",");
                                    for (String accountFormMetaId : accountFormMetaIds) {
                                        List<AccountFormMeta> filterAccountFormMetas = selectAccountFormMetas.stream().filter(item -> item.getId().equals(accountFormMetaId)).collect(Collectors.toList());
                                        AccountFormMeta accountFormMeta = filterAccountFormMetas.get(0);
                                        //标准主键
                                        if (accountFormMeta.getIsStandard()) {
                                            Object object = ReflectUtil.getFieldValue(copyAccount, accountFormMeta.getMetaType());
                                            if (null != object) {
                                                handlelist.add(object.toString().trim());
                                            } else {
                                                handlelist.add("");
                                            }
                                        }
                                        //非标准主键
                                        if (!accountFormMeta.getIsStandard()) {
                                            List<AccountFormField> accountFormFields = copyAccount.getAccountFormFields();
                                            if (CollectionUtil.isNotEmpty(accountFormFields)) {
                                                List<AccountFormField> selectAccountFormFields = accountFormFields.stream()
                                                        .filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                                                if (CollectionUtil.isNotEmpty(selectAccountFormFields)) {
                                                    handlelist.add(selectAccountFormFields.get(0).getFieldData());
                                                } else {
                                                    handlelist.add("");
                                                }
                                            } else {
                                                handlelist.add("");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        handlerows.add(handlelist);
                    }
                    allMap.put(key,handlerows);
                });
            }
            pool.shutdown();
            while (!pool.isTerminated()) {
                try {
                    pool.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Set<String> keyAll = allMap.keySet();
            Set<Integer> ListKey = new TreeSet<>();
            for(String key: keyAll){
                ListKey.add(Integer.parseInt(key));
            }
            for(Integer key: ListKey){
                rows.addAll(allMap.get(key+""));
            }
            ApplicationHome home = new ApplicationHome(getClass());
            File jarFile = home.getSource();
            String path = jarFile.getParentFile().getPath();
            String rootPath = path + File.separator;
            File dir = new File(rootPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String excelFileName = File.separator + "static" + File.separator + "ytdexports" + File.separator +
                    UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
            String fullFileName = rootPath + excelFileName;
//            BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
//
//            // 一次性写出内容，使用默认样式
//            writer.write(rows);
//            // 关闭writer，释放内存
//            writer.close();

            List<String> head = rows.remove(0);
            List<List<String>> headList = CollUtil.newArrayList();
            for (String s : head) {
                List<String> headLength = CollUtil.newArrayList();
                headLength.add(s);
                headList.add(headLength);
            }
            EasyExcel.write(fullFileName)
                    .inMemory(true) // 注意，此项配置不能少
                    .registerWriteHandler(new WaterMarkHandler(WaterExcelUtil.waterRemark()))
                    .head(headList)
                    .sheet("sheet1")
                    .doWrite(rows);


            File filee = new File(fullFileName);
            //   ServletUtil.write(response, filee);
            System.out.println(fullFileName);
            Map<String, Object> map = new HashMap<>();
            Result<Object> result = fileService.upload(filee, contentType);
            if (result.isSuccess()) {
                System.out.println(result.getResult().toString());
                map.put("url", result.getResult().toString());

            } else {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("数据导出异常");
            }
            rows.clear();
            map.put("selectCustomerinformationInteger", selectCustomerinformationInteger);
            map.put("selectStaffInteger", selectStaffInteger);
            map.put("selectAccountFormMetaInteger", selectAccountFormMetaInteger);
            map.put("selectListInteger", selectListInteger);
            filee.delete();
            return new ResultUtil<Map<String, Object>>().setData(map);

        }
        return new ResultUtil<Map<String, Object>>().setSuccessMsg("导入成功");
    }

    public String getCustomerInformationTitleByFiled(String filed) {
        CustomerInformation customerInformation = new CustomerInformation();
        String title = "";
        if ("phone".equals(filed)) {
            title = "客户信息-手机号码";
            return title;
        }
        if ("customerNo".equals(filed)) {
            title = "客户信息-客户号";
            return title;
        }
        if ("name".equals(filed)) {
            title = "客户信息-姓名";
            return title;
        }
        if ("idcardNo".equals(filed)) {
            title = "客户信息-身份证号码";
            return title;
        }
        if ("bankcardNo".equals(filed)) {
            title = "客户信息-银行卡号";
            return title;
        }
        if ("birthday".equals(filed)) {
            title = "客户信息-出生日期";
            return title;
        }
        if ("email".equals(filed)) {
            title = "客户信息-邮箱";
            return title;
        }
        if ("address".equals(filed)) {
            title = "客户信息-地址";
            return title;
        }
        if ("bankBranchNo".equals(filed)) {
            title = "客户信息-分行编码";
            return title;
        }
        if ("bankBranchName".equals(filed)) {
            title = "客户信息-分行名称";
            return title;
        }
        if ("institutionalCode".equals(filed)) {
            title = "客户信息-机构编号";
            return title;
        }
        if ("institutionalName".equals(filed)) {
            title = "客户信息-机构名称";
            return title;
        }
        if ("customerGroupCoding".equals(filed)) {
            title = "客户信息-客群编码";
            return title;
        }
        Dict dict = dictService.findByType("customerInformationExtend");
        if (null != dict) {
            List<DictData> dictDatas = dictDataService.getByValueAndDictId(filed, dict.getId());
            if (CollectionUtil.isNotEmpty(dictDatas)) {
                title = "客户信息-" + dictDatas.get(0).getTitle();
            }
        }

        return title;
    }

    public String getStaffTitleByFiled(String filed) {
        Staff staff = new Staff();
        String title = "";
        if ("staffNo".equals(filed)) {
            title = "员工信息-员工号";
            return title;
        }
        if ("name".equals(filed)) {
            title = "员工信息-姓名";
            return title;
        }
        if ("phone".equals(filed)) {
            title = "员工信息-手机号";
            return title;
        }
        if ("deptNo".equals(filed)) {
            title = "员工信息-机构编码";
            return title;
        }
        if ("title".equals(filed)) {
            title = "员工信息-机构名称";
            return title;
        }
        return title;
    }

    public List<String> getoriginalList(Integer originalInteger, ExcelRow temp) {
        List<String> originalList = new ArrayList<>();
        for (Integer index = 0; index < originalInteger; index++) {
            Object object = ReflectUtil.getFieldValue(temp, "colum" + index);
            if (null != object) {
                originalList.add(object.toString().trim());
            } else {
                originalList.add("");
            }
        }
        return originalList;
    }
}
