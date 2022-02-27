package com.ytdinfo.inndoo.config.security.permission;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.base.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 权限管理决断器
 * 判断用户拥有的权限或角色是否有资源访问权限
 * @author Exrickx
 */
@Slf4j
//@Component
public class MyAccessDecisionManager implements AccessDecisionManager {

    @Override
    public void decide(Authentication authentication, Object o, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {

        Boolean isReset = false;
        List<GrantedAuthority> grantedAuthorityList = (List<GrantedAuthority>) authentication.getAuthorities();
        SecurityUtil securityUtil = SpringContextUtil.getBean(SecurityUtil.class);
        User user = securityUtil.getCurrUser();
        if (user != null) {
            Set<String> userIdList = RedisUtil.membersFromKeyStore(RedisKeyStoreType.roleUserList.getPrefixKey());
            if (CollectionUtil.isNotEmpty(userIdList)){
                for (String userId:userIdList){
                    if (userId.equals(user.getId())){
                        isReset = securityUtil.resetUserMenuList(user);
                    }
                }
            }
            if (isReset){
                userIdList.remove(user.getId());
                RedisUtil.clearKeyFromStore(RedisKeyStoreType.roleUserList.getPrefixKey());
                RedisUtil.addAllKeyToStore(RedisKeyStoreType.roleUserList.getPrefixKey(),userIdList);
            }
            grantedAuthorityList = securityUtil.getCurrUserPerms(user.getUsername());
        }
        if(configAttributes==null){
            return;
        }
        Iterator<ConfigAttribute> iterator = configAttributes.iterator();
        while (iterator.hasNext()){
            ConfigAttribute c = iterator.next();
            String needPerm = c.getAttribute();
            for(GrantedAuthority ga : grantedAuthorityList) {
                // 匹配用户拥有的ga 和 系统中的needPerm
                if(needPerm.trim().equals(ga.getAuthority())) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("抱歉，您没有访问权限");
    }

    @Override
    public boolean supports(ConfigAttribute configAttribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }
}
