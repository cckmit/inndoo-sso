package com.ytdinfo.inndoo.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.dto.SimulationStaffRegistrationDto;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.EncryptVo;
import com.ytdinfo.inndoo.common.vo.ActAccountVo;
import com.ytdinfo.inndoo.common.vo.BaseResultVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IActAccountService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffService;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "会员接口")
@RequestMapping("/account")
@APIModifier(APIModifierType.PUBLIC)
public class AccountController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MatrixApiUtil matrixApiUtil;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountFormMetaService accountFormMetaService;

    @Autowired
    private AccountFormService accountFormService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private ExceptionLogService exceptionLogService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @Autowired
    private ActAccountService actAccountService;

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    @Autowired
    private CustomerInformationService customerInformationService;

    @Autowired
    private IStaffService iStaffService;

    @Autowired
    private IActAccountService iActAccountService;
    @XxlConf("matrix.rsa.publickey")
    private String publickey;

    @XxlConf("matrix.sm2.switch")
    private String sm2Switch;

    @XxlConf("matrix.sm2.publickeyq")
    private String sm2publickeyQ;

    @Autowired
    private SM2Utils sm2Utils;

    @Autowired
    private IAccountService iAccountService;
    @RequestMapping(value = "/getEncryptKey", method = RequestMethod.GET)
    @ApiOperation(value = "获取sm2的公钥")
    @ResponseBody
    public Result<String> getEncryptKey(){
        if(StrUtil.equals("true", sm2Switch) && StrUtil.isNotBlank(sm2publickeyQ) ){
            return new ResultUtil<String>().setData(sm2publickeyQ);
        }else{
            return new ResultUtil<String>().setData("");
        }
    }

    /**
     * 加密传输
     * @param accountRsaVo
     * @param code    手机验证码
     * @param code    手机号
     * @return
     */
    @RequestMapping(value = "/encryptSave", method = RequestMethod.POST)
    @ApiOperation(value = "保存或修改Account")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveAndUpdateEncrypt(@RequestBody EncryptVo accountRsaVo,
                                               @RequestParam(required = true) String formId,
                                               @RequestParam(required = false) String code,
                                               @RequestParam(required = false) String phone,
                                               HttpServletRequest request){
        Account account =  accountService.convertRsa(accountRsaVo);
        phone = sm2Utils.decrypt(phone);
        code = sm2Utils.decrypt(code);
        System.out.println(JSONUtil.toJsonStr(account));
        //return null;
        return saveAndUpdate(account,formId,code,phone,request);
    }



    /**
     * @param account
     * @param code    手机验证码
     * @param code    手机号
     * @return
     */
    @RequestMapping(value = "/saveAndUpdate", method = RequestMethod.POST)
    @ApiOperation(value = "保存或修改Account")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveAndUpdate(@RequestBody Account account, @RequestParam(required = true) String formId, @RequestParam(required = false) String code, @RequestParam(required = false) String phone, HttpServletRequest request) {

        if (StrUtil.isNotBlank(code) && StrUtil.isBlank(phone)) {
            return new ResultUtil<String>().setErrorMsg("手机号必填");
        }
        if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(phone)) {
            // 验证短信验证码
            String v = redisTemplate.opsForValue().get(CommonConstant.PRE_SMS + phone);
            if (StrUtil.isBlank(v)) {
                return new ResultUtil<String>().setErrorMsg("验证码失效或验证码不正确");
            }
            if (!code.equals(v)) {
                return new ResultUtil<String>().setErrorMsg("验证码不正确");
            }
        }
        AccountForm accountForm = accountFormService.get(formId);
        if (accountForm == null) {
            return new ResultUtil<String>().setErrorMsg("fromId不正确");
        }
        // 校验表单是否有效
        Result<String> checkresult = checkAccountForm(accountForm);
        if (!checkresult.isSuccess()) {
            return new ResultUtil<String>().setErrorMsg(checkresult.getMessage());
        }

        List<AccountFormMeta> formMetas = accountFormMetaService.findListByAccountFormId(formId);
        if (null == formMetas || formMetas.size() < 0) {
            return new ResultUtil<String>().setErrorMsg("fromId不正确或该fromId没有组件");
        }
        //获取必填的输入框
        List<AccountFormMeta> requiredFormMetas = formMetas.stream().filter(item -> item.getIsRequired()).collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>();
        if (null != requiredFormMetas && requiredFormMetas.size() > 0) {
            //获取标准输入组件必填的输入框
            List<AccountFormMeta> standardRequiredFormMetas = requiredFormMetas.stream().filter(item -> item.getIsStandard()).collect(Collectors.toList());
            if (null != standardRequiredFormMetas && standardRequiredFormMetas.size() > 0) {
                for (AccountFormMeta accountFormMeta : standardRequiredFormMetas) {
                    Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                    if (null == object) {
//                        System.out.println("131"+accountFormMeta.getTitle() + "必填");
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "必填");
                    } else {
                        if (StrUtil.isBlank(object.toString())) {
//                            System.out.println("135"+accountFormMeta.getTitle() + "必填");
                            return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "必填");
                        }
                        map.put(accountFormMeta.getMetaType(), object.toString().trim());
                    }
                }
            }
            //获取非标准输入组件必填的输入框
            List<AccountFormMeta> notStandardRequiredFormMetas = requiredFormMetas.stream().filter(item -> !item.getIsStandard()).collect(Collectors.toList());
            notStandardRequiredFormMetas.removeIf(item -> item.getMetaType().equals("note"));
            if (null != notStandardRequiredFormMetas && notStandardRequiredFormMetas.size() > 0) {
                List<AccountFormField> accountFormFields = account.getAccountFormFields();
                for (AccountFormMeta accountFormMeta : notStandardRequiredFormMetas) {
                    //获取存在必填非标准组件为accountFormMeta.getTitle()值
                    List<AccountFormField> addAccountFormFields = accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                    if (null != addAccountFormFields && addAccountFormFields.size() > 0) {
                        if (StrUtil.isBlank(addAccountFormFields.get(0).getFieldData())) {
//                            System.out.println("151"+accountFormMeta.getTitle() + "必填");
                            return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "必填");
                        }
                    } else if (!"验证码".equals(accountFormMeta.getTitle())) {
//                        System.out.println("155"+accountFormMeta.getTitle() + "必填");
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "必填");
                    }

                }
            }
        }


        String identifier = "";
        //拼接用户唯一标识
        Result<String> result = accountService.getIdentifier(account);
        if (result.isSuccess()) {
            identifier = result.getResult();
        } else {
            return new ResultUtil<String>().setErrorMsg(result.getMessage());
        }
        //唯一标识不存在时
        if (StrUtil.isBlank(account.getIdentifier())) {
            Account identifierAccount = new Account();
            try {
                identifierAccount = accountService.findByidentifier(identifier);
            } catch (Exception e) {
                String key = "Account::identifier:" + identifier;
                redisTemplate.delete(key);
                identifierAccount = accountService.findByidentifier(identifier);
            }

            if (null == identifierAccount) {
                account.setIdentifier(identifier);
            } else {
                if (StrUtil.isBlank(account.getId())) {
                    account = accountService.copyAccount(account, identifierAccount);
                }
                if (StrUtil.isNotBlank(account.getId()) && !identifierAccount.getId().equals(account.getId())) {
                    account = accountService.copyAccount(account, identifierAccount);
                }
                account.setIdentifier(identifier);
            }

        } else {
            //唯一标识存在时判断是否正确
            if (!identifier.equals(account.getIdentifier())) {
                return new ResultUtil<String>().setErrorMsg("用户唯一标识不匹配无法注册");
            }
        }
        account.setIdentifier(identifier);
        //表示普通用户注册页
        if (accountForm.getFormType() == 1) {
            if (null == account.getIsStaff()) {
                account.setIsStaff(0);
            }
            if (null != account.getIsStaff() && account.getIsStaff() != 1) {
                account.setIsStaff(0);
            }
        }
        //表示员工注册页
        if (accountForm.getFormType() == 0) {
            account.setIsStaff(1);
            String staffNo = account.getStaffNo();
            if (StringUtils.isNotBlank(staffNo)) {
                staffNo = staffNo.trim();
                Staff staff = staffService.findByStaffNo(staffNo);
                if (staff == null) {
                    return new ResultUtil<String>().setErrorMsg("员工号不存在，请输入正确的员工号");
                } else {
                    if (StrUtil.isNotBlank(staff.getAccountId())) {
                        if (StrUtil.isNotBlank(account.getId())) {
                            if (!staff.getAccountId().equals(account.getId())) {
                                return new ResultUtil<String>().setErrorMsg("该员工已绑定账号，不能重复绑定");
                            }
                        } else {
                            return new ResultUtil<String>().setErrorMsg("该员工已绑定账号，不能重复绑定");
                        }
                    }
                    if (staff.getStatus() != 0) {
                        return new ResultUtil<String>().setErrorMsg("该员工号状态不正常，请查看是否拉黑");
                    }
                }
            } else {
                //目前员工号主键不是所有员工注册表单必传主键
                // return new ResultUtil<String>().setErrorMsg("员工号必填");
            }
        }
        String md5Identifier = "";
        //拼接用户唯一标识
        Result<String> resultMd5Identifier = accountService.getmd5Identifier(account);
        if (resultMd5Identifier.isSuccess()) {
            md5Identifier = resultMd5Identifier.getResult();
        } else {
            return new ResultUtil<String>().setErrorMsg(resultMd5Identifier.getMessage());
        }
        account.setMd5identifier(md5Identifier);
        Account saveAndUpdateAccount;
        Result<Account> saveAndUpdateAccountResult = accountService.saveBindaccountForm(account, accountForm, map);
        if (!saveAndUpdateAccountResult.isSuccess()) {
            return new ResultUtil<String>().setErrorMsg(saveAndUpdateAccountResult.getMessage());
        } else {
            saveAndUpdateAccount = saveAndUpdateAccountResult.getResult();
        }
        // 已验证 清除key
        redisTemplate.delete(CommonConstant.PRE_SMS + phone);

        String actAccountId = request.getHeader("userId");
        if (StrUtil.isBlank(actAccountId)) {
            actAccountId = request.getHeader("userid");
        }

        String actDecodeAccountId = AESUtil.decrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD);
        String coreAccountId = saveAndUpdateAccount.getId();

        Date bindTime = saveAndUpdateAccount.getUpdateTime();
        if (bindTime == null) {
            bindTime = saveAndUpdateAccount.getCreateTime();
        }
        //返回加密的活动平台AccountId
        Result bindAccountResult = activityApiUtil.bindAccount(AESUtil.encrypt(coreAccountId, AESUtil.WXLOGIN_PASSWORD), actAccountId, formId, bindTime);
        if (bindAccountResult == null) {
            return new ResultUtil<String>().setErrorMsg("注册失败，请稍后重试");
        } else if (!bindAccountResult.isSuccess()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<String>().setErrorMsg("注册失败，您的注册信息已绑定其他账户");
        }

        String encryptActAccountId = bindAccountResult.getResult().toString();
        actAccountService.saveWithLock(actDecodeAccountId, coreAccountId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    //发送mq导入账户
                    MQMessage<Account> mqMessageAccount = new MQMessage<Account>();
                    mqMessageAccount.setAppid(UserContext.getAppid());
                    mqMessageAccount.setTenantId(UserContext.getTenantId());
                    mqMessageAccount.setContent(saveAndUpdateAccount);
                    rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_DECRYPTACCOUNT_MSG), mqMessageAccount);
                }
            });
        }

        return new ResultUtil<String>().setData(encryptActAccountId);
    }


    /**
     * 加密传输
     * @param accountRsaVo
     * @return
     */
    @RequestMapping(value = "/encryptBindStaff", method = RequestMethod.POST)
    @ApiOperation(value = "添加员工和账户绑定")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<BaseResultVo> encryptBindStaff(@RequestBody EncryptVo accountRsaVo, HttpServletRequest request) {
        Account account =  accountService.convertRsa(accountRsaVo);
        return accountBindStaff(account,request);
    }

    /**
     * @param account
     * @return
     */
    @RequestMapping(value = "/accountBindStaff", method = RequestMethod.POST)
    @ApiOperation(value = "添加员工和账户绑定")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<BaseResultVo> accountBindStaff(@RequestBody Account account, HttpServletRequest request) {
        BaseResultVo baseResultVo = new BaseResultVo();
        String actAccountId = AESUtil.decrypt(account.getActAccountId(), AESUtil.WXLOGIN_PASSWORD);
        String staffNo = account.getStaffNo();
        String name = account.getName();
        if (StrUtil.isBlank(account.getActAccountId())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("活动账户id未传");
        }
        if (StrUtil.isBlank(account.getStaffNo())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("员工号未传");
        }
        if (StrUtil.isBlank(account.getName())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("姓名未传");
        }
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("staffNo", account.getStaffNo());
        selectMap.put("name", AESUtil.encrypt(account.getName().trim()));
        Integer status = 0;//正常
        selectMap.put("status", status);
        selectMap.put("appid", UserContext.getAppid());
        List<Staff> staffs = iStaffService.findByMap(selectMap);
        if (CollectionUtil.isEmpty(staffs)) {
            baseResultVo.setSuccess(false);
            baseResultVo.setErrMsg("未查到要绑定的员工信息");
            return new ResultUtil<BaseResultVo>().setData(baseResultVo);
        }
        Staff staff = staffs.get(0);
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if (null != actAccount) {
            Account copyAccount = accountService.get(actAccount.getCoreAccountId());
            if (null != copyAccount) {
                BeanUtils.copyProperties(copyAccount, account);
                account.setStaffNo(staffNo);
                account.setName(name);
                account.setActAccountId(actAccountId);
            }
        }

        //获取身份识别表单
        Boolean isIdentifierForm = true;
        AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(account.getAppid(), isIdentifierForm);
        //获取初始的用户标识的注册页控件列表
        List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
        if (null != IsIdentifierFormMetas && IsIdentifierFormMetas.size() > 0) {
            for (AccountFormMeta accountFormMeta : IsIdentifierFormMetas) {
                if (accountFormMeta.getIsStandard()) {
                    Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                    if (null == object) {
                        //没有赋值给随机值
                        ReflectUtil.setFieldValue(account, accountFormMeta.getMetaType(), String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    } else {
                        if (StrUtil.isBlank(object.toString().trim())) {
                            //没有赋值给随机值
                            ReflectUtil.setFieldValue(account, accountFormMeta.getMetaType(), String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                        }
                    }
                }
            }
        } else {
            return new ResultUtil<BaseResultVo>().setErrorMsg("必须有用户识别控件");
        }

        String identifier = "";
        //拼接用户唯一标识
        Result<String> result = accountService.getIdentifier(account);
        if (result.isSuccess()) {
            identifier = result.getResult();
        } else {
            return new ResultUtil<BaseResultVo>().setErrorMsg(result.getMessage());
        }
        if (StrUtil.isBlank(account.getIdentifier())) {
            Account identifierAccount = new Account();
            try {
                identifierAccount = accountService.findByidentifier(identifier);
            } catch (Exception e) {
                String key = "Account::identifier:" + identifier;
                redisTemplate.delete(key);
                identifierAccount = accountService.findByidentifier(identifier);
            }

            if (null == identifierAccount) {
                account.setIdentifier(identifier);
            } else {
                if (StrUtil.isBlank(account.getId())) {
                    account = accountService.copyAccount(account, identifierAccount);
                }
                if (StrUtil.isNotBlank(account.getId()) && !identifierAccount.getId().equals(account.getId())) {
                    account = accountService.copyAccount(account, identifierAccount);
                }
                account.setIdentifier(identifier);
            }
        } else {
            //唯一标识存在时判断是否正确
            if (!identifier.equals(account.getIdentifier())) {
                return new ResultUtil<BaseResultVo>().setErrorMsg("用户唯一标识不匹配无法注册");
            }
        }
        String md5Identifier = "";
        //拼接用户唯一标识
        Result<String> resultMd5Identifier = accountService.getmd5Identifier(account);
        if (resultMd5Identifier.isSuccess()) {
            md5Identifier = resultMd5Identifier.getResult();
        } else {
            return new ResultUtil<BaseResultVo>().setErrorMsg(resultMd5Identifier.getMessage());
        }
        account.setMd5identifier(md5Identifier);
        if (StrUtil.isNotBlank(staff.getAccountId()) && !staff.getAccountId().equals(account.getId())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("该员工号已绑定其他账户");
        }
        staff.setAccountId(account.getId());
        Integer isStaff = 1;
        account.setIsStaff(isStaff);
        account = accountService.save(account);
        staffService.save(staff);
        String coreAccountId = account.getId();
        Date bindTime = account.getUpdateTime();
        if (bindTime == null) {
            bindTime = account.getCreateTime();
        }
        String formId = "accountBindStaff-id";
        //返回加密的活动平台AccountId
        Result bindAccountResult = activityApiUtil
                .bindAccount(AESUtil.encrypt(coreAccountId, AESUtil.WXLOGIN_PASSWORD), AESUtil.encrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD),
                        formId, bindTime);
        if (bindAccountResult == null) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("注册失败，请稍后重试");
        } else if (!bindAccountResult.isSuccess()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<BaseResultVo>().setErrorMsg("注册失败，您的注册信息已绑定其他账户");
        }
        if (null != actAccount && !actAccount.getCoreAccountId().equals(coreAccountId)) {
            lockTemplate.execute("updateActAccount:" + actAccountId, 3000, new Callback() {
                @Override
                public Object onGetLock() throws InterruptedException {
                    ActAccount act_account = actAccountService.findByActAccountId(actAccountId);
                    if (!act_account.getCoreAccountId().equals(coreAccountId)) {
                        actAccountService.delete(act_account);
                        act_account.setActAccountId(actAccountId);
                        act_account.setCoreAccountId(coreAccountId);
                        actAccountService.save(act_account);
                    }
                    return null;
                }

                @Override
                public Object onTimeout() throws InterruptedException {
                    ExceptionLog exceptionLog = new ExceptionLog();
                    exceptionLog.setUrl("update actAccount With Lock Timeout");
                    exceptionLog.setException("update actAccount With Lock Timeout");
                    exceptionLog.setMsgBody("actAccountId：" + actAccountId);
                    exceptionLog.setAppid(UserContext.getAppid());
                    exceptionLogService.save(exceptionLog);
                    return null;
                }
            });
        }
        if (null == actAccount) {
            actAccountService.saveWithLock(actAccountId, coreAccountId);
        }
        baseResultVo.setSuccess(true);
        return new ResultUtil<BaseResultVo>().setData(baseResultVo);
    }

    @RequestMapping(value = "/bindAccount", method = RequestMethod.GET)
    @ApiOperation(value = "活动平台账户和小核心账户的绑定")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> bindAccount(@RequestParam String encryCoreAccountId, @RequestParam String encryActAccountId) {

        String actAccountId = AESUtil.decrypt(encryActAccountId, AESUtil.WXLOGIN_PASSWORD);
        String coreAccountId = AESUtil.decrypt(encryCoreAccountId, AESUtil.WXLOGIN_PASSWORD);
        String formId = "bindAccount-Method";
        //返回加密的活动平台AccountId

        Date bindTime = null;
        Account account = accountService.get(coreAccountId);
        if (account != null) {
            bindTime = account.getCreateTime();
        }

        Result bindAccountResult = activityApiUtil.bindAccount(AESUtil.encrypt(coreAccountId, AESUtil.WXLOGIN_PASSWORD), encryActAccountId, formId, bindTime);
        if (bindAccountResult == null) {
            return new ResultUtil<String>().setErrorMsg("注册失败，请稍后重试");
        } else if (!bindAccountResult.isSuccess()) {
            return new ResultUtil<String>().setErrorMsg("注册失败，您的注册信息已绑定其他账户");
        }

        actAccountService.saveWithLock(actAccountId, coreAccountId);
        return new ResultUtil<String>().setData("OK");
    }

    /**
     * 判断注册表单是否有效
     *
     * @param accountForm
     * @return
     */
    public Result<String> checkAccountForm(AccountForm accountForm) {
        //活动过期时间判断
        Date endDate = accountForm.getEndDate();
        if (null != endDate) {
            Date endOfDay = DateUtil.endOfDay(endDate);
            Date nowDate = new Date();
            if (nowDate.getTime() > endOfDay.getTime()) {
                return new ResultUtil<String>().setErrorMsg("该注册活动页面已过期");
            }
        }
        Date startDate = accountForm.getStartDate();
        if (null != startDate) {
            Date startOfDay = DateUtil.beginOfDay(startDate);
            Date nowDate = new Date();
            if (nowDate.getTime() < startOfDay.getTime()) {
                return new ResultUtil<String>().setErrorMsg("该注册活动页面未开始");
            }
        }
        if (null == accountForm.getStatus()) {
            return new ResultUtil<String>().setErrorMsg("该注册活动页面未发布");
        }
        if (accountForm.getStatus() == 0) {
            return new ResultUtil<String>().setErrorMsg("该注册活动页面待发布");

        }
        if (accountForm.getStatus() == -1) {
            return new ResultUtil<String>().setErrorMsg("该注册活动页面已下架");
        }
        return new ResultUtil<String>().setSuccessMsg("校验成功");
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ApiOperation(value = "查询会员信息")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<Account> query(@RequestParam String accountId) {
        Account account = accountService.get(accountId);
        return new ResultUtil<Account>().setData(account);
    }

    //    @RequestMapping(value = "/query", method = RequestMethod.GET)
//    @ApiOperation(value = "查询会员信息")
//    public Result<AccountVo> query(@RequestParam String accountType, @RequestParam String openId) {
//
//        AccountVo accountVo = new AccountVo();
//        accountVo.setExist(false);
//        accountVo.setAccountId(StrUtil.EMPTY);
//        if(CommonConstant.ACCOUNT_TYPE_WEIXIN.equals(accountType)){
//            WechatUser wechatUser = wechatUserService.findByOpenId(openId);
//            if(wechatUser != null){
//                accountVo.setExist(true);
//                accountVo.setAccountId(wechatUser.getAccountId());
//            }
//        }
//        return new ResultUtil<AccountVo>().setData(accountVo);
//    }
//
//    @RequestMapping(value = "/weixin/save", method = RequestMethod.POST)
//    @ApiOperation(value = "创建微信用户")
//    public Result<String> saveWeixinUser(@RequestBody WechatUser wechatUser){
//        WechatUser wechatUserDb = wechatUserService.findByOpenId(wechatUser.getOpenId());
//        if(wechatUserDb != null){
//            String accountId = wechatUserDb.getAccountId();
//            BeanUtil.copyProperties(wechatUser,wechatUserDb);
//            wechatUserDb.setAccountId(accountId);
//            wechatUserService.save(wechatUserDb);
//        }else{
//            wechatUser.setAccountId(StrUtil.EMPTY);
//            wechatUserService.save(wechatUser);
//        }
//        return new ResultUtil<String>().setData("OK");
//    }
    @RequestMapping(value = "/getAccountIdListByPhone", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据手机号查询已绑定该手机号的会员账户Id清单")
    public Result<List<String>> getAccountIdListByPhone(@RequestParam String phone) {
        List<String> accountIdList = new ArrayList<>();
        List<Account> accountList = accountService.findByAppidAndPhone(UserContext.getAppid(), AESUtil.encrypt(phone));
        for (Account account : accountList) {
            accountIdList.add(account.getId());
        }
        return new ResultUtil<List<String>>().setData(accountIdList);
    }

    @RequestMapping(value = "/getAccountIdListByMD5Phone", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据MD5加密的手机号查询已绑定该手机号的会员账户Id清单")
    public Result<List<String>> getAccountIdListByMD5Phone(@RequestParam String md5Phone) {
        List<String> accountIdList = new ArrayList<>();
        List<Account> accountList = accountService.findByAppidAndMd5Phone(UserContext.getAppid(), md5Phone);
        for (Account account : accountList) {
            accountIdList.add(account.getId());
        }
        return new ResultUtil<List<String>>().setData(accountIdList);
    }

    @RequestMapping(value = "/getAccountIdListByCustomerNo", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据客户号查询会员账户Id清单")
    public Result<List<String>> getAccountIdListByCustomerNo(@RequestParam String customerNo) {
        List<String> accountIdList = new ArrayList<>();
        List<Account> accountList = accountService.findByCustomerNoAndAppid(AESUtil.encrypt(customerNo), UserContext.getAppid());
        for (Account account : accountList) {
            accountIdList.add(account.getId());
        }
        return new ResultUtil<List<String>>().setData(accountIdList);
    }

    /**
     * @param phone
     * @return
     */
    @RequestMapping(value = "/simulationRegistration", method = RequestMethod.POST)
    @ApiOperation(value = "模拟用户注册接口")
    @ResponseBody
    public Result<String> simulationRegistration(@RequestParam String actAccountId, @RequestParam String phone, String bindTime, String certNo,
                                                 String source,HttpServletRequest request) throws ParseException {

        String lockKey = "simulationRegistration-" + actAccountId;
        BaseResultVo baseResultVo = (BaseResultVo)lockTemplate.execute(lockKey, 1000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                BaseResultVo baseResultVo = new BaseResultVo();
                String decryptPhone;
                if(StrUtil.isNotEmpty(source)) {
                    decryptPhone = AESUtil.decrypt4v1(phone);
                }else{
                    //decryptPhone = AESUtil.decrypt(phone);
                    decryptPhone = AESUtil.comDecrypt(phone);
                }
                if (StrUtil.isEmpty(decryptPhone)) {
                    baseResultVo.setSuccess(false);
                    baseResultVo.setErrMsg("非法请求");
                    return baseResultVo;
                }
                Account account = new Account();
                account.setAppid(UserContext.getAppid());
                account.setPhone(decryptPhone);
                if(StrUtil.isNotEmpty(certNo)) {
                    account.setIdcardNo(certNo);
                }else{
                    account.setIdcardNo(StrUtil.EMPTY);
                }
                account.setBankcardNo(StrUtil.EMPTY);
                account.setBirthday(StrUtil.EMPTY);
                account.setName(StrUtil.EMPTY);
                account.setLicensePlateNo(StrUtil.EMPTY);
                String identifier = "";
                //拼接用户唯一标识
                Result<String> result = accountService.getIdentifier(account);
                if (result.isSuccess()) {
                    identifier = result.getResult();
                } else {
                    baseResultVo.setSuccess(false);
                    baseResultVo.setErrMsg(result.getMessage());
                    return baseResultVo;
                }
                if (StrUtil.isBlank(account.getIdentifier())) {
                    Account identifierAccount = new Account();
                    try {
                        identifierAccount = accountService.findByidentifier(identifier);
                    } catch (Exception e) {
                        String key = "Account::identifier:" + identifier;
                        redisTemplate.delete(key);
                        identifierAccount = accountService.findByidentifier(identifier);
                    }

                    if (null == identifierAccount) {
                        account.setIdentifier(identifier);
                    } else {
                        if (StrUtil.isBlank(account.getId())) {
                            account = accountService.copyAccount(account, identifierAccount);
                        }
                        if (StrUtil.isNotBlank(account.getId()) && !identifierAccount.getId().equals(account.getId())) {
                            account = accountService.copyAccount(account, identifierAccount);
                        }
                        account.setIdentifier(identifier);
                    }
                } else {
                    //唯一标识存在时判断是否正确
                    if (!identifier.equals(account.getIdentifier())) {
                        baseResultVo.setSuccess(false);
                        baseResultVo.setErrMsg("用户唯一标识不匹配无法注册");
                        return baseResultVo;
                    }
                }
                String md5Identifier = "";
                //拼接用户Md5唯一标识
                Result<String> resultMd5Identifier = accountService.getmd5Identifier(account);
                if (resultMd5Identifier.isSuccess()) {
                    md5Identifier = resultMd5Identifier.getResult();
                } else {
                    baseResultVo.setSuccess(false);
                    baseResultVo.setErrMsg(resultMd5Identifier.getMessage());
                    return baseResultVo;
                }
                account.setMd5identifier(md5Identifier);
                // 普通客户0、员工1
                account.setIsStaff(0);
                account.setStaffNo(StrUtil.EMPTY);
                account.setDeptNo(StrUtil.EMPTY);
                if(StrUtil.isNotEmpty(source)) {
                    account.setActAccountId(AESUtil.decrypt4v1(actAccountId));
                }else {
                    account.setActAccountId(AESUtil.decrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD));
                }
                account.setAccountFormFields(new ArrayList<>());
                if (StrUtil.isNotEmpty(bindTime)) {
                    DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        account.setCreateTime(fmt.parse(bindTime));
                    } catch (ParseException e) {
                        account.setCreateTime(new Date());
                    }
                }
                //改为同步操作
                ActAccount tempActAccount = actAccountService.accountInput(account,source);
                if (tempActAccount == null) {
                    baseResultVo.setSuccess(false);
                    baseResultVo.setErrMsg("注册异常");
                    return baseResultVo;
                }
                baseResultVo.setSuccess(true);
                baseResultVo.setErrMsg(tempActAccount.getCoreAccountId());
                return baseResultVo;
            }
            @Override
            public Object onTimeout() throws InterruptedException {
                BaseResultVo baseResultVo = new BaseResultVo();
                baseResultVo.setSuccess(false);
                baseResultVo.setErrMsg("注册异常");
                return baseResultVo;
            }
        });
        if (baseResultVo != null) {
            if( baseResultVo.isSuccess()){
                return new ResultUtil<String>().setData(baseResultVo.getErrMsg());
            } else {
                return new ResultUtil<String>().setErrorMsg(baseResultVo.getErrMsg());
            }
        } else {
            return new ResultUtil<String>().setData(StrUtil.EMPTY);
        }
    }


    @RequestMapping(value = "/simulationStaffRegistration", method = RequestMethod.POST)
    @ApiOperation(value = "模拟员工注册接口")
    @ResponseBody
    public Result<String> simulationStaffRegistration(@RequestParam String data) {
        SimulationStaffRegistrationDto dto = JSONUtil.toBean(data, SimulationStaffRegistrationDto.class);
        // 简单的请求校验
        String decryptPhone = AESUtil.decrypt(dto.getPhone());
        if (StrUtil.isEmpty(decryptPhone)) {
            return new ResultUtil<String>().setErrorMsg("core:非法请求:" + dto.getPhone());
        }

        String lockKey = "simulationStaffRegistration-" + dto.getActAccountId();
        Object object = lockTemplate.execute(lockKey, 1000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                //改为同步操作
                return actAccountService.simulationStaffRegistration(dto);
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                return false;
            }
        });
        if (object != null) {
            boolean flag = (boolean) object;
            if (flag) {
                return new ResultUtil<String>().setData("success");
            }
        }
        return new ResultUtil<String>().setErrorMsg("fail");
    }

    /**
     * @param phone
     * @return
     */
    @RequestMapping(value = "/updateCustomerInformation", method = RequestMethod.POST)
    @ApiOperation(value = "更新客户信息接口")
    @ResponseBody
    public Result<String> updateCustomerInformation(@RequestParam String accountId, @RequestParam String phone, String customerNo, String orgNo, String branchNo,
                                                    HttpServletRequest request) {
        String decAccountId = AESUtil.decrypt(accountId, AESUtil.WXLOGIN_PASSWORD);
        Account account = accountService.get(decAccountId);
        if (account == null) {
            return new ResultUtil<String>().setErrorMsg("非法请求");
        }
        String decryptPhone = AESUtil.decrypt(phone);
        if (StrUtil.isEmpty(decryptPhone)) {
            return new ResultUtil<String>().setErrorMsg("非法请求");
        }
        //客户信息
        CustomerInformation customerInformation = null;
        if (StrUtil.isNotEmpty(customerNo)) {
            account.setCustomerNo(customerNo);
            customerInformation = new CustomerInformation();
            customerInformation.setCustomerNo(customerNo);
        }
        if (StrUtil.isNotEmpty(orgNo)) {
            if (customerInformation == null) {
                customerInformation = new CustomerInformation();
            }
            customerInformation.setBankBranchNo(orgNo);
        }
        if (StrUtil.isNotEmpty(branchNo)) {
            if (customerInformation == null) {
                customerInformation = new CustomerInformation();
            }
            customerInformation.setInstitutionalCode(branchNo);
        }
        if (customerInformation != null) {
            customerInformation.setPhone(decryptPhone);
        }
        CustomerInformation finalCustomerInformation = customerInformation;
        String lockKey = "updateCustomerInformation-" + account.getActAccountId();
        lockTemplate.execute(lockKey, 1000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                //改为同步操作
                if (finalCustomerInformation != null) {
                    Result<String> identifierResult = customerInformationService.getIdentifier(finalCustomerInformation);
                    if (identifierResult.isSuccess()) {
                        String identifier = identifierResult.getResult();
                        CustomerInformation temp = customerInformationService.findByIdentifier(identifier);
                        if (temp != null) {
                            temp.setPhone(decryptPhone);
                            if (StrUtil.isNotEmpty(customerNo)) {
                                temp.setCustomerNo(customerNo);
                            }
                            if (StrUtil.isNotEmpty(orgNo)) {
                                temp.setBankBranchNo(orgNo);
                            }
                            if (StrUtil.isNotEmpty(branchNo)) {
                                temp.setInstitutionalCode(branchNo);
                            }
                            customerInformationService.update(temp);
                        } else {
                            finalCustomerInformation.setIdentifier(identifier);
                            customerInformationService.save(finalCustomerInformation);
                        }
                    }
                }
                return null;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                return null;
            }
        });
        return new ResultUtil<String>().setData("success");
    }


    @RequestMapping(value = "/untied")
    @ApiOperation(value = "解绑客户")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> untied(@RequestParam String id, @RequestParam String actAccountId) {
        if (null == id) {
            return new ResultUtil<Object>().setErrorMsg("客户号必传");
        }
        List<Account> accounts = new ArrayList<>();
        List<String> list = new ArrayList<>();
        Account account = accountService.get(id);
        if (null != account) {
            accounts.add(account);
        }
        list.add(id);
//        //解除act绑定小核心账户
//        Boolean untiedAccount = activityApiUtil.untied(list);
//        if (!untiedAccount) {
//            return new ResultUtil<Object>().setErrorMsg("act解绑小核心账户异常");
//        }
        //解除小核心项目act和core账户的绑定
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if (null != actAccount) {
            actAccountService.delete(actAccount);
        }
        //解除员工和账户的绑定
        List<Staff> staffs = staffService.findByAccountIds(list);
        for (Staff staff : staffs) {
            staffService.removeFromCache(staff.getAccountId());
            staff.setAccountId("");
        }
        if (null != staffs && staffs.size() > 0) {
            staffService.saveOrUpdateAll(staffs);
        }
        //如果是员工解除员工和账户的绑定
        if(CollectionUtil.isNotEmpty(accounts)){
            for (Account acc: accounts){
                account = accountService.clearAccount(acc);
                if(account.getIsStaff() == 1 ){
                    Integer isStaff = 0;
                    account.setIsStaff(isStaff);
                    account.setStaffNo("");
                }
                accountService.save(account);
            }
        }
        return new ResultUtil<Object>().setSuccessMsg("解绑成功");
    }


    @RequestMapping(value = "/isBindThePhone", method = RequestMethod.POST)
    @ApiOperation(value = "小核心账户是否绑定指定手机号")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result isBindThePhone(@RequestParam String encryptCoreAccountId, @RequestParam String encryptPhone) {
        String phone = AESUtil.decrypt(encryptPhone);
        String coreAccountId = AESUtil.decrypt(encryptCoreAccountId, AESUtil.WXLOGIN_PASSWORD);
        Account account = accountService.get(coreAccountId);
        boolean flag = false;
        if (account != null) {
            flag = phone.equals(account.getPhone());
        }
        if (flag) {
            return new ResultUtil().setSuccessMsg("账户绑定了指定手机号");
        } else {
            return new ResultUtil().setErrorMsg("账户没有绑定指定手机号");
        }
    }

    @RequestMapping(value = "/getCoreAccountByActAccountId", method = RequestMethod.GET)
    @ApiOperation(value = "根据活动账户Id获取小核心账户信息")
    @ResponseBody
    public Result<Account> getCoreAccountByActAccountId(@RequestParam String actAccountId) {
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
        if(null == account){
            account = new Account();
        }
        return new ResultUtil<Account>().setData(account);
    }

    @RequestMapping(value = "/getCoreAccountByIdentifier", method = RequestMethod.GET)
    @ApiOperation(value = "根据唯一标识获取小核心账户")
    @ResponseBody
    public Result<Account> getCoreAccountByIdentifier(@RequestParam String identifier) {
        Account uaccount = new Account();
        List<Account> accounts = accountService.findListByidentifier(identifier);
        if(CollectionUtil.isNotEmpty(accounts)){
            if(accounts.size()>1){
                for(Account account: accounts){
                    List<ActAccount> actAccounts = actAccountService.findByCoreAccountId(account.getId());
                    if (CollectionUtil.isNotEmpty(actAccounts)) {
                        Account returnAccount = new Account();
                        BeanUtils.copyProperties(account, returnAccount);
                        returnAccount = accountService.decryptAccount(returnAccount);
                        return new ResultUtil<Account>().setData(returnAccount);
                    }
                }
                Account returnAccount = new Account();
                BeanUtils.copyProperties(accounts.get(0), returnAccount);
                returnAccount = accountService.decryptAccount(returnAccount);
                return new ResultUtil<Account>().setData(returnAccount);
            }else {
                Account returnAccount = new Account();
                BeanUtils.copyProperties(accounts.get(0), returnAccount);
                returnAccount = accountService.decryptAccount(returnAccount);
                return new ResultUtil<Account>().setData(returnAccount);
            }
        }
        return new ResultUtil<Account>().setData(uaccount);
    }

    @RequestMapping(value = "/untiedById")
    @ApiOperation(value = "根据小核心账户解绑客户")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> untiedById(@RequestParam String coreAccountid) {
        List<Account> accounts = new ArrayList<>();
        List<String> list = new ArrayList<>();
        Account accountv = accountService.get(coreAccountid);
        if (null != accountv ) {
            accounts.add(accountv);
        }
        list.add(coreAccountid);

        //解除小核心项目act和core账户的绑定
        List<ActAccount> actAccounts = actAccountService.findByCoreAccountIds(list);
        if(null != actAccounts && actAccounts.size()>0 ){
            actAccountService.delete(actAccounts);
        }
        //解除员工和账户的绑定
        List<Staff> staffs = staffService.findByAccountIds(list);
        for (Staff staff: staffs) {
            staffService.removeFromCache(staff.getAccountId());
            staff.setAccountId("");
        }
        if(null != staffs && staffs.size()>0){
            staffService.saveOrUpdateAll(staffs);
        }
        //如果是员工解除员工和账户的绑定
        if(CollectionUtil.isNotEmpty(accounts)){
            for (Account caccount: accounts){
                caccount = accountService.clearAccount(caccount);
                if(caccount.getIsStaff() == 1 ){
                    Integer isStaff = 0;
                    caccount.setIsStaff(isStaff);
                    caccount.setStaffNo("");
                }
                accountService.save(caccount);
            }
        }
        //解除act绑定小核心账户
        Boolean untiedAccount = activityApiUtil.untied(list);
        if(!untiedAccount ){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<Object>().setErrorMsg("act解绑小核心账户异常");
        }
        return new ResultUtil<Object>().setSuccessMsg("解绑成功");
    }

    @RequestMapping(value = "/checkCustomerNo", method = RequestMethod.GET)
    @ApiOperation(value = "判断核心客户号是否重复绑定")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> checkCustomerNo(@RequestParam String customerNo, @RequestParam String actAccountId, @RequestParam(required = false)  Byte accountType) {
        actAccountId = AESUtil.decrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD);
        //获取注册的有相同核心客户号的小核心账号
        List<Account> accounts = accountService.findByCustomerNoAndAppid(AESUtil.encrypt(customerNo), UserContext.getAppid());
        List<Account> bindAccounts = new ArrayList<>();//绑定活动平台账号
        Integer cnt = 0;
        if (CollectionUtil.isNotEmpty(accounts)) {
            for (Account account : accounts) {
                List<ActAccount> actAccounts = actAccountService.findByCoreAccountId(account.getId());
                //判断小核心账号是否绑定活动平台账号
                if (CollectionUtil.isNotEmpty(actAccounts)) {
                    //有绑定+1
                    cnt = cnt + 1;
                    bindAccounts.add(account);
                } else {
                    accountService.delete(account);
                }
            }
            if (cnt > 1) {
                //绑定人数大于1判定重复绑定
                return new ResultUtil<String>().setErrorMsg("重复绑定");
            } else {
                if(null == accountType || accountType != 1){
                    //直接返回
                    Account account = new Account();
                    account.setCustomerNo(customerNo);
                    account.setAppid(UserContext.getAppid());
                    String identifier = "";
                    //拼接用户唯一标识
                    Result<String> result = accountService.getIdentifier(account);
                    if (result.isSuccess()) {
                        identifier = result.getResult();
                    }
                    //判定未重复绑定
                    return new ResultUtil<String>().setData(identifier);
                }
                if (cnt == 0) {
                    Account account = new Account();
                    account.setCustomerNo(customerNo);
                    account.setAppid(UserContext.getAppid());
                    String identifier = "";
                    //拼接用户唯一标识
                    Result<String> result = accountService.getIdentifier(account);
                    if (result.isSuccess()) {
                        identifier = result.getResult();
                    }
                    //判定未重复绑定
                    return new ResultUtil<String>().setData(identifier);
                } else if (cnt == 1) {
                    Account codAccount = bindAccounts.get(0);
                    Account bindAccount = new Account();
                    BeanUtils.copyProperties(codAccount, bindAccount);
                    bindAccount = accountService.decryptAccount(bindAccount);
                    List<ActAccountVo> actAccountVos = activityApiUtil.getCoreAccountId(bindAccount.getId());
                    List<ActAccountVo> accountTypeFilters = new ArrayList<>();
                    for (ActAccountVo actAccountVo: actAccountVos){
                        if (actAccountVo.getAccountType().intValue() == accountType.intValue()){
                            accountTypeFilters.add(actAccountVo);
                        }
                    }
                    if(CollectionUtil.isNotEmpty(accountTypeFilters)){
                        List<ActAccountVo> actAccountFilters = new ArrayList<>();
                        for (ActAccountVo actAccountVo: accountTypeFilters){
                            if (actAccountVo.getAccountId().equals(actAccountId) ){
                                actAccountFilters.add(actAccountVo);
                            }
                        }
                        if(CollectionUtil.isEmpty(actAccountFilters)){
                            //有绑定
                            return new ResultUtil<String>().setErrorMsg("重复绑定");
                        }else {
                            Account account = new Account();
                            account.setCustomerNo(customerNo);
                            account.setAppid(UserContext.getAppid());
                            String identifier = "";
                            //拼接用户唯一标识
                            Result<String> result = accountService.getIdentifier(account);
                            if (result.isSuccess()) {
                                identifier = result.getResult();
                            }
                            //不是重复绑定
                            return new ResultUtil<String>().setData(identifier);
                        }
                    } else {
                        Account account = new Account();
                        account.setCustomerNo(customerNo);
                        account.setAppid(UserContext.getAppid());
                        String identifier = "";
                        //拼接用户唯一标识
                        Result<String> result = accountService.getIdentifier(account);
                        if (result.isSuccess()) {
                            identifier = result.getResult();
                        }
                        //不是重复绑定
                        return new ResultUtil<String>().setData(identifier);
                    }

                } else {
                    Account account = new Account();
                    account.setCustomerNo(customerNo);
                    account.setAppid(UserContext.getAppid());
                    String identifier = "";
                    //拼接用户唯一标识
                    Result<String> result = accountService.getIdentifier(account);
                    if (result.isSuccess()) {
                        identifier = result.getResult();
                    }
                    //判定未重复绑定
                    return new ResultUtil<String>().setData(identifier);
                }
            }
        } else {
            //未查询到直接判定未重复绑定
            Account account = new Account();
            account.setCustomerNo(customerNo);
            account.setAppid(UserContext.getAppid());
            String identifier = "";
            //拼接用户唯一标识
            Result<String> result = accountService.getIdentifier(account);
            if (result.isSuccess()) {
                identifier = result.getResult();
            }
            return new ResultUtil<String>().setData(identifier);
        }
    }

    /**
     * @param phone
     * @return
     */
    @RequestMapping(value = "/changeCertNo", method = RequestMethod.POST)
    @ApiOperation(value = "变更身份证")
    @ResponseBody
    public Result<String> changeCertNo(@RequestParam String phone, @RequestParam String certNo,HttpServletRequest request) throws ParseException {
        String decryptPhone= AESUtil.decrypt4v1(phone);
        if (StrUtil.isEmpty(decryptPhone)) {
            return new ResultUtil<String>().setErrorMsg("非法请求");
        }
        certNo = HtmlUtils.htmlEscape(certNo);
        List<Account> accountList = accountService.findByAppidAndPhone(UserContext.getAppid(),AESUtil.encrypt(decryptPhone));
        for (Account account : accountList) {
            account = accountService.decryptAccount(account);
            account.setIdcardNo(certNo);
            accountService.update(account);
        }
        return new ResultUtil<String>().setData(Integer.toString(accountList.size()));
    }

    @RequestMapping(value = "/queryIdentifierByAccountId", method = RequestMethod.GET)
    @ApiOperation(value = "根据coreAccountId查询客户的唯一标识")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<String> queryIdentifierByCoreAccountId(@RequestParam String accountId) {
        String identifier = "";
        //获取身份识别表单
        AccountForm accountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(), true);
        if (null != accountForm) {
            //获取初始的用户标识的注册页控件列表
            List<AccountFormMeta> accountFormMetaList = accountForm.getAccountFormMetas();
            if (null != accountFormMetaList && accountFormMetaList.size() > 0) {
                Account account = accountService.get(accountId);
                for (AccountFormMeta accountFormMeta : accountFormMetaList) {
                    if (accountFormMeta.getMetaType().equals("phone") && StrUtil.isNotBlank(account.getPhone())) {
                        identifier = account.getPhone();
                        break;
                    } else if (accountFormMeta.getMetaType().equals("customerNo") && StrUtil.isNotBlank(account.getCustomerNo())) {
                        identifier = account.getCustomerNo();
                        break;
                    }
                }
            }
        }
        return new ResultUtil<String>().setData(identifier);
    }

    @RequestMapping(value = "/deleteByCoreAccountId", method = RequestMethod.GET)
    @ApiOperation(value = "物理删除小核心账户信息")
    @APIModifier(APIModifierType.PRIVATE)
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteByCoreAccountId (@RequestParam String coreAccountId) {
        Account account = accountService.get(coreAccountId);
        if ( null == account ) {
            return new ResultUtil<String>().setErrorMsg("未查到小核心信息");
        }
        List<String> list = new ArrayList<>();
        list.add(coreAccountId);
        //解除员工和账户的绑定
        List<Staff> staffs = staffService.findByAccountIds(list);
        for (Staff staff: staffs) {
            staffService.removeFromCache(staff.getAccountId());
            staff.setAccountId("");
        }
        if(null != staffs && staffs.size()>0){
            staffService.saveOrUpdateAll(staffs);
        }
        iAccountService.deleteById(coreAccountId);
        iActAccountService.deleteByCoreAccountId(coreAccountId);
        return new ResultUtil<String>().setData( account.getPhone() );
    }

   /* @RequestMapping(value = "/testlive", method = RequestMethod.GET)
    @ApiOperation(value = "测试接口")
    @ResponseBody
    public Result<Boolean> testlive() throws ParseException {






      *//*  if(StrUtil.isBlank(dto.getAccountId())){
            return new ResultUtil<Boolean>().setData(false);
        }
        Account accout=accountService.get(dto.getAccountId());
        if(accout==null){
            return new ResultUtil<Boolean>().setData(false);
        }
        if(StrUtil.isBlank(accout.getPhone())){
            return new ResultUtil<Boolean>().setData(false);
        }*//*
        String phone="18559662301"; //accout.getPhone();
        // Result<Boolean> result =checkConsumerMobile(phone);
        Result<Boolean> result = checkConsumerMobileServiceItestmpl.checkConsumerMobile(phone);
        if(result.isSuccess()){
            if(result.getResult())
            {
                return new ResultUtil<Boolean>().setData(true);
            }
            else{
                return new ResultUtil<Boolean>().setData(false);
            }

        }
        return new ResultUtil<Boolean>().setData(false);








*//*
        Result<Boolean> kk=  checkConsumerMobileServiceItestmpl.checkConsumerMobile("18559662301");

        Result<Boolean> kk1=  checkConsumerMobileServiceItestmpl.checkConsumerMobile("17805917901");

        Result<Boolean> kk2=  checkConsumerMobileServiceItestmpl.checkConsumerMobile("15880802528");*//*

*//*
        Map paramMap = new HashMap<>();
        paramMap.put("tenantId", "315210878691680256");
        paramMap.put("wxappid", "wx4f0a8cee7b7ceb56");
        paramMap.put("appkey", "ons7Scs8bEDP1Fcj71rPJ7kxOhJipGmW");
        paramMap.put("timestamp", System.currentTimeMillis());
        paramMap.put("listId", listId);
        paramMap.put("record", record);
        paramMap.put("appsecret", "ICcnsCDf1pQztl64WgFHDxRoj8mBisAy");
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        paramMap.put("sign", sign);
        paramMap.remove("appsecret"); //appsecret仅用于生成签名，请求时请移除
        String params = HttpUtil.toParams(paramMap);
        String url = "https://inndoo.ytdcloud.com/core-api/whitelist/pushListRecord"+ "?" + params;
        String resultStr = HttpRequestUtil.get(url);*//*
       // return new ResultUtil<String>().setData("");
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ApiOperation(value = "测试接口")
    @ResponseBody
    public Result<String> changeCertNo(@RequestParam String listId, @RequestParam  String record,String times) throws ParseException {
        Map paramMap = new HashMap<>();
        paramMap.put("tenantId", "154040647714607104");
        paramMap.put("wxappid", "wx351238e871260534");
        paramMap.put("appkey", "4eB5Vz2Ufw1fLxO77OSGatGiskC9BzDJ");
        paramMap.put("timestamp", System.currentTimeMillis());
        paramMap.put("listId", listId);
        paramMap.put("record", record);
        paramMap.put("times", times);
        paramMap.put("appsecret", "u1cQO8kOa42SPBlhFofmaeMC3ngmj2gu");
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        paramMap.put("sign", sign);
        paramMap.remove("appsecret"); //appsecret仅用于生成签名，请求时请移除
        String params = HttpUtil.toParams(paramMap);
        String url = "http://localhost:5031/core-api/whitelist/pushListRecord"+ "?" + params;
        String resultStr = HttpRequestUtil.get(url);
        return new ResultUtil<String>().setData(resultStr);
    }

    @RequestMapping(value = "/test2", method = RequestMethod.GET)
    @ApiOperation(value = "测试接口")
    @ResponseBody
    public Result<String> changeCertNo2(@RequestParam String listId, @RequestParam  String record,String times) throws ParseException {
        Map paramMap = new HashMap<>();
        paramMap.put("tenantId", "303234888457256960");
        paramMap.put("wxappid", "wxc47bbac687d8e219");
        paramMap.put("appkey", "5RkGtaL9wL2Fh2QLB4tkRjOj7pxHNhPs");
        paramMap.put("timestamp", System.currentTimeMillis());
        paramMap.put("listId", listId);
        paramMap.put("record", record);
        paramMap.put("times", times);
        paramMap.put("appsecret", "scgXBRDA1gIkLiLabUlnG4mb4BX4rPGk");
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        paramMap.put("sign", sign);
        paramMap.remove("appsecret"); //appsecret仅用于生成签名，请求时请移除
        String params = HttpUtil.toParams(paramMap);
        String url = "https://inndoo.ytdcloud.com/core-api/whitelist/pushListRecord"+ "?" + params;
        long startTime = System.currentTimeMillis();    //获取开始时间
        String resultStr = HttpRequestUtil.get(url);
        long endTime = System.currentTimeMillis();    //获取结束时间
        System.out.println("程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间
        return new ResultUtil<String>().setData(resultStr);
    }
*/




    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ApiOperation(value = "测试接口")
    @ResponseBody
    public Result<String> changeCertNo(@RequestParam String listId, @RequestParam  String record,String times) throws ParseException {
        Map paramMap = new HashMap<>();
        paramMap.put("tenantId", "154040647714607104");
        paramMap.put("wxappid", "wx351238e871260534");
        paramMap.put("appkey", "4vjYEn85EddqRe59");
        paramMap.put("timestamp", System.currentTimeMillis());
        paramMap.put("listId", listId);
        paramMap.put("record", record);
        paramMap.put("times", times);
        paramMap.put("appsecret", "ee1WkKcHRP6Nw96F4vcnj0jciObfr7cF");
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        paramMap.put("sign", sign);
        paramMap.remove("appsecret"); //appsecret仅用于生成签名，请求时请移除
        String params = HttpUtil.toParams(paramMap);
        String url = "http://dev.inndoo.ytdinfo.com.cn/activity-api/weixin/pushListRecord"+ "?" + params;
        String resultStr = HttpRequestUtil.get(url);
        return new ResultUtil<String>().setData(resultStr);
    }

    @RequestMapping(value = "/countByCreateTime", method = RequestMethod.GET)
    @ApiOperation(value = "查询指定日期区间的数量")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<Integer> countByCreateTime(@RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime) {
        Integer count = iAccountService.countByCreateTime(startTime, endTime);
        return new ResultUtil<Integer>().setData(count);
    }
}
