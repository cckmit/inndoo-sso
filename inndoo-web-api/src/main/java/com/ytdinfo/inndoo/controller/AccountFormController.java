package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.constant.SettingConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffService;
import com.ytdinfo.inndoo.vo.AccountAttributeVo;
import com.ytdinfo.inndoo.vo.AccountFormVo;
import com.ytdinfo.util.StringUtils;
import com.ytdinfo.inndoo.vo.SpecialAccountForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.Collator;
import java.text.DateFormat;
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
@Api(description = "会员注册页面主信息管理接口")
@RequestMapping("/accountform")

@APIModifier(APIModifierType.PUBLIC)
public class AccountFormController {

    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private IStaffService iStaffService;
    @Autowired
    private StaffService staffService;
    @XxlConf("core.inndoo.urlprefix")
    private String urlprefix;


    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id删除数据")
    @SystemLog(description = "通过id删除数据")
    public Result<Object> deleteById(@PathVariable String id) {
        accountFormService.delete(id);
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }


    @RequestMapping(value = "/querySpecial/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<SpecialAccountForm> query(@PathVariable String id){
        AccountForm entity = accountFormService.get(id);
        if(entity == null ) {
            return new ResultUtil<SpecialAccountForm>().setData(null);
        }
        if(null == entity.getType()){
            Byte type = 0;
            entity.setType(type);
        }
        SpecialAccountForm specialAccountForm = new SpecialAccountForm();
        BeanUtil.copyProperties(entity,specialAccountForm);
        specialAccountForm.setId("");
        List<AccountFormResource> accountFormResourceList = specialAccountForm.getAccountFormResources();
        for(AccountFormResource temp : accountFormResourceList){
            temp.setId("");
        }
        List<AccountFormMeta> accountFormMetaList =  specialAccountForm.getAccountFormMetas();
        for(AccountFormMeta temp : accountFormMetaList){
            temp.setId("");
        }
        return new ResultUtil<SpecialAccountForm>().setData(specialAccountForm);
    }

