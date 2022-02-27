package com.ytdinfo.inndoo.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.HibernateProxyTypeAdapter;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.ModifyDepartmentVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @ClassName DepartmentController
 * @Description: 部门
 * @Author: zl
 * @DATE 2019/10/30
 * @TIME 13:02
 * @Version 1.0
 */
@Slf4j
@RestController
@Api(description = "部门接口")
@RequestMapping("/department")

@APIModifier(APIModifierType.PUBLIC)
public class DepartmentController {

    @Autowired
    private IStaffRoleService iStaffRoleService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据")
    public Result<List<Department>> listAll() {
        List<Department> list =departmentService.findAllToTree();
        Collator collator = Collator.getInstance(Locale.CHINESE);
        list.sort((o1, o2) -> collator.compare(o1.getTitle(), o2.getTitle()));
        return new ResultUtil<List<Department>>().setData(list);
    }

    @RequestMapping(value = "/countLevel", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取当前账号下机构层级")
    public Result<Integer> countLevel() {
        Integer count =  departmentService.countLevel();
        return new ResultUtil<Integer>().setData(count);
    }

    @RequestMapping(value = "/listall", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据简单格式")
    public Result<List<Department>> listAllSimple() {
        List<Department> list =departmentService.findAll();
        Collator collator = Collator.getInstance(Locale.CHINESE);
        list.sort((o1, o2) -> collator.compare(o1.getTitle(), o2.getTitle()));
        return new ResultUtil<List<Department>>().setData(list);
    }


    //同步小核心部门数据
    @RequestMapping(value = "/synCoreDepartmentlog", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据简单格式")
    public Result<String> synCoreDepartmentlog(String wxappid) {
        List<Department> list =departmentService.findByAppid(wxappid);
        for(Department department:list){
            ModifyDepartmentVo modifyDepartmentVo = departmentService.getModiifyDepartment(department.getId());
            //推到活动平台
            modifyDepartmentVo.setType("initial");
            activityApiUtil.modiifyDepartment(modifyDepartmentVo);
        }
        return new ResultUtil<String>().setData("OK");
    }


    //同步小核心部门数据
    @RequestMapping(value = "/synCoreDepartmentlog1", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据简单格式")
    public Result<List<ModifyDepartmentVo>> synCoreDepartmentlog1(String wxappid) {
        List<Department> list =departmentService.findByAppid(wxappid);
        List<ModifyDepartmentVo> departmentlist=new ArrayList<ModifyDepartmentVo>();
        for(Department department:list){
            ModifyDepartmentVo modifyDepartmentVo = departmentService.getModiifyDepartment(department.getId());
            departmentlist.add(modifyDepartmentVo);
        }
        return new ResultUtil<List<ModifyDepartmentVo>>().setData(departmentlist);

    }




    //同步小核心部门数据
    @RequestMapping(value = "/synCoreDepartment", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据简单格式")
    public Result<List<Department>> synCoreDepartment(String wxappid) {
        List<Department> list =departmentService.findByAppid(wxappid);
        return new ResultUtil<List<Department>>().setData(list);
    }


    @RequestMapping(value = "/getListByIds", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据ids列表获取全部数据")
    public Result<List<Department>> getListByIds(@RequestParam String coreDepartmentIds){
        JSONArray jsonArray = JSONUtil.parseArray(coreDepartmentIds);
        List<String> departmentIds = JSONUtil.toList(jsonArray,String.class);
        List<Department> departments = new ArrayList<>();
        if(null != departmentIds && departmentIds.size() > 0) {
            departments = departmentService.findByIdIn(departmentIds);
        }
        return new ResultUtil<List<Department>>().setData(departments);
    }

    @RequestMapping(value = "/getTopList", method = RequestMethod.GET)
    @ApiOperation(value = "通过获取顶级机构")
    @SystemLog(description = "查看顶级机构列表")
    public Result<List<Department>> getTopList(){
        List<Department> list = departmentService.findByAppidAndParentIdAndStatus(UserContext.getAppid(),"0", 0);
        return new ResultUtil<List<Department>>().setData(list);
    }

    @RequestMapping(value = "/listByParentId/{parentId}", method = RequestMethod.GET)
    @ApiOperation(value = "通过parentId获取")
    @SystemLog(description = "查看机构列表")
    public Result<List<Department>> getByParentId(@PathVariable String parentId){
        List<Department> list = departmentService.findByParentIdAndStatusOrderBySortOrder(parentId, 0);
        return new ResultUtil<List<Department>>().setData(list);
    }

    @RequestMapping(value = "/getContactTree", method = RequestMethod.GET)
    @ApiOperation(value = "获取拥有机构联系人的部门（树结构）")
    @SystemLog(description = "获取拥有机构联系人的部门（树结构）")
    public Result<List<Department>> getContactTree(){
        List<Department> departments = iStaffRoleService.findContactDept();
        List<String> filterIds = new ArrayList<>();
        for(Department tempDept: departments){
            filterIds.add(tempDept.getId());
        }
        List<Department> result =  departmentService.filterTree(filterIds);
        return new ResultUtil<List<Department>>().setData(result);
    }


    @RequestMapping(value = "/queryByCode/{code}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据ids列表获取全部数据")
    public Result<Department> queryByCode(@PathVariable String code){
        Department department =  departmentService.findByDeptCode(code);
        return new ResultUtil<Department>().setData(department);
    }


}
