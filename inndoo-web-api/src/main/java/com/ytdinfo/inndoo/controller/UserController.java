package com.ytdinfo.inndoo.controller;

import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhuzheng
 */
@Slf4j
@RestController
@Api(description = "系统用户接口")
@RequestMapping("/user")

@APIModifier(APIModifierType.PRIVATE)
public class UserController {

    @Autowired
    private RabbitUtil rabbitUtil;

    @RequestMapping(value = "/syncUser/{action}", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "从sso同步用户")
    public Result<String> syncUser(@RequestParam String message,@PathVariable String action) {
        MQMessage mqMessage = JSONUtil.toBean(message,MQMessage.class);
        rabbitUtil.sendToExchange(QueueEnum.QUEUE_USER_EVENT_MSG.getExchange(), "", mqMessage, action);
        return new ResultUtil<String>().setData("success");
    }

}
