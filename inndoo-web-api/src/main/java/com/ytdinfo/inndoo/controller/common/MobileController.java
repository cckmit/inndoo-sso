package com.ytdinfo.inndoo.controller.common;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "手机接口")
@RequestMapping("/base/common/mobile")
@APIModifier(APIModifierType.PUBLIC)
public class MobileController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     *
     * @param phone
     * @return
     */
    @RequestMapping(value = "/getCodeByPhone", method = RequestMethod.GET)
    @ApiOperation(value = "根据手机号获取短信验证码")
    @ResponseBody
    private Result<String> getCodeByPhone(@RequestParam String phone){
        // 验证短信验证码
        String v = redisTemplate.opsForValue().get(CommonConstant.PRE_SMS + phone);
        if (StrUtil.isBlank(v)) {
            return new ResultUtil<String>().setErrorMsg("验证码失效或验证码不正确");
        }else {
            return new ResultUtil<String>().setData(v);
        }
    }

    /**
     *
     * @param phone
     * @return
     */
    @RequestMapping(value = "/deleteByPhone", method = RequestMethod.GET)
    @ApiOperation(value = "根据手机号删除缓存中短信验证码")
    @ResponseBody
    private Result<String> deleteByPhone(@RequestParam String phone){
        // 删除短信验证码
        redisTemplate.delete(CommonConstant.PRE_SMS + phone);

        return new ResultUtil<String>().setData("删除成功");

    }
}
