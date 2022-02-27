package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.ThreadPoolUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.PhoneLocationService;
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
@RequestMapping("/phonelocation")

@APIModifier(APIModifierType.PRIVATE)
public class PhoneLocationController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private PhoneLocationService phoneLocationService;

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<String> validateRecord(@RequestParam String accountId, @RequestParam(defaultValue = "") String area) {
        ResultUtil<String> resultUtil = new ResultUtil<>();
        if (StringUtils.isEmpty(area)) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        Account account = accountService.get(accountId);
        if (account == null) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String phone = account.getPhone();
        if (StringUtils.isEmpty(phone)) {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
        String aesPhone = AESUtil.encrypt(phone);
        boolean eqProvince = false, eqCity = false;
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
            if (StrUtil.isNotEmpty(phoneLocation.getProvince()) && area.contains(phoneLocation.getProvince())) {
                eqProvince = true;
            }
            if (StrUtil.isNotEmpty(phoneLocation.getCity()) && area.contains(phoneLocation.getCity())) {
                eqCity = true;
            }
            String result = (eqProvince || eqCity) ? CommonConstant.RESULT_YES : CommonConstant.RESULT_NO;
            return resultUtil.setData(result);
        } else {
            return resultUtil.setData(CommonConstant.RESULT_NO);
        }
    }
}