    @RequestMapping(value = "/saveSpecial", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加")
    @SystemLog(description = "添加")
    public Result<AccountForm> add(@RequestBody AccountForm accountForm, HttpServletRequest request){

        Integer version = 1;
        String name = accountForm.getName();
        String newName =  accountForm.getName();
        long num = accountFormService.countByName(name);
        while (num > 0   && version <1000){
            newName = version + "-"+ name;
            if(StrUtil.length(newName) >50){
                newName = StrUtil.subWithLength(newName ,newName.length()-50, 50);
            }
            version++;
            num = accountFormService.countByName(newName);
        }
        accountForm.setName(newName);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        if(StrUtil.isNotBlank(accountForm.getViewStartDate())) {
            accountForm.setStartDate(DateUtils.parseDate(accountForm.getViewStartDate(),df));
        }
        if(StrUtil.isNotBlank(accountForm.getViewEndDate())) {
            accountForm.setEndDate(DateUtils.parseDate(accountForm.getViewEndDate(),df));
        }
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        //全部设置成开启
        accountForm.setEnableCaptcha(true);
        List<AccountFormMeta> accountFormMetas = accountForm.getAccountFormMetas();
        if(null == accountFormMetas || accountFormMetas.size() < 1){
            return new ResultUtil<AccountForm>().setErrorMsg("请加入输入框组件");
        }
        //去重
        List<AccountFormMeta> uniqueAccountFormMeta = accountFormMetas.parallelStream().distinct()
                .filter(distinctByKey(b -> b.getTitle()))
                .collect(toList());
        if(accountFormMetas.size() != uniqueAccountFormMeta.size()) {
            return new ResultUtil<AccountForm>().setErrorMsg("标题不能重复");
        }
        if(accountForm.getFormType() == 0){
        }
        AccountForm accountForm1 = accountFormService.save(accountForm);
        return new ResultUtil<AccountForm>().setData(accountForm1);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }



    @RequestMapping(value = "/query/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<Map<String, Object>> query(@PathVariable String id, @RequestParam(required = true) String actAccountId) {
//        String actAccountId = request.getParameter("actAccountId");
//        String coreAccountId = request.getParameter("coreAccountId");
        Map<String, Object> map = new HashMap<>();
        Account account = new Account();
        if (StrUtil.isNotBlank(actAccountId)) {
            actAccountId = AESUtil.decrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD);
        }
        String coreAccountId = "";
        ActAccount actAccount = new ActAccount();
        if (StrUtil.isNotBlank(actAccountId)) {
            actAccount = actAccountService.findByActAccountId(actAccountId);
        }

        if (null != actAccount) {
            coreAccountId = actAccount.getCoreAccountId();
        }
        if (StrUtil.isNotBlank(coreAccountId)) {
            account = accountService.get(coreAccountId);
        }
        AccountForm entity = accountFormService.get(id);
        if (null == entity) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("不存在该注册页面");
        }
        if(null == entity.getType()){
            Byte type = 0;
            entity.setType(type);
        }
        //判断页面状态
        entity = accountFormService.setActStatus(entity);
        if (entity.getActStatus() == 0) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("该注册页面未发布");
        }
        if (entity.getActStatus() == -1) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("该注册页面活动未开始");
        }
        if (entity.getActStatus() == 2) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("该注册页面活动已过期");
        }
        if (entity.getActStatus() == 3) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("该注册页面活动已下架");
        }
        map.put("urlprefix", urlprefix);

        if (null != account) {
            //获取account的拓展属性值列表
            List<AccountFormField> accountFormFields = account.getAccountFormFields();
            // 获取动态表单控件配置信息
            List<AccountFormMeta> accountFormMetas = entity.getAccountFormMetas();
            List<AccountFormMeta> newAccountFormMetas = new ArrayList<>();
            if (null != accountFormMetas && accountFormMetas.size() > 0) {
                for (AccountFormMeta accountFormMeta : accountFormMetas) {
                    if (accountFormMeta.getIsStandard()) {
                        // System.out.println("IsStandard:" + accountFormMeta.getMetaType());
                        // System.out.println("accountFormMetaID:" + accountFormMeta.getId());
//                        if (accountFormMeta.getMetaType().equals("phone")) {
//                            newAccountFormMetas.add(accountFormMeta);
//                        } else {
                        Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                        if (null != object) {
                            String fieldValue = object.toString();
                            if (StrUtil.isBlank(fieldValue)) {
                                //System.out.println("isStandard加入的:"+accountFormMeta.getTitle() + " metaId:"+accountFormMeta.getId());
                                newAccountFormMetas.add(accountFormMeta);
                            } else {
                                //不做处理
                                //System.out.println("isStandard未加入的:"+accountFormMeta.getTitle()  + " metaId:"+accountFormMeta.getId());

                            }
                        } else {
                            //System.out.println("isStandard加入的:"+accountFormMeta.getTitle()  + " metaId:"+accountFormMeta.getId());
                            newAccountFormMetas.add(accountFormMeta);
                        }
                        //  }
                    } else {
                        if (null != accountFormFields && accountFormFields.size() > 0) {
                            // System.out.println("NoStandard:"+accountFormMeta.getTitle());
                            List<AccountFormField> selectaccountFormFields = accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                            if (null != selectaccountFormFields && selectaccountFormFields.size() > 0) {
                                //不做处理
                                // System.out.println("NoStandard未加入的:"+accountFormMeta.getTitle()  + " metaId:"+accountFormMeta.getId());
                            } else {
                                // System.out.println("NoStandard加入的:"+accountFormMeta.getTitle()  + " metaId:"+accountFormMeta.getId());
                                newAccountFormMetas.add(accountFormMeta);
                            }
                        } else {
                            //System.out.println("NoStandard加入的:"+accountFormMeta.getTitle()  + " metaId:"+accountFormMeta.getId());
                            newAccountFormMetas.add(accountFormMeta);
                        }
                    }

                }
                Optional<AccountFormMeta> accountFormMetaOptional = newAccountFormMetas.stream().filter(item -> item.getMetaType().equals("phone")).findFirst();
                if (accountFormMetaOptional.isPresent()) {
                    // 存在
                } else {
                    // 不存在
                    newAccountFormMetas.removeIf(s -> s.getTitle().contains("验证码"));
                    newAccountFormMetas.removeIf(s -> s.getTitle().contains("图形验证"));
                }
                entity.setAccountFormMetas(newAccountFormMetas);
                if(newAccountFormMetas.size()<=2){
                    //没有可需要提交的数据
                    map.put("needSubmit", false);
                    //如果是员工注册表单  账户信息已存在但是没绑定员工号的情况，这时在直接绑定员工
                    if(entity.getFormType() == 0 && account.getIsStaff() == 0){
                        String checkStaff = entity.getCheckStaff();
                        if (StrUtil.isNotBlank(checkStaff)) {
                            String[] checkStaffs = checkStaff.split(",");
                            Map<String,Object> selectMap = new HashMap<>();
                            if(Arrays.asList(checkStaffs).contains("deptNo")){
                                List<AccountFormMeta> filters = accountFormMetas.stream().filter(item -> item.getMetaType().equals("deptNo")).collect(Collectors.toList());
                                if (CollectionUtil.isEmpty(filters) ) {
                                    return new ResultUtil<Map<String, Object>>().setErrorMsg("员工注册页中机构编码输入框应存在且为必填输入框，请检查员工注册页配置");
                                }
                                selectMap.put("deptNo",account.getDeptNo());
                            }
                            if(Arrays.asList(checkStaffs).contains("staffNo")){
                                List<AccountFormMeta> filters = accountFormMetas.stream().filter(item -> item.getMetaType().equals("staffNo")).collect(Collectors.toList());
                                if (CollectionUtil.isEmpty(filters) ) {
                                    return new ResultUtil<Map<String, Object>>().setErrorMsg("员工注册页中员工号输入框应存在且为必填输入框，请检查员工注册页配置");
                                }
                                selectMap.put("staffNo",account.getStaffNo());
                            }
                            if(Arrays.asList(checkStaffs).contains("phone")){
                                List<AccountFormMeta> filters = accountFormMetas.stream().filter(item -> item.getMetaType().equals("phone")).collect(Collectors.toList());
                                if (CollectionUtil.isEmpty(filters) ) {
                                    return new ResultUtil<Map<String, Object>>().setErrorMsg("员工注册页中手机号输入框应存在且为必填输入框，请检查员工注册页配置");
                                }
                                selectMap.put("phone",AESUtil.encrypt(account.getPhone().trim()));
                            }
                            if(Arrays.asList(checkStaffs).contains("name")){
                                List<AccountFormMeta> filters = accountFormMetas.stream().filter(item -> item.getMetaType().equals("name")).collect(Collectors.toList());
                                if (CollectionUtil.isEmpty(filters)) {
                                    return new ResultUtil<Map<String, Object>>().setErrorMsg("员工注册页中姓名输入框应存在且为必填输入框，请检查员工注册页配置");
                                }
                                selectMap.put("name",AESUtil.encrypt(account.getName().trim()) );
                            }
                            Integer status = 0;//正常
                            selectMap.put("status",status);
                            selectMap.put("appid",UserContext.getAppid());
                            if(selectMap.keySet().size() <2){
                                return new ResultUtil<Map<String, Object>>().setErrorMsg("员工注册活动员工身份校验属性必须勾选，请检查该员工注册页");
                            }
                            List<Staff> staffs = iStaffService.findByMap(selectMap);
                            if(CollectionUtil.isEmpty(staffs)){
                                return new ResultUtil<Map<String, Object>>().setErrorMsg("经校验，您不是我司员工，仅限我司员工注册。");
                            } else {
                                Staff staff = staffs.get(0);
                                if(staffs.size() >1){
                                    return new ResultUtil<Map<String, Object>>().setErrorMsg("您的账户查到多条员工信息，您无法注册");
                                } else {
                                    if(StrUtil.isNotBlank(staff.getAccountId())) {
                                        if(! staff.getAccountId().equals(entity.getId())){
                                            return new ResultUtil<Map<String, Object>>().setErrorMsg("该员工已绑定账号，不能重复绑定");
                                        }
                                    }
                                }
                                staff.setAccountId(account.getId());
                                if(StrUtil.isNotBlank(account.getName())){
                                    staff.setName(AESUtil.encrypt(account.getName().trim()) );
                                }
                                if(StrUtil.isNotBlank(account.getPhone())){
                                    staff.setPhone(AESUtil.encrypt(account.getPhone()));
                                }
                                if(StrUtil.isNotBlank(account.getStaffNo())){
                                    staff.setStaffNo(account.getStaffNo());
                                }
                                if(StrUtil.isNotBlank(account.getDeptNo() )){
                                    staff.setDeptNo(account.getDeptNo());
                                }
                                staffService.update(staff);
                                Integer isStaff = 1;
                                account.setIsStaff(isStaff);
                                account.setStaffNo(staff.getStaffNo());
                                accountService.save(account);
                            }
                        }
                    }

                }else {
                    map.put("needSubmit", true);
                }
            }

        }
        if(SettingConstant.LOCAL_OSS.equals(XXLConfUtil.serviceProvider)){
            FileUtil fileUtil = SpringContextUtil.getBean(FileUtil.class);
            String privateImgUrl = fileUtil.getOssSetting().getEndpoint();
            String publicImgUrl = XXLConfUtil.uploadPublicUrl;
            if(StrUtil.isNotEmpty(publicImgUrl)){
                for (AccountFormResource accountFormResource : entity.getAccountFormResources()) {
                    String resourceData = accountFormResource.getResourceData();
                    resourceData = resourceData.replace(privateImgUrl,publicImgUrl);
                    accountFormResource.setResourceData(resourceData);
                }
            }
        }
        //强制开启图形验证码
        entity.setEnableCaptcha(Boolean.TRUE);
        map.put("account", account);
        map.put("accountForm", entity);
        //判断手机号是否允许修改
        if(StrUtil.isNotBlank(account.getPhone())){
            // 不允许修改
            map.put("updatePhone",true);
        }else {
            // 允许修改
            map.put("updatePhone",false);
        }

        return new ResultUtil<Map<String, Object>>().setData(map);
    }

    @RequestMapping(value = "/queryByName", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过姓名获取客户自定义注册")
    public Result<AccountForm> queryByName ( @RequestParam String name ) {
        AccountForm accountForm = new AccountForm();
        Map<String,Object> map = new HashMap<>();
        map.put("name",name);
        map.put("appid",UserContext.getAppid());
        Integer formType = 1;
        map.put("formType",formType);
        List<AccountForm> accountForms = accountFormService.findByMap(map);
        if (CollectionUtil.isNotEmpty(accountForms) ) {
            accountForm = accountForms.get(0);
            List<AccountFormMeta> accountFormMetas = accountFormMetaService.findListByAccountFormId(accountForm.getId());
            accountForm.setAccountFormMetas(accountFormMetas);
        }
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        return new ResultUtil<AccountForm>().setData(accountForm);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取注册表单清单")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<List<AccountFormVo>> list(@RequestParam Integer formType,@RequestParam(required = false) Byte type) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("status", CommonConstant.STATUS_APPROVED);
        map.put("appid", UserContext.getAppid());
        map.put("formType", formType);
        map.put("startEndDate", DateUtil.parse(DateUtil.today()));
        map.put("endStartDate", DateUtil.parse(DateUtil.today()));
        Boolean isIdentifierForm = false;
        map.put("isIdentifierForm", isIdentifierForm);
        if(null != type){
            map.put("type", type);
        }
        map.put("appid",UserContext.getAppid());
        List<AccountForm> formList = accountFormService.findByMap(map);

        List<AccountFormVo> voList = new ArrayList<>();
        formList.forEach(entity -> {
            AccountFormVo vo = new AccountFormVo();
            vo.setId(entity.getId());
            vo.setName(entity.getName());
            vo.setRegisterType(entity.getType());
            voList.add(vo);
        });
        Collator collator = Collator.getInstance(Locale.CHINESE);
        formList.sort((o1, o2) -> collator.compare(o1.getName(), o2.getName()));
        return new ResultUtil<List<AccountFormVo>>().setData(voList);
    }

    @RequestMapping(value = "/getDefaultAccountForm", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取默认注册表单")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<AccountFormVo> getDefaultAccountForm(@RequestParam Integer formType) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("status", CommonConstant.STATUS_APPROVED);
        map.put("appid", UserContext.getAppid());
        Boolean isDefault = true;
        map.put("isDefault", isDefault);
        map.put("formType", formType);
        map.put("startEndDate", DateUtil.parse(DateUtil.today()));
        map.put("endStartDate", DateUtil.parse(DateUtil.today()));
        Boolean isIdentifierForm = false;
        map.put("isIdentifierForm", isIdentifierForm);
        map.put("appid",UserContext.getAppid());
        List<AccountForm> formList = accountFormService.findByMap(map);
        AccountFormVo vo = new AccountFormVo();
        if (null != formList && formList.size() > 0) {
            vo.setId(formList.get(0).getId());
            vo.setName(formList.get(0).getName());
            vo.setRegisterType(formList.get(0).getType());
        } else {
            return new ResultUtil<AccountFormVo>().setErrorMsg("未查询到默认注册页，可能有以下原因1、未设置默认注册页2、默认注册页活动已过期3、默认注册页活动未开始");
        }
        return new ResultUtil<AccountFormVo>().setData(vo);
    }

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取，返回Y表示已注册，返回N表示未注册")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<String> validate(@RequestParam String formId, @RequestParam String accountId) {
        Account account = accountService.get(accountId);
        if (StrUtil.isEmpty(formId)) {
            String result = CommonConstant.RESULT_YES;
            if (account == null) {
                result = CommonConstant.RESULT_NO;
            }
            return new ResultUtil<String>().setData(result);
        }
        if (account == null) {
            String result = CommonConstant.RESULT_NO;
            return new ResultUtil<String>().setData(result);
        }
        List<AccountFormField> accountFormFields = account.getAccountFormFields();
        List<AccountFormMeta> formMetas = accountFormMetaService.findListByAccountFormId(formId);
        formMetas.removeIf(accountFormMeta -> accountFormMeta.getMetaType().equals("note") || accountFormMeta.getMetaType().equals("agreement"));
        String result = CommonConstant.RESULT_YES;
        for (AccountFormMeta meta : formMetas) {
            if (Boolean.TRUE.equals(meta.getIsRequired())) {
                if (Boolean.TRUE.equals(meta.getIsStandard())) {
                    Object fieldValue = ReflectUtil.getFieldValue(account, meta.getMetaType());
                    if (fieldValue == null || StrUtil.isEmpty(fieldValue.toString())) {
                        result = CommonConstant.RESULT_NO;
                        break;
                    }
                } else {
                    final boolean[] found = new boolean[1];
                    if(accountFormFields != null){
                        accountFormFields.forEach(accountFormField -> {
                            if (accountFormField.getMetaTitle().equals(meta.getTitle())
                                    && StrUtil.isNotEmpty(accountFormField.getFieldData())) {
                                found[0] = true;
                            }
                        });
                        if (!found[0]) {
                            result = CommonConstant.RESULT_NO;
                            break;
                        }
                    }else{
                        result = CommonConstant.RESULT_NO;
                    }
                }
            }
        }
        return new ResultUtil<String>().setData(result);
    }

