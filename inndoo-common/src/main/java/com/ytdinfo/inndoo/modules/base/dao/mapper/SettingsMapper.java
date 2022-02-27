package com.ytdinfo.inndoo.modules.base.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ytdinfo.inndoo.modules.base.entity.Settings;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置数据处理层
 * @author yaochangning
 */
public interface SettingsMapper extends BaseMapper<Settings> {
    List<Settings> selectByKeyName(@Param("keyName") String keyName);

    /**
     * 通过keyName模糊获取
     * @param keyName
     * @return
     */
    List<Settings> selectListByLikeKeyName(@Param("keyName") String keyName);
}
