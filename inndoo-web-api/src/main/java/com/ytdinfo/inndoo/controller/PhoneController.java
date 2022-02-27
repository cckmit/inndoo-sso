package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.ThreadPoolUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.ActAccountService;
import com.ytdinfo.inndoo.modules.core.service.PhoneLocationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhuzheng
 * @date 2020/07/30
 */
@Slf4j
@RestController
@Api(description = "手机号接口")
@RequestMapping("/phone")

@APIModifier(APIModifierType.PRIVATE)
public class PhoneController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ActAccountService actAccountService;

    @RequestMapping(value = "/query/{actAccountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<String> query(@PathVariable String actAccountId) {
        ResultUtil<String> resultUtil = new ResultUtil<>();
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if (actAccount == null) {
            return resultUtil.setErrorMsg(CommonConstant.RESULT_NO);
        }
        String coreAccountId = actAccount.getCoreAccountId();
        if (StringUtils.isEmpty(coreAccountId)) {
            return resultUtil.setErrorMsg(CommonConstant.RESULT_NO);
        }
        Account account = accountService.get(coreAccountId);
        if (account == null) {
            return resultUtil.setErrorMsg(CommonConstant.RESULT_NO);
        }
        String phone = account.getPhone();
        if (StringUtils.isEmpty(phone)) {
            return resultUtil.setErrorMsg(CommonConstant.RESULT_NO);
        }
        //return resultUtil.setData(AESUtil.encrypt(phone));
        //使用通用加密出去
        return resultUtil.setData(AESUtil.comEncrypt(phone));
    }
}