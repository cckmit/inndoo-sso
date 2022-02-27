package com.ytdinfo.inndoo.modules.base.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.modules.base.dao.RoleDao;
import com.ytdinfo.inndoo.modules.base.dao.UserDao;
import com.ytdinfo.inndoo.modules.base.dao.UserRoleDao;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.entity.UserRole;
import com.ytdinfo.inndoo.modules.base.service.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户角色接口实现
 * @author Exrickx
 */
@Slf4j
@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleDao roleDao;

    @Override
    public UserRoleDao getRepository() {
        return userRoleDao;
    }

    @Override
    public List<UserRole> findByRoleId(String roleId) {
        return userRoleDao.findByRoleId(roleId);
    }

    @Override
    public List<Role> findRoleByUserId(String userId) {
        List<UserRole> userRoleList = userRoleDao.findByUserId(userId);
        List<Role> list = new ArrayList<>();
        if (CollectionUtil.isEmpty(userRoleList)){
            return list;
        }
        for(UserRole ur : userRoleList){
            Role role = roleDao.getOne(ur.getRoleId());
            if(role!=null){
                list.add(role);
            }
        }
        return list;
    }

    @Override
    public List<User> findUserByRoleId(String roleId) {

        List<UserRole> userRoleList = userRoleDao.findByRoleId(roleId);
        List<User> list = new ArrayList<>();
        for(UserRole ur : userRoleList){
            User u = userDao.getOne(ur.getUserId());
            if(u!=null&& CommonConstant.USER_STATUS_NORMAL.equals(u.getStatus())){
                list.add(u);
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserId(String userId) {
        userRoleDao.deleteByUserId(userId);
    }

    @Override
    public List<UserRole> findByRoleIdAndUserId(String roleId,String userId) {
        return userRoleDao.findByRoleIdAndUserId(roleId,userId);
    }
}
