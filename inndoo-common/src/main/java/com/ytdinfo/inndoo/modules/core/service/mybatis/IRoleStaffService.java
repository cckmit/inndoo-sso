package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;

import java.util.List;

/**
 * 员工-角色
 *
 * @author 朱林
 * @date 2021-03-08 下午3:35
 **/
public interface IRoleStaffService extends IService<RoleStaff> {

    RoleStaff findByCode(String code);
}