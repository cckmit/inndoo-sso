package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;
import java.util.Map;

/**
 * 角色（员工）接口
 * @author Nolan
 */
public interface RoleStaffService extends BaseService<RoleStaff,String> {

    /**
    * 多条件分页获取
    * @param roleStaff
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<RoleStaff> findByCondition(RoleStaff roleStaff, SearchVo searchVo, Pageable pageable);

    /**
     * 获取默认角色
     * @param defaultRole
     * @return
     */
    List<RoleStaff> findByDefaultRole(Boolean defaultRole);

    List<RoleStaff> findByIdIn(List<String> roleIds);

    List<RoleStaff> findByNameIn(List<String> roleName);

    /**
     * 获取key为Id的集合
     * @return
     */
    Map<String, RoleStaff> getIdMap();

    /**
     * 获取key为name的集合
     * @return
     */
    Map<String, RoleStaff> getNameMap();

    List<RoleStaff> findByName(String name);

    List<RoleStaff> findByCode(String code);
}