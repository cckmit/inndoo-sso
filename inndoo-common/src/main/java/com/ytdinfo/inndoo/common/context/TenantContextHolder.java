package com.ytdinfo.inndoo.common.context;

import com.ytdinfo.inndoo.common.vo.Tenant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author timmy
 * @date 2019/9/3
 */
public class TenantContextHolder {
    private static final Map<String, Tenant> map = new HashMap<>();

    public static synchronized void init(List<Tenant> tenants) {
        for (Tenant tenant : tenants) {
            map.put(tenant.getId(), tenant);
        }
    }

    public static synchronized void add(Tenant tenant){
        map.put(tenant.getId(),tenant);
    }

    public static synchronized void remove(String tenantId){
        map.remove(tenantId);
    }

    public static Tenant get(String tenantId){
        return map.get(tenantId);
    }

}