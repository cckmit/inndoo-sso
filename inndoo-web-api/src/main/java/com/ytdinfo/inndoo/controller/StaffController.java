package com.ytdinfo.inndoo.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.YReflectUtil;
import com.ytdinfo.inndoo.common.vo.BusinessManagerVo;
import com.ytdinfo.inndoo.common.vo.ModifyStaffVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.core.dto.SearchStaffDto;
import com.ytdinfo.inndoo.modules.core.dto.StaffDto;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationService;
import com.ytdinfo.inndoo.modules.core.service.RoleStaffService;
import com.ytdinfo.inndoo.modules.core.service.StaffService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffRoleService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffService;
import com.ytdinfo.inndoo.modules.core.serviceimpl.StaffServiceImpl;
import com.ytdinfo.inndoo.vo.IsStaffVo;
import com.ytdinfo.inndoo.vo.StaffVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Struct;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author timmy
 * @date 2019/10/16
 */
@Slf4j
@RestController
@Api(description = "员工接口")
@RequestMapping("/staff")

@APIModifier(APIModifierType.PUBLIC)
public class StaffController {

    @Autowired
    private StaffService staffService;

    @Autowired
    private IStaffService iStaffService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private IStaffRoleService iStaffRoleService;

    @Autowired
    private CustomerInformationService customerInformationService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @Autowired
    private StaffServiceImpl staffServiceImpl;

    @Autowired
    private RoleStaffService roleStaffService;

