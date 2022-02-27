package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.PrivacyUtil;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.core.dao.mapper.StaffRoleMapper;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 短信发送记录接口实现
 *
 * @author haiqing
 */
@Slf4j
@Service
public class IStaffRoleServiceImpl extends BaseServiceImpl<StaffRoleMapper, StaffRole> implements IStaffRoleService {

    @Autowired
    private StaffRoleMapper staffRoleMapper;

    @Autowired
    private RedisTemplate<String,List<Staff>> staffListTemplate;

    @Autowired
    private RedisTemplate<String,List<Department>> departmentListTemplate;

    @Override
    public List<StaffRole> findByStaffId(String staffid) {
        return staffRoleMapper.findRoleByStaffId(staffid);
    }

    @Override
    public List<Department> findContactDept() {

        String cacheKey = "contact_departmentlist::all";
        List<Department> cacheList = departmentListTemplate.opsForValue().get(cacheKey);
        if(cacheList != null && cacheList.size()>0){
            return  cacheList;
        }
        List<Department> departments = staffRoleMapper.findContactDept("STAFF_CONTACTS");
        if(departments.size() > 0){
            departmentListTemplate.opsForValue().set(cacheKey,departments);
            departmentListTemplate.expire(cacheKey,7, TimeUnit.DAYS);
        }
        return departments;
    }

    @Override
    public List<StaffRole> findRoleByRoleId(String roleid) {
        return staffRoleMapper.findRoleByRoleId(roleid);
    }

    @Override
    public StaffRole findRoleByRoleIdAndStaffId(String roleid, String staffId) {
        return staffRoleMapper.findRoleByRoleIdAndStaffId(roleid,staffId);
    }

    @Override
    public List<Staff> findContactStaffByDepartId(String departmentId) {
        String cacheKey = "contact_stafflist::"+departmentId;
        List<Staff> cacheList = staffListTemplate.opsForValue().get(cacheKey);
        if(cacheList != null && cacheList.size()>0){
            return  cacheList;
        }
        List<Staff> staffList = staffRoleMapper.findContactStaffByDepartId(departmentId);
        for(Staff tempStaff:staffList ){
            if (StrUtil.isNotEmpty(tempStaff.getName())) {
                tempStaff.setName(AESUtil.decrypt(tempStaff.getName()));
            }
            if (StrUtil.isNotEmpty(tempStaff.getPhone())) {
                tempStaff.setPhone(AESUtil.decrypt(tempStaff.getPhone()));
            }
            tempStaff.setTmname(PrivacyUtil.nameEncrypt(tempStaff.getName()));
            tempStaff.setTmstaffNo(PrivacyUtil.formatToMask(tempStaff.getStaffNo()));
        }
        staffListTemplate.opsForValue().set(cacheKey,staffList);
        staffListTemplate.expire(cacheKey,7, TimeUnit.DAYS);
        return staffList;
    }


}