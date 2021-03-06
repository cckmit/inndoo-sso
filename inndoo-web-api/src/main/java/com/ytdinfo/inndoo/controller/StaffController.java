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
@Api(description = "????????????")
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
     * ??????????????????code
     */
    private final static String STAFF_BUSINESS_MANAGER = "STAFF_BUSINESS_MANAGER";

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "????????????")
    public Result<String> validateRecord(@RequestParam String accountId) {
        boolean exist = staffService.validate(accountId);
        String result = exist ? CommonConstant.RESULT_YES : CommonConstant.RESULT_NO;
        return new ResultUtil<String>().setData(result);
    }

    @RequestMapping(value = "/IsStaf/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "????????????????????????")
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
    @ApiOperation(value = "????????????????????????")
    public Result<List<RoleStaff>> getRoleStaffAll(){
        List<RoleStaff> roleStaffs = roleStaffService.findAll();
        return new ResultUtil<List<RoleStaff>>().setData(roleStaffs);
    }

    @RequestMapping(value = "/info/{accountId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "????????????????????????")
    public Result<StaffVo> select(@PathVariable String accountId) throws InstantiationException, IllegalAccessException {
        Account account = accountService.get(accountId);
        if (account == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("?????????????????????");
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
            //?????????????????????
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
        return new ResultUtil<StaffVo>().setErrorMsg("?????????????????????");
    }

    @RequestMapping(value = "/updateDepartment", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "??????????????????????????????")
    public Result<StaffVo> updateDepartment(@RequestParam String accountId, @RequestParam String departmentId) throws InstantiationException, IllegalAccessException {
        Account account = accountService.get(accountId);
        if (account == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("?????????????????????");
        }
        Staff staff = staffService.findByAccountId(accountId);
        if (staff == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("?????????????????????");
        }
        if (StrUtil.isNotBlank(departmentId) && departmentId.equals(staff.getDeptNo())) {
            StaffVo staffVo = YReflectUtil.copyFields(staff, StaffVo.class);
            staffVo.setDepartment(departmentService.selectById(departmentId));
            return new ResultUtil<StaffVo>().setData(staffVo);
        }
        Department department = departmentService.get(departmentId);
        if (department == null) {
            return new ResultUtil<StaffVo>().setErrorMsg("?????????????????????");
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
        //?????????????????????
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
    @ApiOperation(value = "????????????Id???????????????????????????????????????????????????????????????")
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
    @ApiOperation(value = "???????????????2??????????????????")
    public Result<List<StaffDto>> get2LevelStaffList(@ModelAttribute SearchStaffDto search){
        List<StaffDto> list=  iStaffService.find2LevelStaffBySearchStaffDto(search);
        //?????????????????????
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
    @ApiOperation(value = "???????????????3??????????????????")
    public Result<List<StaffDto>> get3LevelStaffList(@ModelAttribute SearchStaffDto search){
        List<StaffDto> list=   iStaffService.find3LevelStaffBySearchStaffDto(search);
        //?????????????????????
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
    @ApiOperation(value = "??????1???0??????????????????")
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
            // ???????????????
            if (StrUtil.isNotBlank(deptNo)) {
               Department department = departmentService.findByDeptCode(deptNo);
               if (department == null) {
                     return new ResultUtil<String>().setErrorMsg("????????????????????????");
                }
                cordstaff.setDeptNo(department.getId());
            }
            if (StrUtil.isNotBlank(phone)) {
                List<Staff> staff1 = staffService.findByPhone(phone);
                if (staff1 != null && staff1.size() > 0) {
                    for (Staff staff2 : staff1) {
                        if (!staff2.getId().equals(cordstaff.getId())) {
                            return new ResultUtil<String>().setErrorMsg("????????????????????????");
                        }
                    }
                }
                cordstaff.setPhone(AESUtil.encrypt(phone));
            }
            if(StrUtil.isNotEmpty(name)){
                if( name.trim().length() > 10 ){
                    return new ResultUtil<String>().setErrorMsg("??????????????????????????????10?????????");
                }
                cordstaff.setName(AESUtil.encrypt(name));
            }
            // ????????????
            if(StrUtil.isNotBlank(phone) && !StrUtil.equals(oldPhone,phone)){
                // ????????????
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
                // ??????????????????
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
        return new ResultUtil<String>().setErrorMsg("?????????????????????");
    }

    @RequestMapping(value = "/getContactList/{departmentId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "????????????Id??????????????????????????????????????????")
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
            ////????????????????????? ?????????????????????????????????
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
    @ApiOperation(value = "????????????Id????????????????????????????????????")
    public Result<StaffVo> getContact(@PathVariable String staffId) throws InstantiationException, IllegalAccessException {
        Staff staff = iStaffService.findImgStaffById(staffId);
        if(staff == null){
            return new ResultUtil<StaffVo>().setErrorMsg("?????????????????????");
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
    @ApiOperation(value = "????????????Id??????????????????????????????????????????")
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
        ////????????????????????? ?????????????????????????????????
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


    //?????????????????????
    @RequestMapping(value = "getstaffcount", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "?????????????????????")
    public Result<Integer> getstaffcount(String wxappid)  {
        Integer staffcount= iStaffService.getStaffCount(wxappid);
        return new ResultUtil<Integer>().setData(staffcount);
    }

    //?????????????????????
    @RequestMapping(value = "synstaff", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "??????????????????")
    public Result<List<Staff>> getStaffData(String wxappid,Integer start,Integer end)  {
        List<Staff> StaffData= iStaffService.getStaffData(wxappid,start,end);
        //?????????????????????
        if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
            for (Staff staff : StaffData) {
                if (StrUtil.isNotBlank(staff.getName())) {
                    staff.setName(AESUtil.comEncrypt(AESUtil.decrypt(staff.getName())));
                }
            }
        }
        return new ResultUtil<List<Staff>>().setData(StaffData);
    }

    //?????????????????????
    @RequestMapping(value = "synstafflog", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "??????????????????")
    public Result<String> getStaffDatalog(String wxappid,Integer start,Integer end)  {
        List<Staff> StaffData= iStaffService.getStaffData(wxappid,start,end);
        //????????????Log
        List<Department> departments = departmentService.findAll();
        for (Staff staff:StaffData) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
            modifyStaffVo.setType("initial");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }
        return new ResultUtil<String>().setData("OK");
    }

    @RequestMapping(value = "/getAdvBusinessManager", method = RequestMethod.GET)
    @ApiOperation(value = "???????????????????????????")
    public Result<String> getAdvBusinessManager(@Param("staffIds") String staffIds){
        return new ResultUtil<String>().setData(staffService.getAdvBusinessManager(staffIds,STAFF_BUSINESS_MANAGER,null));
    }

    @RequestMapping(value = "/getDefaultBusinessManager", method = RequestMethod.GET)
    @ApiOperation(value = "???????????????????????????(??????)")
    public Result<String> getDefaultBusinessManager(){
        return new ResultUtil<String>().setData(staffService.getAdvBusinessManager(null,STAFF_BUSINESS_MANAGER,1));
    }

    @RequestMapping(value = "/getAllBusinessManager", method = RequestMethod.GET)
    @ApiOperation(value = "???????????????????????????(??????)")
    public Result<String> getAllBusinessManager(){
        return new ResultUtil<String>().setData(staffService.getAdvBusinessManager(null,STAFF_BUSINESS_MANAGER,null));
    }

    @RequestMapping(value = "/queryBussinessManagerStaff", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????staff")
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
    @ApiOperation(value = "??????id??????????????????")
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