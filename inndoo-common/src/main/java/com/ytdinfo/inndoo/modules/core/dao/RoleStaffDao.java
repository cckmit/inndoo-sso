package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;

import java.util.List;

/**
 * 角色（员工）数据处理层
 * @author Nolan
 */
public interface RoleStaffDao extends BaseDao<RoleStaff,String> {

    List<RoleStaff> findByDefaultRole(Boolean defaultRole);

    List<RoleStaff> findByIdIn(List<String> ids);

    List<RoleStaff> findByNameIn(List<String> names);

    List<RoleStaff> findByName(String name);

    List<RoleStaff> findByCode(String code);
}