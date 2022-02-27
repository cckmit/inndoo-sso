package com.ytdinfo.inndoo.controller.base.manage;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.core.util.JedisUtil;
import com.ytdinfo.inndoo.modules.activiti.service.ActNodeService;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.entity.RoleDepartment;
import com.ytdinfo.inndoo.modules.base.entity.RolePermission;
import com.ytdinfo.inndoo.modules.base.entity.UserRole;
import com.ytdinfo.inndoo.modules.base.service.RoleDepartmentService;
import com.ytdinfo.inndoo.modules.base.service.RolePermissionService;
import com.ytdinfo.inndoo.modules.base.service.RoleService;
import com.ytdinfo.inndoo.modules.base.service.UserRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "角色管理接口")
@RequestMapping("/base/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private RoleDepartmentService roleDepartmentService;

    @Autowired
    private ActNodeService actNodeService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiOperation(value = "获取全部角色")
    public Result<Object> list(){

        List<Role> list = roleService.findAll();
        return new ResultUtil<Object>().setData(list);
    }

    @RequestMapping(value = "/listByPage", method = RequestMethod.GET)
    @ApiOperation(value = "分页获取角色")
    public Result<Page<Role>> listByPage(@ModelAttribute PageVo page){

        Page<Role> list = roleService.findAll(PageUtil.initPage(page));
        for(Role role : list.getContent()){
            // 角色拥有权限
            List<RolePermission> permissions = rolePermissionService.findByRoleId(role.getId());
            role.setPermissions(permissions);
            // 角色拥有数据权限
            List<RoleDepartment> departments = roleDepartmentService.findByRoleId(role.getId());
            role.setDepartments(departments);
        }
        return new ResultUtil<Page<Role>>().setData(list);
    }

    @RequestMapping(value = "/setDefault", method = RequestMethod.POST)
    @ApiOperation(value = "设置或取消默认角色")
    public Result<Object> setDefault(@RequestParam String id,
                                     @RequestParam Boolean isDefault){

        Role role = roleService.get(id);
        if(role==null){
            return new ResultUtil<Object>().setErrorMsg("角色不存在");
        }
        role.setDefaultRole(isDefault);
        roleService.update(role);
        return new ResultUtil<Object>().setSuccessMsg("设置成功");
    }

    @RequestMapping(value = "/updateRolePerm", method = RequestMethod.POST)
    @ApiOperation(value = "编辑角色分配菜单权限")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> updateRolePerm(@RequestParam String roleId,
                                       @RequestParam(required = false) String[] permIds){

        //删除其关联权限
        rolePermissionService.deleteByRoleId(roleId);
        //分配新权限
        List<RolePermission> list = new ArrayList<>();
        for(String permId : permIds){
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permId);
            list.add(rolePermission);
        }
        rolePermissionService.saveOrUpdateAll(list);

        //标记角色对应的全量用户
        List<UserRole> userRoleList = userRoleService.findByRoleId(roleId);
        if(CollectionUtil.isNotEmpty(userRoleList)){
            Set<String> set = new HashSet<>();
            for (UserRole userRole:userRoleList){
                set.add(userRole.getUserId());
            }
            if (CollectionUtil.isNotEmpty(set)){
                RedisUtil.addAllKeyToStore(RedisKeyStoreType.roleUserList.getPrefixKey(),set);
            }
        }

//        //手动批量删除缓存
//        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
//        redisTemplate.delete(keysUser);
//        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
//        Set<String> keysUserRole = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userRole.getPrefixKey());
//        redisTemplate.delete(keysUserRole);
//        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userRole.getPrefixKey());
//        Set<String> keysUserPerm = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userPermission.getPrefixKey());
//        redisTemplate.delete(keysUserPerm);
//        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userPermission.getPrefixKey());
//        Set<String> keysUserMenu = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userMenuList.getPrefixKey());
//        redisTemplate.delete(keysUserMenu);
//        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userMenuList.getPrefixKey());
//        Set<String> userTokens = RedisUtil.membersFromKeyStore(RedisKeyStoreType.USER_TOKEN.getPrefixKey());
//        if(CollectionUtil.isNotEmpty(userTokens)){
//            for(String userToken:userTokens){
//                Integer index = userToken.lastIndexOf(":");
//                String Token = userToken.substring(index);
//                String ssoToken = "sso:INNDOO_USER_TOKEN" + Token;
//                String matrixToken = "matrix:INNDOO_USER_TOKEN" + Token;
//                JedisUtil.del(ssoToken);
//                JedisUtil.del(matrixToken);
//            }
//        }
//        redisTemplate.delete(userTokens);
//        RedisUtil.clearKeyFromStore(RedisKeyStoreType.USER_TOKEN.getPrefixKey());
//        Set<String> tokenPres = RedisUtil.membersFromKeyStore(RedisKeyStoreType.TOKEN_PRE.getPrefixKey());
//        if(CollectionUtil.isNotEmpty(tokenPres)){
//            for(String tokenPre:tokenPres){
//                Integer index = tokenPre.lastIndexOf(":");
//                String Token = tokenPre.substring(index);
//                String ssotokenPre = "sso:INNDOO_TOKEN_PRE" + Token;
//                String matrixtokenPre = "matrix:INNDOO_TOKEN_PRE" + Token;
//                String ssotoken = "sso:accessToken" + Token;
//                JedisUtil.del(ssotokenPre);
//                JedisUtil.del(matrixtokenPre);
//                JedisUtil.del(ssotoken);
//            }
//        }
//        redisTemplate.delete(tokenPres);
//        RedisUtil.clearKeyFromStore(RedisKeyStoreType.TOKEN_PRE.getPrefixKey());
//        Set<String> users = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
//        if(CollectionUtil.isNotEmpty(users)){
//            for(String userName:users){
//                Integer index = userName.lastIndexOf("::");
//                String Token = userName.substring(index);
//                String ssouserName = "sso:user" + Token;
//                String matrixuserName = "matrix:user" + Token;
//                JedisUtil.del(ssouserName);
//                JedisUtil.del(matrixuserName);
//            }
//        }
//        redisTemplate.delete(users);
//        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/updateRoleDep", method = RequestMethod.POST)
    @ApiOperation(value = "编辑角色分配数据权限")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> updateRoleDep(@RequestParam String roleId,
                                      @RequestParam Integer dataType,
                                      @RequestParam(required = false) String[] depIds){

        Role r = roleService.get(roleId);
        r.setDataType(dataType);
        roleService.update(r);
        // 删除其关联数据权限
        roleDepartmentService.deleteByRoleId(roleId);
        // 分配新数据权限
        for(String depId : depIds){
            RoleDepartment roleDepartment = new RoleDepartment();
            roleDepartment.setRoleId(roleId);
            roleDepartment.setDepartmentId(depId);
            roleDepartmentService.save(roleDepartment);
        }
        // 手动删除相关缓存
        Set<String> keys = RedisUtil.keys("department:" + "*");
        redisTemplate.unlink(keys);
        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
        redisTemplate.unlink(keysUser);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
        Set<String> keysUserRole = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userRole.getPrefixKey());
        redisTemplate.unlink(keysUserRole);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userRole.getPrefixKey());
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ApiOperation(value = "保存数据")
    public Result<Role> create(@ModelAttribute Role role){

        Role r = roleService.save(role);
        return new ResultUtil<Role>().setData(r);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ApiOperation(value = "更新数据")
    public Result<Role> update(@ModelAttribute Role entity){

        Role r = roleService.update(entity);
        //手动批量删除缓存
        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
        redisTemplate.delete(keysUser);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
        Set<String> keysUserRole = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userRole.getPrefixKey());
        redisTemplate.delete(keysUserRole);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userRole.getPrefixKey());
        return new ResultUtil<Role>().setData(r);
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "批量通过ids删除")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> batchDelete(@PathVariable String[] ids){

        for(String id:ids){
            List<UserRole> list = userRoleService.findByRoleId(id);
            if(list!=null&&list.size()>0){
                return new ResultUtil<Object>().setErrorMsg("删除失败，包含正被用户使用关联的角色");
            }
        }
        for(String id:ids){
            roleService.delete(id);
            //删除关联菜单权限
            rolePermissionService.deleteByRoleId(id);
            //删除关联数据权限
            roleDepartmentService.deleteByRoleId(id);
            // 删除流程关联节点
            actNodeService.deleteByRelateId(id);
        }
        // 删除所有用户缓存
        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
        redisTemplate.delete(keysUser);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
        return new ResultUtil<Object>().setSuccessMsg("批量通过id删除数据成功");
    }

}
