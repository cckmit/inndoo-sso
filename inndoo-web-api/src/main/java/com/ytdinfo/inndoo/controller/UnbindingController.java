package com.ytdinfo.inndoo.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.SettingConstant;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SettingUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.vo.UnbindingSetting;
import com.ytdinfo.inndoo.modules.core.service.BindLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;


/**
 * @author zhuzheng
 */
@Slf4j
@RestController
@Api(description = "解绑接口")
@RequestMapping("/unbind")

@APIModifier(APIModifierType.PRIVATE)
public class UnbindingController {

    @Autowired
    private BindLogService bindLogService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SettingUtil settingUtil;

    @RequestMapping(value = "/unbind/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "解绑")
    public Result<String> unbind(@PathVariable String accountId) {
        String v = redisTemplate.opsForValue().get(SettingConstant.UNBINDING_SETTING);
        if (StrUtil.isBlank(v)) {
            v = settingUtil.getSettingValue(SettingConstant.UNBINDING_SETTING);
        }
        Result<String> result;
        if (StrUtil.isBlank(v)) {
//            result = new ResultUtil<String>().setErrorMsg("未找到解绑相关配置信息");
//            result.setResult("");
//            return result;
            result = bindLogService.unbind(accountId, null);
            result.setResult("");
            return result;
        }
        UnbindingSetting setting = JSONUtil.toBean(v, UnbindingSetting.class);
        if (setting == null) {
            result = new ResultUtil<String>().setErrorMsg("未找到解绑相关配置信息");
            result.setResult("");
            return result;
        }

        result = bindLogService.unbind(accountId, setting);
        result.setResult("");
        return result;
    }

    @RequestMapping(value = "/unbindNonLog/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "解绑")
    public Result<String> unbindNonLog(@PathVariable String accountId) {
        Result<String> result = bindLogService.unbind(accountId, null);
        result.setResult("");
        return result;
    }

    /***
     * 只解绑微信端用户
     * @param accountId
     * @return
     */
    @RequestMapping(value = "/unbindNonLog2/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "解绑2")
    public Result<String> unbindNonLog2(@PathVariable String accountId) {
        Result<String> result = bindLogService.unbind2(accountId);
        result.setResult("");
        return result;
    }


}