package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;
import com.ytdinfo.inndoo.modules.core.service.ApiRequestLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Api(description = "会员账号管理接口")
@RequestMapping("/apirequestlog")
public class ApiRequestLogController extends BaseController<ApiRequestLog, String> {

    @Autowired
    private ApiRequestLogService apiRequestLogService;
    @Autowired
    private RabbitUtil rabbitUtil;
    @Override
    public ApiRequestLogService getService() {
        return apiRequestLogService;
    }

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<ApiRequestLog>> listByCondition(@ModelAttribute ApiRequestLog apiRequestLog,
                                          @ModelAttribute SearchVo searchVo,
                                          @ModelAttribute PageVo pageVo) {
        Page<ApiRequestLog> page = apiRequestLogService.findByCondition(apiRequestLog, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<ApiRequestLog>>().setData(page);
    }

    @RequestMapping(value = "/clearApiRequestLog", method = RequestMethod.DELETE)
    @ApiOperation(value = "全量删除apiRequest日志")
    @SystemLog(description = "全量删除apiRequest日志")
    public Result<Object> clearApiRequestLog() {
        MQMessage<String> mqMessage = new MQMessage<String>();
        rabbitUtil.sendToExchange(rabbitUtil.getExchageName(StrUtil.EMPTY, QueueEnum.QUEUE_CLEAR_API_LOG_MSG), "", mqMessage);
        return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
    }

}
