package com.ytdinfo.inndoo.common.context;

import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.common.vo.Tenant;
import com.ytdinfo.inndoo.modules.base.entity.User;

/**
 *
 * @author timmy
 * @date 2019/9/6
 */
public class UserContext {
    private static final TransmittableThreadLocal<User> USER_CONTEXT = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<String> WXAPPID_CONTEXT = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<String> COMPONENT_APPID_CONTEXT = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<String> TENANTID_CONTEXT = new TransmittableThreadLocal<>();

    public static User getUser(){
        User user = USER_CONTEXT.get();
        if(user == null){
            SecurityUtil securityUtil = SpringContextUtil.getBean(SecurityUtil.class);
            user = securityUtil.getCurrUser();
            USER_CONTEXT.set(user);
        }
        return user;
    }

    public static synchronized void setWxAppId(String wxappid){
        if(StrUtil.isEmpty(wxappid)){
            WXAPPID_CONTEXT.remove();
            return;
        }
        WXAPPID_CONTEXT.set(wxappid);
    }

    public static String getAppid(){
        return WXAPPID_CONTEXT.get();
    }

    public static String getComponentAppid(){
        return COMPONENT_APPID_CONTEXT.get();
    }

    public static synchronized void setTenantId(String tenantId){
        if(StrUtil.isEmpty(tenantId)){
            COMPONENT_APPID_CONTEXT.remove();
            COMPONENT_APPID_CONTEXT.remove();
            return;
        }
        TENANTID_CONTEXT.set(tenantId);
        Tenant tenant = TenantContextHolder.get(tenantId);
        if(tenant != null){
            COMPONENT_APPID_CONTEXT.set(tenant.getWxopenComponentAppId());
        }
    }

    public static String getTenantId(){
        return TENANTID_CONTEXT.get();
    }

    public static void remove(){
        USER_CONTEXT.remove();
        WXAPPID_CONTEXT.remove();
        COMPONENT_APPID_CONTEXT.remove();
        TENANTID_CONTEXT.remove();
    }
}