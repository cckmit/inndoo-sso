package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;

import java.util.List;

/**
 * 员工-角色数据处理层
 * @author Nolan
 */
public interface StaffRoleDao extends BaseDao<StaffRole,String> {

    void deleteByStaffId(String staffId);

    List<StaffRole> findByRoleId(String roleId);

    StaffRole findByStaffIdAndRoleId(String staffId,String roleId);
}