package com.ytdinfo.inndoo.modules.base.dao.mapper;

import com.ytdinfo.inndoo.modules.base.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

/**
 * @author Exrickx
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 通过用户id获取
     * @param userId
     * @return
     */
    List<Permission> findByUserId(@Param("userId") String userId);
}
