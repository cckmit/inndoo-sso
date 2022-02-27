package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 员工-角色接口
 * @author Nolan
 */
public interface StaffRoleService extends BaseService<StaffRole,String> {

    /**
    * 多条件分页获取
    * @param staffRole
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<StaffRole> findByCondition(StaffRole staffRole, SearchVo searchVo, Pageable pageable);

    void deleteByStaffId(String id);

    List<StaffRole> findByRoleId(String id);

    StaffRole findByRoleIdAndStaffId(String roleId,String staffId);
}