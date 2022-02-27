package com.ytdinfo.inndoo.modules.base.service.mybatis;

import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.base.entity.Permission;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * @author Exrickx
 */
@CacheConfig(cacheNames = "userPermission")
public interface IPermissionService extends IService<Permission> {

    /**
     * 通过用户id获取
     * @param userId
     * @return
     */
    @Cacheable(key = "#userId")
    @CacheExpire(CommonConstant.SECOND_1DAY)
    List<Permission> findByUserId(String userId);
}