//    @RequestMapping(value = "/validate", method = RequestMethod.GET)
//    @ResponseBody
//    @ApiOperation(value = "通过id获取，返回Y表示已注册，返回N表示未注册")
//    @APIModifier(APIModifierType.PRIVATE)
//    public Result<String> validate(@RequestParam String accountId) {
//        Account account = accountService.get(accountId);
//        String result = CommonConstant.RESULT_YES;
//        if(account == null){
//            result = CommonConstant.RESULT_NO;
//        }
//        return new ResultUtil<String>().setData(result);
//    }

    @RequestMapping(value = "/deptlist/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据")
    public Result<List<Department>> listAll(@PathVariable String id) {
        AccountForm entity = accountFormService.get(id);
        if (null == entity) {
            return new ResultUtil<List<Department>>().setErrorMsg("不存在该注册页面");
        }
        List<AccountFormMeta> metas = accountFormMetaService.findByAccountFormIdAndMetaType(entity.getId(), "deptNo");
        if (metas == null || metas.size() == 0) {
            return new ResultUtil<List<Department>>().setErrorMsg("注册页面不存在部门信息");
        }
        List<Department> list = departmentService.findAllToTree();
        return new ResultUtil<List<Department>>().setData(list);
    }

    @RequestMapping(value = "/getInputAccountFormMeta/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取自定义注册需要输入的动态表单控件配置信息")
    public Result<List<AccountFormMeta>> getInputAccountFormMeta(@PathVariable String id) {
        AccountForm entity = accountFormService.get(id);
        if (null == entity) {
            return new ResultUtil<List<AccountFormMeta>>().setErrorMsg("不存在该注册页面");
        }
        List<AccountFormMeta> metas = entity.getAccountFormMetas();
        metas.removeIf(r -> r.getMetaType().equals("agreement"));
        metas.removeIf(r -> r.getMetaType().equals("button"));
        metas.removeIf(r -> r.getTitle().equals("验证码"));
        return new ResultUtil<List<AccountFormMeta>>().setData(metas);
    }

    @RequestMapping(value = "/getAccountFormMeta/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取单个动态表单控件配置信息")
    public Result<AccountFormMeta> getAccountFormMeta(@PathVariable String id) {
        AccountFormMeta accountFormMeta = accountFormMetaService.get(id);
        return new ResultUtil<AccountFormMeta>().setData(accountFormMeta);
    }

    @RequestMapping(value = "/getAccountAttributeValue", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取Account某个属性的值")
    public Result<String> getAccountAttributeValue(@RequestParam String accountId,@RequestParam String metaId ) {
        Account account = accountService.get(accountId);
        AccountFormMeta accountFormMeta = accountFormMetaService.get(metaId);
        if(accountFormMeta.getIsStandard()){
            Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
            if (null != object) {
                String fieldValue = object.toString();
                return new ResultUtil<String>().setData(fieldValue);
            }else {
                return new ResultUtil<String>().setData("");
            }
        }else {
            List<AccountFormField> accountFormFields = account.getAccountFormFields();
            if(null != accountFormFields && CollUtil.isNotEmpty(accountFormFields)) {
                List<AccountFormField> selectAccountFormFields =  accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                if(CollUtil.isNotEmpty(selectAccountFormFields)){
                    return new ResultUtil<String>().setData(selectAccountFormFields.get(0).getFieldData());
                }else {
                    return new ResultUtil<String>().setData("");
                }
            }else {
                return new ResultUtil<String>().setData("");
            }

        }
    }

    @RequestMapping(value = "/getAccountAttribute", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取Account某个属性")
    public Result<AccountAttributeVo> getAccountAttribute(@RequestParam String accountId, @RequestParam String metaId ) {
        AccountAttributeVo vo =new AccountAttributeVo();
        Account account = accountService.get(accountId);
        AccountFormMeta accountFormMeta = accountFormMetaService.get(metaId);
        if(accountFormMeta.getIsStandard()){
            vo.setMetaType(accountFormMeta.getMetaType());
            vo.setMetaDesc(accountFormMeta.getMetaDesc());
            Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
            if (null != object) {
                String fieldValue = object.toString();
                vo.setValue(fieldValue);
                if(StrUtil.equals("deptNo",accountFormMeta.getMetaType())){
                    Department department = departmentService.get(fieldValue);
                    if(department != null){
                        vo.setValue(department.getTitle());
                    }else{
                        vo.setValue("");
                    }
                }
                return new ResultUtil<AccountAttributeVo>().setData(vo);
            }else {
                return new ResultUtil<AccountAttributeVo>().setData(vo);
            }
        }else {
            List<AccountFormField> accountFormFields = account.getAccountFormFields();
            if(null != accountFormFields && CollUtil.isNotEmpty(accountFormFields)) {
                List<AccountFormField> selectAccountFormFields =  accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                if(CollUtil.isNotEmpty(selectAccountFormFields)){
                    AccountFormField temp = selectAccountFormFields.get(0);
                    vo.setMetaType(temp.getMetaType());
                    vo.setValue(temp.getFieldData());
                    vo.setMetaDesc(temp.getMetaTitle());
                    return new ResultUtil<AccountAttributeVo>().setData(vo);
                }else {
                    return new ResultUtil<AccountAttributeVo>().setData(vo);
                }
            }else {
                return new ResultUtil<AccountAttributeVo>().setData(vo);
            }

        }
    }
    @RequestMapping(value = "/getIdentifierForm", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取身份识别表单")
    public Result<AccountForm> getIdentifierForm(){
        Boolean isIdentifierForm = true;
        //根据appid设置成身份识别表单的主键
        AccountForm accountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(),isIdentifierForm);
        if(null == accountForm || accountForm.getAccountFormMetas().size() <1){
            return new ResultUtil<AccountForm>().setErrorMsg("请先创建初始的身份识别表单");
        }
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        return new ResultUtil<AccountForm>().setData(accountForm);
    }


}
