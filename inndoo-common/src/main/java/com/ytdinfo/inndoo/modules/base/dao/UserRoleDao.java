package com.ytdinfo.inndoo.modules.base.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.base.entity.UserRole;

import java.util.List;

/**
 * 用户角色数据处理层
 * @author Exrickx
 */
public interface UserRoleDao extends BaseDao<UserRole,String> {

    /**
     * 通过roleId查找
     * @param roleId
     * @return
     */
    List<UserRole> findByRoleId(String roleId);

    /**
     * 通过userId查找
     * @param userId
     * @return
     */
    List<UserRole> findByUserId(String userId);

    /**
     * 删除用户角色
     * @param userId
     */
    void deleteByUserId(String userId);

    /**
     * 通过roleId查找
     * @param roleId
     * @return
     */
    List<UserRole> findByRoleIdAndUserId(String roleId,String userId);
}
