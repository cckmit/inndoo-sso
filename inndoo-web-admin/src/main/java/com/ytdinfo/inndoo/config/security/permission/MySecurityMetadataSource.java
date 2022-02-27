package com.ytdinfo.inndoo.config.security.permission;

import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.config.security.IgnoredUrlsProperties;
import com.ytdinfo.inndoo.modules.base.entity.Permission;
import com.ytdinfo.inndoo.modules.base.service.PermissionService;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.*;

/**
 * 权限资源管理器
 * 为权限决断器提供支持
 * @author Exrickx
 */
@Slf4j
@Component
public class MySecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

    @Autowired
    private PermissionService permissionService;
    @Autowired
    private IgnoredUrlsProperties ignoredUrlsProperties;

    @Autowired
    private RedisTemplate<String,List<Permission>> redisTemplate;

    /**
     * 加载权限表中所有操作请求权限
     */
    public Map<String, Collection<ConfigAttribute>> loadResourceDefine(boolean refresh){

        String cacheKey = "permission:list";
        List<Permission> permissions = redisTemplate.opsForValue().get(cacheKey);
        if(refresh == true || permissions == null){
            permissions = permissionService.findByTypeAndStatusOrderBySortOrder(CommonConstant.PERMISSION_OPERATION, CommonConstant.STATUS_NORMAL);
            redisTemplate.opsForValue().set(cacheKey,permissions);
        }
        Map<String, Collection<ConfigAttribute>> map = new HashMap<>(16);
        Collection<ConfigAttribute> configAttributes;
        ConfigAttribute cfg;
        for(Permission permission : permissions) {
            if(StrUtil.isNotBlank(permission.getTitle())&&StrUtil.isNotBlank(permission.getPath())){
                configAttributes = new ArrayList<>();
                cfg = new SecurityConfig(permission.getTitle());
                //作为MyAccessDecisionManager类的decide的第三个参数
                configAttributes.add(cfg);
                //用权限的path作为map的key，用ConfigAttribute的集合作为value
                map.put(permission.getPath(), configAttributes);
            }
        }
        return map;
    }

    /**
     * 判定用户请求的url是否在权限表中
     * 如果在权限表中，则返回给decide方法，用来判定用户是否有此权限
     * 如果不在权限表中则放行
     * @param o
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public Collection<ConfigAttribute> getAttributes(Object o) throws IllegalArgumentException {

        Map<String, Collection<ConfigAttribute>> map = loadResourceDefine(false);
        //Object中包含用户请求request
        String url = ((FilterInvocation) o).getRequestUrl();
        //路径中去除虚拟路径
        String contextPath = ((FilterInvocation) o).getRequest().getContextPath();
        url = url.replace(contextPath,url);
        PathMatcher pathMatcher = new AntPathMatcher();

        for(String ignoreUrl:ignoredUrlsProperties.getUrls()){
            if(pathMatcher.match(ignoreUrl,url)){
                return null;
            }
        }

        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String resURL = iterator.next();
            if (StrUtil.isNotBlank(resURL)&&pathMatcher.match(resURL,url)) {
                return map.get(resURL);
            }
        }
        //默认拒绝访问
        SecurityConfig block = new SecurityConfig(url);
        ArrayList<ConfigAttribute> blockConfig = new ArrayList<>();
        blockConfig.add(block);
        return blockConfig;
//        return null;
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }
}
