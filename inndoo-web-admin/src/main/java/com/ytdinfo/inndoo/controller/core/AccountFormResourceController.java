package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormResource;
import com.ytdinfo.inndoo.modules.core.service.AccountFormResourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "注册页面ui资源管理管理接口")
@RequestMapping("/accountformresource")
public class AccountFormResourceController extends BaseController<AccountFormResource, String> {

    @Autowired
    private AccountFormResourceService accountFormResourceService;

    @Override
    public AccountFormResourceService getService() {
        return accountFormResourceService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<AccountFormResource>> listByCondition(@ModelAttribute AccountFormResource accountFormResource,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<AccountFormResource> page = accountFormResourceService.findByCondition(accountFormResource, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<AccountFormResource>>().setData(page);
    }

}
