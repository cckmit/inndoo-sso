package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.ThreadPoolUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.AccountFormService;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationService;
import com.ytdinfo.inndoo.modules.core.service.PhoneLocationService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IBindLogService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ISmsCaptchaLogService;
import com.ytdinfo.inndoo.vo.RegisterVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Api(description = "运维助手")
@RequestMapping("/operationhelper")
@APIModifier(APIModifierType.PRIVATE)
public class OperationHelperController {

    @XxlConf("core.phone.whitelist.prefix")
    private String phoneWhitelistPrefix;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PhoneLocationService phoneLocationService;

    @Autowired
    private CustomerInformationService customerInformationService;

    @Autowired
    private ISmsCaptchaLogService iSmsCaptchaLogService;

    @Autowired
    private IBindLogService iBindLogService;

    @Autowired
    private AccountFormService accountFormService;

    @RequestMapping(value = "/registerInfo", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "注册信息")
    public Result<RegisterVo> registerInfo(@RequestParam String coreAccountId, String simple) {
        RegisterVo registerVo = new RegisterVo();
        Account account = accountService.get(coreAccountId);
        if (account != null) {
            String phone = account.getPhone();
            registerVo.setPhone(phone);
            registerVo.setRegisterDate(account.getCreateTime());
            String[] whiteList = this.phoneWhitelistPrefix.split(",");
            boolean isInWhitelist = false;
            for (String w : whiteList) {
                if (StrUtil.startWith(phone, w)) {
                    isInWhitelist = true;
                    break;
                }
            }
            registerVo.setVirtualPhone(isInWhitelist ? "否" : "是");
            if ("N".equals(simple)) {
                String aesPhone = AESUtil.encrypt(phone);
                PhoneLocation phoneLocation = phoneLocationService.getByPhone(aesPhone);
                if (phoneLocation == null) {
                    phoneLocation = new PhoneLocation();
                    PhoneLocation phoneLocationFromApi = null;
                    try {
                        phoneLocationFromApi = phoneLocationService.getPhoneLocationFromApi(phone);
                        CopyOptions copyOptions = CopyOptions.create().ignoreNullValue();
                        BeanUtil.copyProperties(phoneLocationFromApi, phoneLocation, copyOptions);
                        final PhoneLocation finalPhoneLocation = phoneLocation;
                        ThreadPoolUtil.getPool().execute(() -> {
                            phoneLocationService.save(finalPhoneLocation);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (phoneLocation != null) {
                    registerVo.setLocation(phoneLocation.getProvince() + phoneLocation.getCity());
                    registerVo.setCompany(phoneLocation.getCompany());
                }
                String identifier = account.getIdentifier();
                if (StrUtil.isNotEmpty(identifier)) {
                    CustomerInformation customerInformation = customerInformationService.findByIdentifier(identifier);
                    if (customerInformation != null) {
                        registerVo.setBocWlh(customerInformation.getBankBranchNo());
                        registerVo.setBocOrgCode(customerInformation.getInstitutionalCode());
                        registerVo.setCusNo(customerInformation.getCustomerNo());
                    }
                }
            }
        }
        return new ResultUtil<RegisterVo>().setData(registerVo);
    }

    @RequestMapping(value = "/registerMsg", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "注册页信息")
    public Result<RegisterVo> registerMsg(@RequestParam String formId) {
        AccountForm accountForm = accountFormService.get(formId);
        RegisterVo vo = new RegisterVo();
        if(accountForm != null){
            vo.setFormType(accountForm.getFormType());
        }
        return new ResultUtil<RegisterVo>().setData(vo);
    }

    @RequestMapping(value = "/sendSmsLog", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "发短信记录")
    public Result<IPage<SmsCaptchaLogVo>> sendSmsLog(@RequestBody SmsSendLogSearchVo searchVo) {
        searchVo.getPageVo().setSort("create_time");
        searchVo.getPageVo().setOrder("desc");
        searchVo.setPhone(AESUtil.encrypt(searchVo.getPhone()));
        IPage<SmsCaptchaLogVo> result = iSmsCaptchaLogService.listForHelper(searchVo);
        List<SmsCaptchaLogVo> records = result.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            for (SmsCaptchaLogVo record : records) {
                Integer sendStatus = record.getSendStatus();
                if (sendStatus != null) {
                    if (sendStatus.intValue() == 1) {
                        record.setSendStatusDesc("发送成功");
                    } else if (sendStatus.intValue() == -1) {
                        record.setSendStatusDesc("发送失败");
                    }
                }
            }
        }
        return new ResultUtil<IPage<SmsCaptchaLogVo>>().setData(result);
    }

    @RequestMapping(value = "/bindLog", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "绑定日志")
    public Result<IPage<BindLogVo>> bindLog(@RequestBody BindLogSearchVo searchVo) {
        searchVo.getPageVo().setSort("create_time");
        searchVo.getPageVo().setOrder("desc");
        IPage<BindLogVo> result = iBindLogService.listForHelper(searchVo);
        List<BindLogVo> records = result.getRecords();
        for (BindLogVo record : records) {
            record.setPhone(AESUtil.decrypt(record.getPhone()));
            if (Boolean.TRUE.equals(record.getIsBind())) {
                record.setBindDesc("绑定");
            } else {
                record.setBindDesc("解绑");
            }

        }
        return new ResultUtil<IPage<BindLogVo>>().setData(result);
    }
}
