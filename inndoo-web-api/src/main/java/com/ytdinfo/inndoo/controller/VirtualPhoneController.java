package com.ytdinfo.inndoo.controller;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author timmy
 * @date 2019/10/17
 */
@Slf4j
@RestController
@Api(description = "手机号码归属地接口")
@RequestMapping("/virtualphone")

@APIModifier(APIModifierType.PRIVATE)
public class VirtualPhoneController {

    @Autowired
    private AccountService accountService;
    @XxlConf("core.phone.whitelist.prefix")
    private String phoneWhitelistPrefix;

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<String> validateRecord(@RequestParam String accountId) {
        ResultUtil<String> resultUtil = new ResultUtil<>();
        Account account = accountService.get(accountId);
        if (account == null) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String phone = account.getPhone();
        if (StringUtils.isEmpty(phone)) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String[] whiteList = this.phoneWhitelistPrefix.split(",");
        boolean isInWhitelist = false;
        for (String w : whiteList) {
            if (StrUtil.startWith(phone, w)) {
                isInWhitelist = true;
                break;
            }
        }
        if (isInWhitelist) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        } else {
            return resultUtil.setData(CommonConstant.RESULT_YES);
        }
    }
}