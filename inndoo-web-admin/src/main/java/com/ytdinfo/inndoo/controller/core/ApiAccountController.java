package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.ApiAccount;
import com.ytdinfo.inndoo.modules.core.service.ApiAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "API用户帐号管理管理接口")
@RequestMapping("/apiaccount")
public class ApiAccountController extends BaseController<ApiAccount, String> {

    @Autowired
    private ApiAccountService apiAccountService;

    @Override
    public ApiAccountService getService() {
        return apiAccountService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<ApiAccount>> listByCondition(@ModelAttribute ApiAccount apiAccount,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<ApiAccount> page = apiAccountService.findByCondition(apiAccount, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<ApiAccount>>().setData(page);
    }

}
