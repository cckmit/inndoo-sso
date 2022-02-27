package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.MqException;
import com.ytdinfo.inndoo.modules.core.service.MqExceptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yaochangning
 */
@Slf4j
@RestController
@Api(description = "mq执行异常管理接口")
@RequestMapping("/mqException")
public class MqExceptionController extends BaseController<MqException, String> {

    @Autowired
    private MqExceptionService mqExceptionService;

    @Override
    public MqExceptionService getService() {
        return mqExceptionService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<MqException>> listByCondition(@ModelAttribute MqException mqException,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<MqException> page = mqExceptionService.findByCondition(mqException, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<MqException>>().setData(page);
    }

}
