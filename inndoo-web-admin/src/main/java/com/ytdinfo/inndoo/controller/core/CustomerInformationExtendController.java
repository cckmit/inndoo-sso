package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformationExtend;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationExtendService;
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
@Api(description = "客户信息拓展表管理接口")
@RequestMapping("/customerinformationextend")
public class CustomerInformationExtendController extends BaseController<CustomerInformationExtend, String> {

    @Autowired
    private CustomerInformationExtendService customerInformationExtendService;

    @Override
    public CustomerInformationExtendService getService() {
        return customerInformationExtendService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<CustomerInformationExtend>> listByCondition(@ModelAttribute CustomerInformationExtend customerInformationExtend,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<CustomerInformationExtend> page = customerInformationExtendService.findByCondition(customerInformationExtend, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<CustomerInformationExtend>>().setData(page);
    }

}
