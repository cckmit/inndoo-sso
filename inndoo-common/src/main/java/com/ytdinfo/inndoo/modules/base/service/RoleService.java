package com.ytdinfo.inndoo.modules.base.service;


import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.base.entity.Role;

import java.util.List;

/**
 * 角色接口
 * @author Exrickx
 */
public interface RoleService extends BaseService<Role,String> {

    /**
     * 获取默认角色
     * @param defaultRole
     * @return
     */
    List<Role> findByDefaultRole(Boolean defaultRole);

    /**
     * 根据名称获取角色
     * @param name
     * @return
     */
    Role findByName(String name);
}