    /**
     * 业务经理角色code
     */
    private final static String STAFF_BUSINESS_MANAGER = "STAFF_BUSINESS_MANAGER";

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "校验数据")
    public Result<String> validateRecord(@RequestParam String accountId) {
        boolean exist = staffService.validate(accountId);
        String result = exist ? CommonConstant.RESULT_YES : CommonConstant.RESULT_NO;
        return new ResultUtil<String>().setData(result);
    }

    @RequestMapping(value = "/IsStaf/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取员工信息数据")
    public Result<IsStaffVo> IsStaf(@PathVariable String accountId){
        IsStaffVo isStaffVo = new IsStaffVo();
        Account account = accountService.get(accountId);
        if (account == null) {
            isStaffVo.setIsStaff(false);
            return new ResultUtil<IsStaffVo>().setData(isStaffVo);
        }
        Staff staff = staffService.findByAccountId(accountId);
        if(staff == null){
            if(StrUtil.isNotBlank(account.getStaffNo())){
                staff = staffService.findByStaffNo(account.getStaffNo());
            }
        }
        if (null != staff) {
            List<StaffRole> staffRoles = iStaffRoleService.findByStaffId(staff.getId());
            if (CollectionUtil.isNotEmpty(staffRoles)) {
                List<String> roleIds = new ArrayList<>();
                for ( StaffRole staffRole : staffRoles ) {
                    roleIds.add(staffRole.getRoleId());
                }
                List<RoleStaff> roleStaffs = roleStaffService.findByIdIn(roleIds);
                isStaffVo.setIsStaff(true);
                isStaffVo.setRoleStaffs(roleStaffs);
                return new ResultUtil<IsStaffVo>().setData(isStaffVo);
            } else {
                isStaffVo.setIsStaff(true);
                return new ResultUtil<IsStaffVo>().setData(isStaffVo);
            }

        } else {
            isStaffVo.setIsStaff(false);
            return new ResultUtil<IsStaffVo>().setData(isStaffVo);
        }
    }

    @RequestMapping(value = "/getRoleStaffAll", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取员工角色列表")
    public Result<List<RoleStaff>> getRoleStaffAll(){
        List<RoleStaff> roleStaffs = roleStaffService.findAll();
        return new ResultUtil<List<RoleStaff>>().setData(roleStaffs);
    }

    @RequestMapping(value = "/info/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取员工信息数据")
    public Result<StaffVo> select(@PathVariable String accountId) throws InstantiationException, IllegalAccessException {
        Account account = accountService.get(accountId);
        if (account == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("未找到相关数据");
        }
        Staff staff = staffService.findByAccountId(accountId);
        if(staff == null){
            if(StrUtil.isNotBlank(account.getStaffNo())){
                staff = staffService.findByStaffNo(account.getStaffNo());
            }
        }
        if (staff != null && !staff.getIsDeleted()) {
            StaffVo staffVo = YReflectUtil.copyFields(staff, StaffVo.class);
            if (StrUtil.isNotEmpty(staff.getDeptNo())) {
                staffVo.setDepartment(departmentService.selectById(staff.getDeptNo()));
            }
            if(staffVo.getQrcode() == null){
                staffVo.setQrcode("");
            }
            if(staffVo.getHeadImg() == null){
                staffVo.setHeadImg("");
            }
            //先解密，再加密
            if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
                if (StrUtil.isNotBlank(staffVo.getName())) {
                    staffVo.setName(AESUtil.comEncrypt(AESUtil.decrypt(staffVo.getName())));
                }
                if (StrUtil.isNotBlank(staffVo.getPhone())) {
                    staffVo.setPhone(AESUtil.comEncrypt(AESUtil.decrypt(staffVo.getPhone())));
                }
            }
            return new ResultUtil<StaffVo>().setData(staffVo);
        }else{
            boolean exist = staffService.validate(accountId);
            if(exist){
                staffService.removeFromCache(accountId);
            }
        }
        return new ResultUtil<StaffVo>().setErrorMsg("未找到相关数据");
    }

    @RequestMapping(value = "/updateDepartment", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "更新员工机构信息数据")
    public Result<StaffVo> updateDepartment(@RequestParam String accountId, @RequestParam String departmentId) throws InstantiationException, IllegalAccessException {
        Account account = accountService.get(accountId);
        if (account == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("未找到相关数据");
        }
        Staff staff = staffService.findByAccountId(accountId);
        if (staff == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("未找到相关数据");
        }
        if (StrUtil.isNotBlank(departmentId) && departmentId.equals(staff.getDeptNo())) {
            StaffVo staffVo = YReflectUtil.copyFields(staff, StaffVo.class);
            staffVo.setDepartment(departmentService.selectById(departmentId));
            return new ResultUtil<StaffVo>().setData(staffVo);
        }
        Department department = departmentService.get(departmentId);
        if (department == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("未找到相关数据");
        }
        staff.setDeptNo(departmentId);
        staff = staffService.update(staff);
       /* if (StrUtil.isNotBlank(staff.getName())) {
            staff.setName(AESUtil.encrypt(staff.getName()));
        }
        if (StrUtil.isNotBlank(staff.getPhone())) {
            staff.setPhone(AESUtil.encrypt(staff.getPhone()));
        }*/
        StaffVo staffVo = YReflectUtil.copyFields(staff, StaffVo.class);
        if (StrUtil.isNotEmpty(staff.getDeptNo())) {
            staffVo.setDepartment(departmentService.selectById(staff.getDeptNo()));
        }
        if(staffVo.getQrcode() == null){
            staffVo.setQrcode("");
        }
        if(staffVo.getHeadImg() == null){
            staffVo.setHeadImg("");
        }
        //先解密，再加密
        if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
            if (StrUtil.isNotBlank(staffVo.getName())) {
                staffVo.setName(AESUtil.comEncrypt(AESUtil.decrypt(staffVo.getName())));
            }
            if (StrUtil.isNotBlank(staffVo.getPhone())) {
                staffVo.setPhone(AESUtil.comEncrypt(AESUtil.decrypt(staffVo.getPhone())));
            }
        }
        return new ResultUtil<StaffVo>().setData(staffVo);
    }

    @RequestMapping(value = "/countAllByDepartmentId", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "根据部门Id获取该部门下所有员工数量（包含子部门员工）")
    public Result<Long> countAllByDepartmentId(@RequestParam String departmentId){
        Department department = departmentService.get(departmentId);
        if(department == null){
            return new ResultUtil<Long>().setData(0L);
        }
        long num= staffService.countAllByParentDeptNo(departmentId);
        return new ResultUtil<Long>().setData(num);
    }

    @RequestMapping(value = "/get2LevelStaffList", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "查找机构为2层结构的员工")
    public Result<List<StaffDto>> get2LevelStaffList(@ModelAttribute SearchStaffDto search){
        List<StaffDto> list=  iStaffService.find2LevelStaffBySearchStaffDto(search);
        //先解密，再加密
        if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
            for(StaffDto dto:list)
            {
                if (StrUtil.isNotBlank(dto.getStaffName())) {
                    dto.setStaffName(AESUtil.comEncrypt(AESUtil.decrypt(dto.getStaffName())));
                }
            }
        }
        return new ResultUtil<List<StaffDto>>().setData(list);
    }

    @RequestMapping(value = "/get3LevelStaffList", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "查找机构为3层结构的员工")
    public Result<List<StaffDto>> get3LevelStaffList(@ModelAttribute SearchStaffDto search){
        List<StaffDto> list=   iStaffService.find3LevelStaffBySearchStaffDto(search);
        //先解密，再加密
        if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
            for(StaffDto dto:list)
            {
                if (StrUtil.isNotBlank(dto.getStaffName())) {
                    dto.setStaffName(AESUtil.comEncrypt(AESUtil.decrypt(dto.getStaffName())));
                }
            }
        }
        return new ResultUtil<List<StaffDto>>().setData(list);
    }

    @RequestMapping(value = "/handlerStaffMess", method = RequestMethod.POST)
    @ApiOperation(value = "处理1；0推送员工消息")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<String> handlerStaffMess(@RequestParam String phone,
                                           @RequestParam String name,
                                           @RequestParam String staffNo,
                                           @RequestParam String deptNo,
                                           HttpServletRequest request) throws ParseException {
        String appid = UserContext.getAppid();
        phone = AESUtil.decrypt4v1(phone);
        name = AESUtil.decrypt4v1(name);
        staffNo = AESUtil.decrypt4v1(staffNo);
        deptNo = AESUtil.decrypt4v1(deptNo);
        Map<String,Object> staffMap = new HashMap<>(16);
        staffMap.put("staffNo",staffNo);
        List<Staff> staffList = iStaffService.findByMap(staffMap);
        Staff cordstaff = CollectionUtil.isNotEmpty(staffList)?staffList.get(0):null;
        if(cordstaff != null){
            String accountid =  cordstaff.getAccountId();
            Account account= accountService.get(accountid);
            String oldPhone = account==null?"":account.getPhone();
            // 更新新信息
            if (StrUtil.isNotBlank(deptNo)) {
               Department department = departmentService.findByDeptCode(deptNo);
               if (department == null) {
                     return new ResultUtil<String>().setErrorMsg("未找到对应部门！");
                }
                cordstaff.setDeptNo(department.getId());
            }
            if (StrUtil.isNotBlank(phone)) {
                List<Staff> staff1 = staffService.findByPhone(phone);
                if (staff1 != null && staff1.size() > 0) {
                    for (Staff staff2 : staff1) {
                        if (!staff2.getId().equals(cordstaff.getId())) {
                            return new ResultUtil<String>().setErrorMsg("手机号已被占用！");
                        }
                    }
                }
                cordstaff.setPhone(AESUtil.encrypt(phone));
            }
            if(StrUtil.isNotEmpty(name)){
                if( name.trim().length() > 10 ){
                    return new ResultUtil<String>().setErrorMsg("员工姓名长度不能超过10个长度");
                }
                cordstaff.setName(AESUtil.encrypt(name));
            }
            // 更新用户
            if(StrUtil.isNotBlank(phone) && !StrUtil.equals(oldPhone,phone)){
                // 解绑员工
                staffService.removeFromCache(cordstaff.getAccountId());
                cordstaff.setAccountId("");
                if(account != null){
                    account = accountService.clearAccount(account);
                    if(account.getIsStaff() == 1 ){
                        Integer isStaff = 0;
                        account.setIsStaff(isStaff);
                        account.setStaffNo("");
                    }
                    accountService.save(account);
                }
                boolean bindNew = false;
                // 绑定的新用户
                List<Account> accounts = accountService.findByAppidAndPhone(appid,AESUtil.encrypt(phone));
                if(CollectionUtil.isNotEmpty(accounts)){
                    Account newAccount = accounts.get(0);
                    newAccount = accountService.decryptAccount(newAccount);
                    cordstaff.setAccountId(newAccount.getId());
                    Integer isStaff = 1;
                    newAccount.setIsStaff(isStaff);
                    newAccount.setStaffNo(cordstaff.getStaffNo());

                    accountService.save(newAccount);
                    bindNew = true;
                }
                staffService.update(cordstaff);
                if(bindNew){
                    return new ResultUtil<String>().setData("bind_new");
                }else{
                    return new ResultUtil<String>().setData("bind_no");
                }
            }else{
                staffService.update(cordstaff);
                return new ResultUtil<String>().setData("staff_info");
            }
        }
        return new ResultUtil<String>().setErrorMsg("未找到员工信息");
    }

    @RequestMapping(value = "/getContactList/{departmentId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据部门Id获取该部门下的所有部门联系人")
    public Result<List<StaffVo>> getContactList(@PathVariable String departmentId) throws InstantiationException, IllegalAccessException {
        List<Staff> contactList = iStaffRoleService.findContactStaffByDepartId(departmentId);
        List<Department> departmentList =  departmentService.findAll();
        Map<String,Department> map = new HashMap<>();
        for(Department department  :departmentList){
            map.put(department.getId(),department);
        }
        List<StaffVo> list = new ArrayList<>();
        for(Staff staff:contactList){
            StaffVo staffVo = YReflectUtil.copyFields(staff, StaffVo.class);
            if(StrUtil.isNotBlank(staffVo.getDeptNo())){
                Department department = map.get(staffVo.getDeptNo());
                staffVo.setDepartment(department);
            }
            if(staffVo.getQrcode() == null){
                staffVo.setQrcode("");
            }
            if(staffVo.getHeadImg() == null){
                staffVo.setHeadImg("");
            }
            ////先解密，再加密 （不需要解密，已解密）
            //if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
            //    if (StrUtil.isNotBlank(staffVo.getName())) {
            //        staffVo.setName(AESUtil.comEncrypt(AESUtil.decrypt(staffVo.getName())));
            //    }
            //    if (StrUtil.isNotBlank(staffVo.getPhone())) {
            //        staffVo.setPhone(AESUtil.comEncrypt(AESUtil.decrypt(staffVo.getPhone())));
            //    }
            //}
            list.add(staffVo);
        }
        return new ResultUtil<List<StaffVo>>().setData(list);
    }


    @RequestMapping(value = "/getContactStaff/{staffId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据员工Id获取该部门下的部门联系人")
    public Result<StaffVo> getContact(@PathVariable String staffId) throws InstantiationException, IllegalAccessException {
        Staff staff = iStaffService.findImgStaffById(staffId);
        if(staff == null){
            return new ResultUtil<StaffVo>().setErrorMsg("未找到相关数据");
        }
        StaffVo staffVo = YReflectUtil.copyFields(staff, StaffVo.class);
        if (StrUtil.isNotBlank(staffVo.getName())) {
            staffVo.setName(AESUtil.decrypt(staffVo.getName()));
        }
        if (StrUtil.isNotBlank(staffVo.getPhone())) {
            staffVo.setPhone(AESUtil.decrypt(staffVo.getPhone()));
        }
        if (StrUtil.isNotEmpty(staff.getDeptNo())) {
            staffVo.setDepartment(departmentService.selectById(staff.getDeptNo()));
        }
        if(staffVo.getQrcode() == null){
            staffVo.setQrcode("");
        }
        if(staffVo.getHeadImg() == null){
            staffVo.setHeadImg("");
        }
        return new ResultUtil<StaffVo>().setData(staffVo);
    }

    @RequestMapping(value = "/getDefaultContactList/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "根据部门Id获取该部门下的所有部门联系人")
    public Result<Map<String,Object>> getDefaultContactList(@PathVariable String accountId) throws InstantiationException, IllegalAccessException {
        Account account = accountService.get(accountId);
        if (account == null) {
            return new ResultUtil<Map<String,Object>>().setData(null);
        }

        String identifier = account.getIdentifier();
        CustomerInformation customerInformation = customerInformationService.findByIdentifier(identifier);
        if(customerInformation == null || StrUtil.isBlank( customerInformation.getInstitutionalCode())){
            return new ResultUtil<Map<String,Object>>().setData(null);
        }
        String orgCode =  customerInformation.getInstitutionalCode();
        Department department = departmentService.findByDeptCode(orgCode);
        if(department == null  ){
            return new ResultUtil<Map<String,Object>>().setData(null);
        }
        Map<String,Object> result =new HashMap<>();
        List<Staff> contactList = iStaffRoleService.findContactStaffByDepartId(department.getId());
        ////先解密，再加密 （不需要解密，已解密）
        // if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
        //    for (Staff staff : contactList) {
        //        if (StrUtil.isNotBlank(staff.getName())) {
        //            staff.setName(AESUtil.comEncrypt(AESUtil.decrypt(staff.getName())));
        //        }
        //    }
        //}
        result.put("contactList",contactList);
        result.put("department",department);
        return new ResultUtil<Map<String,Object>>().setData(result);
    }


    //获取员工总人数
    @RequestMapping(value = "getstaffcount", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取员工总人数")
    public Result<Integer> getstaffcount(String wxappid)  {
        Integer staffcount= iStaffService.getStaffCount(wxappid);
        return new ResultUtil<Integer>().setData(staffcount);
    }

    //获取员工总人数
    @RequestMapping(value = "synstaff", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取员工数据")
    public Result<List<Staff>> getStaffData(String wxappid,Integer start,Integer end)  {
        List<Staff> StaffData= iStaffService.getStaffData(wxappid,start,end);
        //先解密，再加密
        if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
            for (Staff staff : StaffData) {
                if (StrUtil.isNotBlank(staff.getName())) {
                    staff.setName(AESUtil.comEncrypt(AESUtil.decrypt(staff.getName())));
                }
            }
        }
        return new ResultUtil<List<Staff>>().setData(StaffData);
    }

    //获取员工总人数
    @RequestMapping(value = "synstafflog", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取员工数据")
    public Result<String> getStaffDatalog(String wxappid,Integer start,Integer end)  {
        List<Staff> StaffData= iStaffService.getStaffData(wxappid,start,end);
        //通知发送Log
        List<Department> departments = departmentService.findAll();
        for (Staff staff:StaffData) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
            modifyStaffVo.setType("initial");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }
        return new ResultUtil<String>().setData("OK");
    }

    @RequestMapping(value = "/getAdvBusinessManager", method = RequestMethod.GET)
    @ApiOperation(value = "查询广告为业务经理")
    public Result<String> getAdvBusinessManager(@Param("staffIds") String staffIds){
        return new ResultUtil<String>().setData(staffService.getAdvBusinessManager(staffIds,STAFF_BUSINESS_MANAGER,null));
    }

    @RequestMapping(value = "/getDefaultBusinessManager", method = RequestMethod.GET)
    @ApiOperation(value = "查询广告位业务经理(默认)")
    public Result<String> getDefaultBusinessManager(){
        return new ResultUtil<String>().setData(staffService.getAdvBusinessManager(null,STAFF_BUSINESS_MANAGER,1));
    }

    @RequestMapping(value = "/getAllBusinessManager", method = RequestMethod.GET)
    @ApiOperation(value = "查询广告位业务经理(全部)")
    public Result<String> getAllBusinessManager(){
        return new ResultUtil<String>().setData(staffService.getAdvBusinessManager(null,STAFF_BUSINESS_MANAGER,null));
    }

    @RequestMapping(value = "/queryBussinessManagerStaff", method = RequestMethod.GET)
    @ApiOperation(value = "查询业务经理staff")
    public Result<List<BusinessManagerVo>> queryBussinessManagerStaff() {
        List<BusinessManagerVo> list = staffService.queryStaffByRoleCode(STAFF_BUSINESS_MANAGER);
        for (BusinessManagerVo dto : list) {
            if (StrUtil.isNotBlank(dto.getName())) {
                dto.setName(AESUtil.decrypt(dto.getName()));
            }
        }
        return new ResultUtil<List<BusinessManagerVo>>().setData(list);
    }

    @RequestMapping(value = "/getBusinessManagerById", method = RequestMethod.GET)
    @ApiOperation(value = "根据id查询业务经理")
    public Result<BusinessManagerVo> getBusinessManagerById(@RequestParam("id")String id) {
        BusinessManagerVo businessManagerVo = staffService.getBusinessManagerById(id);
        if(null != businessManagerVo){
            if (StrUtil.isNotBlank(businessManagerVo.getPhone())) {
                businessManagerVo.setPhone(AESUtil.decrypt(businessManagerVo.getPhone()));
            }
            if (StrUtil.isNotBlank(businessManagerVo.getName())) {
                businessManagerVo.setName(AESUtil.decrypt(businessManagerVo.getName()));
            }
        }

        return new ResultUtil<BusinessManagerVo>().setData(businessManagerVo);
    }
}