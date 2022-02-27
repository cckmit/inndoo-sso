package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.SmsCaptchaLog;
import com.ytdinfo.inndoo.modules.core.service.SmsCaptchaLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nolan
 */
@Slf4j
@RestController
@Api(description = "手机短信验证码日志管理接口")
@RequestMapping("/smscaptchalog")
public class SmsCaptchaLogController extends BaseController<SmsCaptchaLog, String> {

    @Autowired
    private SmsCaptchaLogService smsCaptchaLogService;

    @Override
    public SmsCaptchaLogService getService() {
        return smsCaptchaLogService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<SmsCaptchaLog>> listByCondition(@ModelAttribute SmsCaptchaLog smsCaptchaLog,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<SmsCaptchaLog> page = smsCaptchaLogService.findByCondition(smsCaptchaLog, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<SmsCaptchaLog>>().setData(page);
    }

}
