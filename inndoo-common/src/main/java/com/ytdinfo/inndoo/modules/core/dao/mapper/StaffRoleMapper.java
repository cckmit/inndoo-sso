package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.core.dto.SearchStaffDto;
import com.ytdinfo.inndoo.modules.core.dto.StaffDto;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface StaffRoleMapper extends BaseInndooMapper<StaffRole> {

    List<StaffRole> findRoleByStaffId(String staffid);

    List<Department> findContactDept(String code);

    List<StaffRole> findRoleByRoleId(String roleid );

    List<Staff> findContactStaffByDepartId(String departmentId);

    StaffRole findRoleByRoleIdAndStaffId(@Param("roleid") String roleid,@Param("staffId") String staffId);
}
