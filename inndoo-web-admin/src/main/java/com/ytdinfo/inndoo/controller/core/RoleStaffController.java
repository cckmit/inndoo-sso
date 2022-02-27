package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.common.vo.TenantStaffRoleLimitVo;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import com.ytdinfo.inndoo.modules.core.service.RoleStaffService;
import com.ytdinfo.inndoo.modules.core.service.StaffRoleService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IRoleStaffService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nolan
 */
@Slf4j
@RestController
@Api(description = "角色（员工）管理接口")
@RequestMapping("/rolestaff")
public class RoleStaffController extends BaseController<RoleStaff, String> {

    @Autowired
    private RoleStaffService roleStaffService;

    @Autowired
    private IRoleStaffService iRoleStaffService;

    @Autowired
    private StaffRoleService staffRoleService;

    @Autowired
    private IStaffRoleService iStaffRoleService;
    @Autowired
    private MatrixApiUtil matrixApiUtil;

    @Override
    public RoleStaffService getService() {
        return roleStaffService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<RoleStaff>> listByCondition(@ModelAttribute RoleStaff roleStaff,
                                                   @ModelAttribute SearchVo searchVo,
                                                   @ModelAttribute PageVo pageVo) {

        Page<RoleStaff> page = roleStaffService.findByCondition(roleStaff, searchVo, PageUtil.initPage(pageVo));
        List<RoleStaff> roleStaffList = page.getContent();
        for(RoleStaff rs: roleStaffList){
            List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(rs.getId());
            if(CollectionUtil.isNotEmpty(staffRoles)){
                rs.setAlreadySize(staffRoles.size() + "");
            } else {
                rs.setAlreadySize("0");
            }
            TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName(rs.getName());
            if(null != selfCustomFormRole){
                if(!selfCustomFormRole.getEnableLimit()){
                    rs.setLimitSize(selfCustomFormRole.getLimitSize()+"");
                } else {
                    rs.setLimitSize("无限制");
                }
            }else {
                rs.setLimitSize("无限制");
            }
        }
        return new ResultUtil<Page<RoleStaff>>().setData(page);
    }


    @RequestMapping(value = "/setDefault", method = RequestMethod.POST)
    @ApiOperation(value = "设置或取消默认角色")
    public Result<Object> setDefault(@RequestParam String id,
                                     @RequestParam Boolean isDefault) {

        RoleStaff role = roleStaffService.get(id);
        if (role == null) {
            return new ResultUtil<Object>().setErrorMsg("角色不存在");
        }
        role.setDefaultRole(isDefault);
        roleStaffService.update(role);
        return new ResultUtil<Object>().setSuccessMsg("设置成功");
    }

    @RequestMapping(value = "/initRole", method = RequestMethod.POST)
    @ApiOperation(value = "初始化创建用户角色")
    public Result<Object> initRole() {
        RoleStaff contact = iRoleStaffService.findByCode("STAFF_CONTACTS");
        if (contact != null) {
            if (!StrUtil.equals(contact.getName(), "机构联系人")) {
                contact.setName("机构联系人");
                roleStaffService.update(contact);
            }
        } else {
            contact = new RoleStaff();
            contact.setName("机构联系人");
            contact.setCode("STAFF_CONTACTS");
            contact.setDescription("机构联系人");
            contact.setDefaultRole(false);
            roleStaffService.save(contact);
        }

        RoleStaff normal = iRoleStaffService.findByCode("STAFF_NORMAL");
        if (normal != null) {
            if (!StrUtil.equals(normal.getName(), "普通员工")) {
                normal.setName("普通员工");
                roleStaffService.update(normal);
            }
        } else {
            normal = new RoleStaff();
            normal.setName("普通员工");
            normal.setCode("STAFF_NORMAL");
            normal.setDescription("普通员工");
            normal.setDefaultRole(false);
            roleStaffService.save(normal);
        }

        return new ResultUtil<Object>().setSuccessMsg("设置成功");
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ApiOperation(value = "添加")
    @Override
    public Result<RoleStaff> create(@ModelAttribute RoleStaff roleStaff) {
        // 判断拦截请求的操作权限按钮名是否已存在
        List<RoleStaff> list1 = roleStaffService.findByName(roleStaff.getName());
        if (list1 != null && list1.size() > 0) {
            return new ResultUtil<RoleStaff>().setErrorMsg("角色名称已存在");
        }

        List<RoleStaff> list2 = roleStaffService.findByCode(roleStaff.getCode());
        if (list2 != null && list2.size() > 0) {
            return new ResultUtil<RoleStaff>().setErrorMsg("角色编码已存在");
        }
        RoleStaff u = roleStaffService.save(roleStaff);
        return new ResultUtil<RoleStaff>().setData(u);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ApiOperation(value = "编辑")
    @Override
    public Result<RoleStaff> update(@ModelAttribute RoleStaff roleStaff) {

        RoleStaff role = roleStaffService.get(roleStaff.getId());
        if (!StrUtil.equals(role.getName(), roleStaff.getName())) {
            // 判断拦截请求的操作权限按钮名是否已存在
            List<RoleStaff> list1 = roleStaffService.findByName(roleStaff.getName());
            if (list1 != null && list1.size() > 0) {
                return new ResultUtil<RoleStaff>().setErrorMsg("角色名称已存在");
            }
        }
        if (!StrUtil.equals(role.getCode(), roleStaff.getCode())) {
            List<RoleStaff> list2 = roleStaffService.findByCode(roleStaff.getCode());
            if (list2 != null && list2.size() > 0) {
                return new ResultUtil<RoleStaff>().setErrorMsg("角色编码已存在");
            }
        }
        RoleStaff u = roleStaffService.save(roleStaff);
        return new ResultUtil<RoleStaff>().setData(u);
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id删除数据")
    public Result<Object> deleteById(@PathVariable  String id){
        List<StaffRole>  staffRoles=  staffRoleService.findByRoleId(id);
        if(staffRoles.size()>0){
            return new ResultUtil<Object>().setErrorMsg("用户角色绑定了其他用户，请先解绑后在操作");
        }
        getService().delete(id);
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }

    @RequestMapping(value = "/batch_delete_check/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量删除")
    public Result<Map<String, Object>>  batchDeleteByIdsCheck(@PathVariable String[] ids){
        List<String> failName = new ArrayList<>();
        List<String> successName = new ArrayList<>();
        List<String> deleteIds = new ArrayList<>();
        for(String id:ids){
            List<StaffRole>  staffRoles=  staffRoleService.findByRoleId(id);
            if(staffRoles.size()>0){
               RoleStaff roleStaff = roleStaffService.get(id);
               if(roleStaff != null){
                   failName.add(roleStaff.getName());
               }
            }else{
                RoleStaff roleStaff = roleStaffService.get(id);
                if(roleStaff != null){
                    successName.add(roleStaff.getName());
                }
                deleteIds.add(id);
            }
        }
        for(String id:deleteIds){
            getService().delete(id);
        }
        Map<String,Object> map = new HashMap<>();
        map.put("failName", failName);
        map.put("successName", successName);
        return new ResultUtil<Map<String, Object>>().setData(map);
    }

}
