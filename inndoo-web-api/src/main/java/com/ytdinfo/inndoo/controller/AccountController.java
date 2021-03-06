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
@Api(description = "????????????")
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
    @ApiOperation(value = "??????sm2?????????")
    @ResponseBody
    public Result<String> getEncryptKey(){
        if(StrUtil.equals("true", sm2Switch) && StrUtil.isNotBlank(sm2publickeyQ) ){
            return new ResultUtil<String>().setData(sm2publickeyQ);
        }else{
            return new ResultUtil<String>().setData("");
        }
    }

    /**
     * ????????????
     * @param accountRsaVo
     * @param code    ???????????????
     * @param code    ?????????
     * @return
     */
    @RequestMapping(value = "/encryptSave", method = RequestMethod.POST)
    @ApiOperation(value = "???????????????Account")
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
     * @param code    ???????????????
     * @param code    ?????????
     * @return
     */
    @RequestMapping(value = "/saveAndUpdate", method = RequestMethod.POST)
    @ApiOperation(value = "???????????????Account")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveAndUpdate(@RequestBody Account account, @RequestParam(required = true) String formId, @RequestParam(required = false) String code, @RequestParam(required = false) String phone, HttpServletRequest request) {

        if (StrUtil.isNotBlank(code) && StrUtil.isBlank(phone)) {
            return new ResultUtil<String>().setErrorMsg("???????????????");
        }
        if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(phone)) {
            // ?????????????????????
            String v = redisTemplate.opsForValue().get(CommonConstant.PRE_SMS + phone);
            if (StrUtil.isBlank(v)) {
                return new ResultUtil<String>().setErrorMsg("????????????????????????????????????");
            }
            if (!code.equals(v)) {
                return new ResultUtil<String>().setErrorMsg("??????????????????");
            }
        }
        AccountForm accountForm = accountFormService.get(formId);
        if (accountForm == null) {
            return new ResultUtil<String>().setErrorMsg("fromId?????????");
        }
        // ????????????????????????
        Result<String> checkresult = checkAccountForm(accountForm);
        if (!checkresult.isSuccess()) {
            return new ResultUtil<String>().setErrorMsg(checkresult.getMessage());
        }

        List<AccountFormMeta> formMetas = accountFormMetaService.findListByAccountFormId(formId);
        if (null == formMetas || formMetas.size() < 0) {
            return new ResultUtil<String>().setErrorMsg("fromId???????????????fromId????????????");
        }
        //????????????????????????
        List<AccountFormMeta> requiredFormMetas = formMetas.stream().filter(item -> item.getIsRequired()).collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>();
        if (null != requiredFormMetas && requiredFormMetas.size() > 0) {
            //??????????????????????????????????????????
            List<AccountFormMeta> standardRequiredFormMetas = requiredFormMetas.stream().filter(item -> item.getIsStandard()).collect(Collectors.toList());
            if (null != standardRequiredFormMetas && standardRequiredFormMetas.size() > 0) {
                for (AccountFormMeta accountFormMeta : standardRequiredFormMetas) {
                    Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                    if (null == object) {
//                        System.out.println("131"+accountFormMeta.getTitle() + "??????");
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "??????");
                    } else {
                        if (StrUtil.isBlank(object.toString())) {
//                            System.out.println("135"+accountFormMeta.getTitle() + "??????");
                            return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "??????");
                        }
                        map.put(accountFormMeta.getMetaType(), object.toString().trim());
                    }
                }
            }
            //?????????????????????????????????????????????
            List<AccountFormMeta> notStandardRequiredFormMetas = requiredFormMetas.stream().filter(item -> !item.getIsStandard()).collect(Collectors.toList());
            notStandardRequiredFormMetas.removeIf(item -> item.getMetaType().equals("note"));
            if (null != notStandardRequiredFormMetas && notStandardRequiredFormMetas.size() > 0) {
                List<AccountFormField> accountFormFields = account.getAccountFormFields();
                for (AccountFormMeta accountFormMeta : notStandardRequiredFormMetas) {
                    //????????????????????????????????????accountFormMeta.getTitle()???
                    List<AccountFormField> addAccountFormFields = accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                    if (null != addAccountFormFields && addAccountFormFields.size() > 0) {
                        if (StrUtil.isBlank(addAccountFormFields.get(0).getFieldData())) {
//                            System.out.println("151"+accountFormMeta.getTitle() + "??????");
                            return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "??????");
                        }
                    } else if (!"?????????".equals(accountFormMeta.getTitle())) {
//                        System.out.println("155"+accountFormMeta.getTitle() + "??????");
                        return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "??????");
                    }

                }
            }
        }


        String identifier = "";
        //????????????????????????
        Result<String> result = accountService.getIdentifier(account);
        if (result.isSuccess()) {
            identifier = result.getResult();
        } else {
            return new ResultUtil<String>().setErrorMsg(result.getMessage());
        }
        //????????????????????????
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
            //???????????????????????????????????????
            if (!identifier.equals(account.getIdentifier())) {
                return new ResultUtil<String>().setErrorMsg("???????????????????????????????????????");
            }
        }
        account.setIdentifier(identifier);
        //???????????????????????????
        if (accountForm.getFormType() == 1) {
            if (null == account.getIsStaff()) {
                account.setIsStaff(0);
            }
            if (null != account.getIsStaff() && account.getIsStaff() != 1) {
                account.setIsStaff(0);
            }
        }
        //?????????????????????
        if (accountForm.getFormType() == 0) {
            account.setIsStaff(1);
            String staffNo = account.getStaffNo();
            if (StringUtils.isNotBlank(staffNo)) {
                staffNo = staffNo.trim();
                Staff staff = staffService.findByStaffNo(staffNo);
                if (staff == null) {
                    return new ResultUtil<String>().setErrorMsg("????????????????????????????????????????????????");
                } else {
                    if (StrUtil.isNotBlank(staff.getAccountId())) {
                        if (StrUtil.isNotBlank(account.getId())) {
                            if (!staff.getAccountId().equals(account.getId())) {
                                return new ResultUtil<String>().setErrorMsg("?????????????????????????????????????????????");
                            }
                        } else {
                            return new ResultUtil<String>().setErrorMsg("?????????????????????????????????????????????");
                        }
                    }
                    if (staff.getStatus() != 0) {
                        return new ResultUtil<String>().setErrorMsg("???????????????????????????????????????????????????");
                    }
                }
            } else {
                //???????????????????????????????????????????????????????????????
                // return new ResultUtil<String>().setErrorMsg("???????????????");
            }
        }
        String md5Identifier = "";
        //????????????????????????
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
        // ????????? ??????key
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
        //???????????????????????????AccountId
        Result bindAccountResult = activityApiUtil.bindAccount(AESUtil.encrypt(coreAccountId, AESUtil.WXLOGIN_PASSWORD), actAccountId, formId, bindTime);
        if (bindAccountResult == null) {
            return new ResultUtil<String>().setErrorMsg("??????????????????????????????");
        } else if (!bindAccountResult.isSuccess()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<String>().setErrorMsg("??????????????????????????????????????????????????????");
        }

        String encryptActAccountId = bindAccountResult.getResult().toString();
        actAccountService.saveWithLock(actDecodeAccountId, coreAccountId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    //??????mq????????????
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
     * ????????????
     * @param accountRsaVo
     * @return
     */
    @RequestMapping(value = "/encryptBindStaff", method = RequestMethod.POST)
    @ApiOperation(value = "???????????????????????????")
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
    @ApiOperation(value = "???????????????????????????")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<BaseResultVo> accountBindStaff(@RequestBody Account account, HttpServletRequest request) {
        BaseResultVo baseResultVo = new BaseResultVo();
        String actAccountId = AESUtil.decrypt(account.getActAccountId(), AESUtil.WXLOGIN_PASSWORD);
        String staffNo = account.getStaffNo();
        String name = account.getName();
        if (StrUtil.isBlank(account.getActAccountId())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("????????????id??????");
        }
        if (StrUtil.isBlank(account.getStaffNo())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("???????????????");
        }
        if (StrUtil.isBlank(account.getName())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("????????????");
        }
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("staffNo", account.getStaffNo());
        selectMap.put("name", AESUtil.encrypt(account.getName().trim()));
        Integer status = 0;//??????
        selectMap.put("status", status);
        selectMap.put("appid", UserContext.getAppid());
        List<Staff> staffs = iStaffService.findByMap(selectMap);
        if (CollectionUtil.isEmpty(staffs)) {
            baseResultVo.setSuccess(false);
            baseResultVo.setErrMsg("?????????????????????????????????");
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

        //????????????????????????
        Boolean isIdentifierForm = true;
        AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(account.getAppid(), isIdentifierForm);
        //???????????????????????????????????????????????????
        List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
        if (null != IsIdentifierFormMetas && IsIdentifierFormMetas.size() > 0) {
            for (AccountFormMeta accountFormMeta : IsIdentifierFormMetas) {
                if (accountFormMeta.getIsStandard()) {
                    Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                    if (null == object) {
                        //????????????????????????
                        ReflectUtil.setFieldValue(account, accountFormMeta.getMetaType(), String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    } else {
                        if (StrUtil.isBlank(object.toString().trim())) {
                            //????????????????????????
                            ReflectUtil.setFieldValue(account, accountFormMeta.getMetaType(), String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                        }
                    }
                }
            }
        } else {
            return new ResultUtil<BaseResultVo>().setErrorMsg("???????????????????????????");
        }

        String identifier = "";
        //????????????????????????
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
            //???????????????????????????????????????
            if (!identifier.equals(account.getIdentifier())) {
                return new ResultUtil<BaseResultVo>().setErrorMsg("???????????????????????????????????????");
            }
        }
        String md5Identifier = "";
        //????????????????????????
        Result<String> resultMd5Identifier = accountService.getmd5Identifier(account);
        if (resultMd5Identifier.isSuccess()) {
            md5Identifier = resultMd5Identifier.getResult();
        } else {
            return new ResultUtil<BaseResultVo>().setErrorMsg(resultMd5Identifier.getMessage());
        }
        account.setMd5identifier(md5Identifier);
        if (StrUtil.isNotBlank(staff.getAccountId()) && !staff.getAccountId().equals(account.getId())) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("?????????????????????????????????");
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
        //???????????????????????????AccountId
        Result bindAccountResult = activityApiUtil
                .bindAccount(AESUtil.encrypt(coreAccountId, AESUtil.WXLOGIN_PASSWORD), AESUtil.encrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD),
                        formId, bindTime);
        if (bindAccountResult == null) {
            return new ResultUtil<BaseResultVo>().setErrorMsg("??????????????????????????????");
        } else if (!bindAccountResult.isSuccess()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<BaseResultVo>().setErrorMsg("??????????????????????????????????????????????????????");
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
                    exceptionLog.setMsgBody("actAccountId???" + actAccountId);
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
    @ApiOperation(value = "?????????????????????????????????????????????")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> bindAccount(@RequestParam String encryCoreAccountId, @RequestParam String encryActAccountId) {

        String actAccountId = AESUtil.decrypt(encryActAccountId, AESUtil.WXLOGIN_PASSWORD);
        String coreAccountId = AESUtil.decrypt(encryCoreAccountId, AESUtil.WXLOGIN_PASSWORD);
        String formId = "bindAccount-Method";
        //???????????????????????????AccountId

        Date bindTime = null;
        Account account = accountService.get(coreAccountId);
        if (account != null) {
            bindTime = account.getCreateTime();
        }

        Result bindAccountResult = activityApiUtil.bindAccount(AESUtil.encrypt(coreAccountId, AESUtil.WXLOGIN_PASSWORD), encryActAccountId, formId, bindTime);
        if (bindAccountResult == null) {
            return new ResultUtil<String>().setErrorMsg("??????????????????????????????");
        } else if (!bindAccountResult.isSuccess()) {
            return new ResultUtil<String>().setErrorMsg("??????????????????????????????????????????????????????");
        }

        actAccountService.saveWithLock(actAccountId, coreAccountId);
        return new ResultUtil<String>().setData("OK");
    }

    /**
     * ??????????????????????????????
     *
     * @param accountForm
     * @return
     */
    public Result<String> checkAccountForm(AccountForm accountForm) {
        //????????????????????????
        Date endDate = accountForm.getEndDate();
        if (null != endDate) {
            Date endOfDay = DateUtil.endOfDay(endDate);
            Date nowDate = new Date();
            if (nowDate.getTime() > endOfDay.getTime()) {
                return new ResultUtil<String>().setErrorMsg("??????????????????????????????");
            }
        }
        Date startDate = accountForm.getStartDate();
        if (null != startDate) {
            Date startOfDay = DateUtil.beginOfDay(startDate);
            Date nowDate = new Date();
            if (nowDate.getTime() < startOfDay.getTime()) {
                return new ResultUtil<String>().setErrorMsg("??????????????????????????????");
            }
        }
        if (null == accountForm.getStatus()) {
            return new ResultUtil<String>().setErrorMsg("??????????????????????????????");
        }
        if (accountForm.getStatus() == 0) {
            return new ResultUtil<String>().setErrorMsg("??????????????????????????????");

        }
        if (accountForm.getStatus() == -1) {
            return new ResultUtil<String>().setErrorMsg("??????????????????????????????");
        }
        return new ResultUtil<String>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<Account> query(@RequestParam String accountId) {
        Account account = accountService.get(accountId);
        return new ResultUtil<Account>().setData(account);
    }

    //    @RequestMapping(value = "/query", method = RequestMethod.GET)
//    @ApiOperation(value = "??????????????????")
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
//    @ApiOperation(value = "??????????????????")
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
    @ApiOperation(value = "?????????????????????????????????????????????????????????Id??????")
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
    @ApiOperation(value = "??????MD5????????????????????????????????????????????????????????????Id??????")
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
    @ApiOperation(value = "?????????????????????????????????Id??????")
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
    @ApiOperation(value = "????????????????????????")
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
                    baseResultVo.setErrMsg("????????????");
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
                //????????????????????????
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
                    //???????????????????????????????????????
                    if (!identifier.equals(account.getIdentifier())) {
                        baseResultVo.setSuccess(false);
                        baseResultVo.setErrMsg("???????????????????????????????????????");
                        return baseResultVo;
                    }
                }
                String md5Identifier = "";
                //????????????Md5????????????
                Result<String> resultMd5Identifier = accountService.getmd5Identifier(account);
                if (resultMd5Identifier.isSuccess()) {
                    md5Identifier = resultMd5Identifier.getResult();
                } else {
                    baseResultVo.setSuccess(false);
                    baseResultVo.setErrMsg(resultMd5Identifier.getMessage());
                    return baseResultVo;
                }
                account.setMd5identifier(md5Identifier);
                // ????????????0?????????1
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
                //??????????????????
                ActAccount tempActAccount = actAccountService.accountInput(account,source);
                if (tempActAccount == null) {
                    baseResultVo.setSuccess(false);
                    baseResultVo.setErrMsg("????????????");
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
                baseResultVo.setErrMsg("????????????");
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
    @ApiOperation(value = "????????????????????????")
    @ResponseBody
    public Result<String> simulationStaffRegistration(@RequestParam String data) {
        SimulationStaffRegistrationDto dto = JSONUtil.toBean(data, SimulationStaffRegistrationDto.class);
        // ?????????????????????
        String decryptPhone = AESUtil.decrypt(dto.getPhone());
        if (StrUtil.isEmpty(decryptPhone)) {
            return new ResultUtil<String>().setErrorMsg("core:????????????:" + dto.getPhone());
        }

        String lockKey = "simulationStaffRegistration-" + dto.getActAccountId();
        Object object = lockTemplate.execute(lockKey, 1000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                //??????????????????
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
    @ApiOperation(value = "????????????????????????")
    @ResponseBody
    public Result<String> updateCustomerInformation(@RequestParam String accountId, @RequestParam String phone, String customerNo, String orgNo, String branchNo,
                                                    HttpServletRequest request) {
        String decAccountId = AESUtil.decrypt(accountId, AESUtil.WXLOGIN_PASSWORD);
        Account account = accountService.get(decAccountId);
        if (account == null) {
            return new ResultUtil<String>().setErrorMsg("????????????");
        }
        String decryptPhone = AESUtil.decrypt(phone);
        if (StrUtil.isEmpty(decryptPhone)) {
            return new ResultUtil<String>().setErrorMsg("????????????");
        }
        //????????????
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
                //??????????????????
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
    @ApiOperation(value = "????????????")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> untied(@RequestParam String id, @RequestParam String actAccountId) {
        if (null == id) {
            return new ResultUtil<Object>().setErrorMsg("???????????????");
        }
        List<Account> accounts = new ArrayList<>();
        List<String> list = new ArrayList<>();
        Account account = accountService.get(id);
        if (null != account) {
            accounts.add(account);
        }
        list.add(id);
//        //??????act?????????????????????
//        Boolean untiedAccount = activityApiUtil.untied(list);
//        if (!untiedAccount) {
//            return new ResultUtil<Object>().setErrorMsg("act???????????????????????????");
//        }
        //?????????????????????act???core???????????????
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if (null != actAccount) {
            actAccountService.delete(actAccount);
        }
        //??????????????????????????????
        List<Staff> staffs = staffService.findByAccountIds(list);
        for (Staff staff : staffs) {
            staffService.removeFromCache(staff.getAccountId());
            staff.setAccountId("");
        }
        if (null != staffs && staffs.size() > 0) {
            staffService.saveOrUpdateAll(staffs);
        }
        //?????????????????????????????????????????????
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
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }


    @RequestMapping(value = "/isBindThePhone", method = RequestMethod.POST)
    @ApiOperation(value = "??????????????????????????????????????????")
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
            return new ResultUtil().setSuccessMsg("??????????????????????????????");
        } else {
            return new ResultUtil().setErrorMsg("?????????????????????????????????");
        }
    }

    @RequestMapping(value = "/getCoreAccountByActAccountId", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????Id???????????????????????????")
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
    @ApiOperation(value = "???????????????????????????????????????")
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
    @ApiOperation(value = "?????????????????????????????????")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> untiedById(@RequestParam String coreAccountid) {
        List<Account> accounts = new ArrayList<>();
        List<String> list = new ArrayList<>();
        Account accountv = accountService.get(coreAccountid);
        if (null != accountv ) {
            accounts.add(accountv);
        }
        list.add(coreAccountid);

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
        //??????act?????????????????????
        Boolean untiedAccount = activityApiUtil.untied(list);
        if(!untiedAccount ){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<Object>().setErrorMsg("act???????????????????????????");
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/checkCustomerNo", method = RequestMethod.GET)
    @ApiOperation(value = "???????????????????????????????????????")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> checkCustomerNo(@RequestParam String customerNo, @RequestParam String actAccountId, @RequestParam(required = false)  Byte accountType) {
        actAccountId = AESUtil.decrypt(actAccountId, AESUtil.WXLOGIN_PASSWORD);
        //?????????????????????????????????????????????????????????
        List<Account> accounts = accountService.findByCustomerNoAndAppid(AESUtil.encrypt(customerNo), UserContext.getAppid());
        List<Account> bindAccounts = new ArrayList<>();//????????????????????????
        Integer cnt = 0;
        if (CollectionUtil.isNotEmpty(accounts)) {
            for (Account account : accounts) {
                List<ActAccount> actAccounts = actAccountService.findByCoreAccountId(account.getId());
                //???????????????????????????????????????????????????
                if (CollectionUtil.isNotEmpty(actAccounts)) {
                    //?????????+1
                    cnt = cnt + 1;
                    bindAccounts.add(account);
                } else {
                    accountService.delete(account);
                }
            }
            if (cnt > 1) {
                //??????????????????1??????????????????
                return new ResultUtil<String>().setErrorMsg("????????????");
            } else {
                if(null == accountType || accountType != 1){
                    //????????????
                    Account account = new Account();
                    account.setCustomerNo(customerNo);
                    account.setAppid(UserContext.getAppid());
                    String identifier = "";
                    //????????????????????????
                    Result<String> result = accountService.getIdentifier(account);
                    if (result.isSuccess()) {
                        identifier = result.getResult();
                    }
                    //?????????????????????
                    return new ResultUtil<String>().setData(identifier);
                }
                if (cnt == 0) {
                    Account account = new Account();
                    account.setCustomerNo(customerNo);
                    account.setAppid(UserContext.getAppid());
                    String identifier = "";
                    //????????????????????????
                    Result<String> result = accountService.getIdentifier(account);
                    if (result.isSuccess()) {
                        identifier = result.getResult();
                    }
                    //?????????????????????
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
                            //?????????
                            return new ResultUtil<String>().setErrorMsg("????????????");
                        }else {
                            Account account = new Account();
                            account.setCustomerNo(customerNo);
                            account.setAppid(UserContext.getAppid());
                            String identifier = "";
                            //????????????????????????
                            Result<String> result = accountService.getIdentifier(account);
                            if (result.isSuccess()) {
                                identifier = result.getResult();
                            }
                            //??????????????????
                            return new ResultUtil<String>().setData(identifier);
                        }
                    } else {
                        Account account = new Account();
                        account.setCustomerNo(customerNo);
                        account.setAppid(UserContext.getAppid());
                        String identifier = "";
                        //????????????????????????
                        Result<String> result = accountService.getIdentifier(account);
                        if (result.isSuccess()) {
                            identifier = result.getResult();
                        }
                        //??????????????????
                        return new ResultUtil<String>().setData(identifier);
                    }

                } else {
                    Account account = new Account();
                    account.setCustomerNo(customerNo);
                    account.setAppid(UserContext.getAppid());
                    String identifier = "";
                    //????????????????????????
                    Result<String> result = accountService.getIdentifier(account);
                    if (result.isSuccess()) {
                        identifier = result.getResult();
                    }
                    //?????????????????????
                    return new ResultUtil<String>().setData(identifier);
                }
            }
        } else {
            //???????????????????????????????????????
            Account account = new Account();
            account.setCustomerNo(customerNo);
            account.setAppid(UserContext.getAppid());
            String identifier = "";
            //????????????????????????
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
    @ApiOperation(value = "???????????????")
    @ResponseBody
    public Result<String> changeCertNo(@RequestParam String phone, @RequestParam String certNo,HttpServletRequest request) throws ParseException {
        String decryptPhone= AESUtil.decrypt4v1(phone);
        if (StrUtil.isEmpty(decryptPhone)) {
            return new ResultUtil<String>().setErrorMsg("????????????");
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
    @ApiOperation(value = "??????coreAccountId???????????????????????????")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<String> queryIdentifierByCoreAccountId(@RequestParam String accountId) {
        String identifier = "";
        //????????????????????????
        AccountForm accountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(), true);
        if (null != accountForm) {
            //???????????????????????????????????????????????????
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
    @ApiOperation(value = "?????????????????????????????????")
    @APIModifier(APIModifierType.PRIVATE)
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteByCoreAccountId (@RequestParam String coreAccountId) {
        Account account = accountService.get(coreAccountId);
        if ( null == account ) {
            return new ResultUtil<String>().setErrorMsg("????????????????????????");
        }
        List<String> list = new ArrayList<>();
        list.add(coreAccountId);
        //??????????????????????????????
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
    @ApiOperation(value = "????????????")
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
        paramMap.remove("appsecret"); //appsecret??????????????????????????????????????????
        String params = HttpUtil.toParams(paramMap);
        String url = "https://inndoo.ytdcloud.com/core-api/whitelist/pushListRecord"+ "?" + params;
        String resultStr = HttpRequestUtil.get(url);*//*
       // return new ResultUtil<String>().setData("");
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ApiOperation(value = "????????????")
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
        paramMap.remove("appsecret"); //appsecret??????????????????????????????????????????
        String params = HttpUtil.toParams(paramMap);
        String url = "http://localhost:5031/core-api/whitelist/pushListRecord"+ "?" + params;
        String resultStr = HttpRequestUtil.get(url);
        return new ResultUtil<String>().setData(resultStr);
    }

    @RequestMapping(value = "/test2", method = RequestMethod.GET)
    @ApiOperation(value = "????????????")
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
        paramMap.remove("appsecret"); //appsecret??????????????????????????????????????????
        String params = HttpUtil.toParams(paramMap);
        String url = "https://inndoo.ytdcloud.com/core-api/whitelist/pushListRecord"+ "?" + params;
        long startTime = System.currentTimeMillis();    //??????????????????
        String resultStr = HttpRequestUtil.get(url);
        long endTime = System.currentTimeMillis();    //??????????????????
        System.out.println("?????????????????????" + (endTime - startTime) + "ms");    //????????????????????????
        return new ResultUtil<String>().setData(resultStr);
    }
*/




    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ApiOperation(value = "????????????")
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
        paramMap.remove("appsecret"); //appsecret??????????????????????????????????????????
        String params = HttpUtil.toParams(paramMap);
        String url = "http://dev.inndoo.ytdinfo.com.cn/activity-api/weixin/pushListRecord"+ "?" + params;
        String resultStr = HttpRequestUtil.get(url);
        return new ResultUtil<String>().setData(resultStr);
    }

    @RequestMapping(value = "/countByCreateTime", method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????????????????")
    @APIModifier(APIModifierType.PRIVATE)
    public Result<Integer> countByCreateTime(@RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime) {
        Integer count = iAccountService.countByCreateTime(startTime, endTime);
        return new ResultUtil<Integer>().setData(count);
    }
}
