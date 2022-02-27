package com.ytdinfo.inndoo.controller;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformation;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author timmy
 * @date 2019/10/16
 */
@Slf4j
@RestController
@Api(description = "客户信息校验接口")
@RequestMapping("/account/validate")

@APIModifier(APIModifierType.PRIVATE)
public class AccountValidateController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private CustomerInformationService customerInformationService;

    @RequestMapping(value = "/validateOrg", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验分行编码")
    public Result<String> validateOrg(@RequestParam String accountId, @RequestParam String orgId) {
        Account account = accountService.get(accountId);
        ResultUtil<String> resultUtil = new ResultUtil<>();
        if (account == null) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String identifier = account.getIdentifier();
        CustomerInformation customerInformation = customerInformationService.findByIdentifier(identifier);
        if (customerInformation == null) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        //分行编码
        String bankBranchNo = customerInformation.getBankBranchNo();
        if (StrUtil.isEmpty(bankBranchNo)) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String[] splitOrgList = orgId.split(",");
        boolean result = false;
        for (String id : splitOrgList) {
            if (StrUtil.equalsIgnoreCase(bankBranchNo, id)) {
                result = true;
                break;
            }
        }
        if (result) {
            return resultUtil.setData(CommonConstant.RESULT_YES);
        } else {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }

    }

    @RequestMapping(value = "/validateBranch", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<String> validateRecord(@RequestParam String accountId, @RequestParam String branchId) {
        Account account = accountService.get(accountId);
        ResultUtil<String> resultUtil = new ResultUtil<>();
        if (account == null) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String identifier = account.getIdentifier();
        CustomerInformation customerInformation = customerInformationService.findByIdentifier(identifier);
        if (customerInformation == null) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        //机构编码
        String institutionalCode = customerInformation.getInstitutionalCode();
        if (StrUtil.isEmpty(institutionalCode)) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String[] splitList = branchId.split(",");
        boolean result = false;
        for (String id : splitList) {
            if (StrUtil.equalsIgnoreCase(institutionalCode, id)) {
                result = true;
                break;
            }
        }
        if (result) {
            return resultUtil.setData(CommonConstant.RESULT_YES);
        } else {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }

    }

}