package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.entity.UserRole;
import com.ytdinfo.inndoo.modules.base.service.RoleService;
import com.ytdinfo.inndoo.modules.base.service.UserRoleService;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import com.ytdinfo.inndoo.vo.RoleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Scope("prototype")
public class UserConsumer extends BaseConsumer {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(MQMessage mqMessage) {
        String action = mqMessage.getMessageProperties().getHeaders().get("action").toString();
        User u = JSONUtil.toBean((JSONObject) mqMessage.getContent(), User.class);
        if ("delete".equals(action)) {
            userService.delete(u);
            userRoleService.deleteByUserId(u.getId());
        }
        if ("put".equals(action)) {
            userService.update(u);
            boolean isAuth = false;
            if (StrUtil.isNotBlank(u.getTenantId())){
                String[] tenantIds = u.getTenantId().split(",");
                for (String tenantId:tenantIds){
                    if (tenantId.equals(UserContext.getTenantId())){
                        isAuth = true;
                    }
                }
            }
            //当前用户在当前租户未授权
            if (!isAuth||CollectionUtil.isEmpty(userRoleService.findRoleByUserId(u.getId()))){
                List<UserRole> userRoleList = getRoleList(u);
                if (CollectionUtil.isNotEmpty(userRoleList)){
                    userRoleService.deleteByUserId(u.getId());
                    userRoleService.saveOrUpdateAll(userRoleList);
                }
            }
        }
        if ("add".equals(action)) {
            User user = userService.save(u);

            List<UserRole> userRoleList = getRoleList(u);
            if (CollectionUtil.isNotEmpty(userRoleList)){
                userRoleService.saveOrUpdateAll(userRoleList);
            }

        }
        if ("updateRole".equals(action)) {
            userRoleService.deleteByUserId(u.getId());
            List<UserRole> userRoleList = getRoleList(u);
            if (CollectionUtil.isNotEmpty(userRoleList)){
                userRoleService.saveOrUpdateAll(userRoleList);
            }
        }
        // 删除缓存
        redisTemplate.delete("user::" + u.getUsername());
        redisTemplate.delete("userRole::"+u.getId());
        SecurityUtil securityUtil = SpringContextUtil.getBean(SecurityUtil.class);
        securityUtil.resetUserMenuList(u);
    }

    private List<UserRole> getRoleList(User user){
        List<UserRole> userRoleList = new ArrayList<>();
        if (StrUtil.isNotBlank(user.getAppRoles())){
            JSONArray jsonArray = new JSONArray(user.getAppRoles());
            List<RoleVo> roleList = JSONUtil.toList(jsonArray, RoleVo.class);
            for (RoleVo roleVo:roleList){
                if (roleVo.getTitle().startsWith("客户信息系统")){
                    Role role = roleService.findByName(roleVo.getTitle().substring(7));
                    if (role!=null){
                        UserRole ur = new UserRole();
                        ur.setUserId(user.getId());
                        ur.setRoleId(role.getId());
                        userRoleList.add(ur);
                    }
                }
            }
        }
        return userRoleList;
    }


    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
