package com.ytdinfo.inndoo.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffService;
import com.ytdinfo.inndoo.vo.ZhengZhouAccountInputVo;
import com.ytdinfo.inndoo.vo.ZhengZhouAccountOutVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Api(description = "郑州银行专用会员接口")
@RequestMapping("/zhengzhouaccount")
@APIModifier(APIModifierType.PUBLIC)
public class ZhengZhouAccountController {
    @Autowired
    private IStaffService iStaffService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private StaffService staffService;
    @Autowired
    private ActivityApiUtil activityApiUtil;
    @Autowired
    private AccountFormMetaService accountFormMetaService;

    /**
     * @param zhengZhouAccountInputVo
     * @return
     */
    @RequestMapping(value = "/saveAndUpdate", method = RequestMethod.POST)
    @ApiOperation(value = "郑州银行账户注册")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<ZhengZhouAccountOutVo> saveAndUpdate(@RequestBody ZhengZhouAccountInputVo zhengZhouAccountInputVo, @RequestParam(required = false) String customerNo, @RequestParam(required = false) String code, HttpServletRequest request,@RequestParam String returnUrl) {
        ZhengZhouAccountOutVo zhengZhouAccountOutVo = new ZhengZhouAccountOutVo();
        if(StrUtil.isBlank(zhengZhouAccountInputVo.getActAccountId())){
            return new ResultUtil<ZhengZhouAccountOutVo>().setErrorMsg("活动平台账户必传");
        }
        if(StrUtil.isBlank(zhengZhouAccountInputVo.getOpenId())){
            return new ResultUtil<ZhengZhouAccountOutVo>().setErrorMsg("微信openId必传");
        }
        String actAccountId =AESUtil.decrypt(zhengZhouAccountInputVo.getActAccountId(), AESUtil.WXLOGIN_PASSWORD);
        String openId = zhengZhouAccountInputVo.getOpenId();
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        Account account = new Account();
        if( null != actAccount){
            Account copyAccount = accountService.get(actAccount.getCoreAccountId());
            if(null != copyAccount ){
                BeanUtils.copyProperties(copyAccount,account);
            }
        }
        // 调银行获取银行客户号
        if(StrUtil.isBlank(customerNo)){
            //调银行接口
            customerNo = "";
        }
        account.setCustomerNo(customerNo);
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
                        ReflectUtil.setFieldValue(account, accountFormMeta.getMetaType(),String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                        return new ResultUtil<ZhengZhouAccountOutVo>().setErrorMsg(accountFormMeta.getTitle() + "是唯一标识控件，必须有值");
                    }else {
                        if(StrUtil.isBlank(object.toString().trim())){
                            //没有赋值给随机值
                            ReflectUtil.setFieldValue(account, accountFormMeta.getMetaType(),String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                        }
                    }
                }
            }
        } else {
            return new ResultUtil<ZhengZhouAccountOutVo>().setErrorMsg("必须有用户识别控件");
        }

        String identifier = "";
        //拼接用户唯一标识
        Result<String> result = accountService.getIdentifier(account);
        if (result.isSuccess()) {
            identifier = result.getResult();
        } else {
            return new ResultUtil<ZhengZhouAccountOutVo>().setErrorMsg(result.getMessage());
        }
        account = accountService.save(account);
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
            return new ResultUtil<ZhengZhouAccountOutVo>().setErrorMsg("注册失败，请稍后重试");
        } else if (!bindAccountResult.isSuccess()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResultUtil<ZhengZhouAccountOutVo>().setErrorMsg("注册失败，您的注册信息已绑定其他账户");
        }

        if (null != actAccount && !actAccount.getCoreAccountId().equals(account.getId())) {
            actAccountService.delete(actAccount);
            actAccount.setActAccountId(actAccountId);
            actAccount.setCoreAccountId(coreAccountId);
            actAccountService.save(actAccount);
        }
        if(null ==actAccount ){
            actAccount = new ActAccount();
            actAccount.setActAccountId(actAccountId);
            actAccount.setCoreAccountId(coreAccountId);
            actAccountService.save(actAccount);
        }

        Boolean register = true;
        zhengZhouAccountOutVo.setRegister(register);
        return new ResultUtil<ZhengZhouAccountOutVo>().setData(zhengZhouAccountOutVo);
    }

    /**
     *  更新用户客户号
     * @param actAccountId
     * @param customerNo
     * @param request
     * @return
     */
    @RequestMapping(value = "/updateAccountCustomerNo", method = RequestMethod.GET)
    @ApiOperation(value = "账户更换customerNo")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    @APIModifier(APIModifierType.PRIVATE)
    public Result<Boolean> updateAccountCustomerNo(@RequestParam String actAccountId,@RequestParam String customerNo,HttpServletRequest request) {
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId );
        if (null != actAccount) {
            Account account = accountService.get(actAccount.getCoreAccountId());
            if(null != account){
                if(StrUtil.isBlank(account.getCustomerNo())){
                    account.setCustomerNo(customerNo);
                    accountService.save(account);
                } else {
                    if(!account.getCustomerNo().equals(customerNo)){
                        account.setCustomerNo(customerNo);
                        accountService.save(account);
                    }
                }
            }
        }
        return new ResultUtil<Boolean>().setData(true);
    }

}
