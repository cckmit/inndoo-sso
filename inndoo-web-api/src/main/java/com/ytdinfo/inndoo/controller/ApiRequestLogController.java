package com.ytdinfo.inndoo.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IApiRequestLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "api请求日志接口")
@RequestMapping("/apiRequestLog")
@APIModifier(APIModifierType.PUBLIC)
public class ApiRequestLogController {

    @Autowired
    private IApiRequestLogService apiRequestLogService;


    @RequestMapping(value = "/removeLog")
    @ApiOperation(value = "删除日志")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> removeLog() {
        DateTime dateTime = DateUtil.offsetDay(DateUtil.beginOfDay(new Date()), -14);
        while (true){
            List<String> log4Delete = apiRequestLogService.find4Delete(dateTime);
            if(log4Delete.size() == 0){
                break;
            }
            apiRequestLogService.batchRemove(log4Delete);
        }
        return new ResultUtil<Object>().setSuccessMsg("清除成功");
    }
}
