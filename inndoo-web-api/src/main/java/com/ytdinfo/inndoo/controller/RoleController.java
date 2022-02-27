package com.ytdinfo.inndoo.controller;

import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.service.RoleService;
import com.ytdinfo.inndoo.modules.base.service.UserRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yangyang
 */
@Slf4j
@RestController
@Api(description = "系统角色接口")
@RequestMapping("/role")

@APIModifier(APIModifierType.PRIVATE)
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "获取角色列表")
    public Result<List<Role>> list() {
        List<Role> list = roleService.findAll();
        return new ResultUtil<List<Role>>().setData(list);
    }

    @RequestMapping(value = "/userRoleList", method = RequestMethod.GET)
    @ApiOperation(value = "获取用户角色列表")
    public Result<List<Role>> userRoleList(@RequestParam String userId) {
        return new ResultUtil<List<Role>>().setData(userRoleService.findRoleByUserId(userId));
    }

}
