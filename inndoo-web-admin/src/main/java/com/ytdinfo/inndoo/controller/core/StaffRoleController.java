package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import com.ytdinfo.inndoo.modules.core.service.StaffRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * @author Nolan
 */
@Slf4j
@RestController
@Api(description = "员工-角色管理接口")
@RequestMapping("/staffrole")
public class StaffRoleController extends BaseController<StaffRole, String> {

    @Autowired
    private StaffRoleService staffRoleService;

    @Override
    public StaffRoleService getService() {
        return staffRoleService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<StaffRole>> listByCondition(@ModelAttribute StaffRole staffRole,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<StaffRole> page = staffRoleService.findByCondition(staffRole, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<StaffRole>>().setData(page);
    }

}
