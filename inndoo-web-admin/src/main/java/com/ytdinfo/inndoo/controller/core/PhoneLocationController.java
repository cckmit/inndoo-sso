package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import com.ytdinfo.inndoo.modules.core.service.PhoneLocationService;
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
@Api(description = "手机号码归属地管理接口")
@RequestMapping("/phonelocation")
public class PhoneLocationController extends BaseController<PhoneLocation, String> {

    @Autowired
    private PhoneLocationService phoneLocationService;

    @Override
    public PhoneLocationService getService() {
        return phoneLocationService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<PhoneLocation>> listByCondition(@ModelAttribute PhoneLocation phoneLocation,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<PhoneLocation> page = phoneLocationService.findByCondition(phoneLocation, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<PhoneLocation>>().setData(page);
    }

}
