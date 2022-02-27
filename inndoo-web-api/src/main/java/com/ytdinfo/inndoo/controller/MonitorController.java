package com.ytdinfo.inndoo.controller;

import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by timmy on 2020/8/20.
 */
@Slf4j
@Controller
@Api(description = "监控接口")
@RequestMapping("/monitor/")
public class MonitorController {

    @APIModifier(value = APIModifierType.PUBLIC)
    @ResponseBody
    @RequestMapping(value = "/index", method = RequestMethod.GET)
    @ApiOperation(value = "监控首页",position = 1)
    public String monitorIndex(){
        return "OK";
    }
}