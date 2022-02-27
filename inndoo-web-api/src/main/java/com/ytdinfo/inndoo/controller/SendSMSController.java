package com.ytdinfo.inndoo.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.context.UserContext;
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
@Api(description = "发送短信接口")
@RequestMapping("/sms")

@APIModifier(APIModifierType.PRIVATE)
public class SendSMSController {

    @Autowired
    private RabbitUtil rabbitUtil;

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "发送短信")
    public Result<String> send(@RequestParam String message) {
        MQMessage mqMessage = JSONUtil.toBean(message,MQMessage.class);
        rabbitUtil.sendToExchange(rabbitUtil.getExchageName(StrUtil.EMPTY, QueueEnum.QUEUE_SEND_SMS_EVENT_MSG), "", mqMessage);
        return new ResultUtil<String>().setData("success");
    }

}
