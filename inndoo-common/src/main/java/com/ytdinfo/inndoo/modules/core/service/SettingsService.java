package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.base.entity.Settings;

import java.util.List;

/**
 * 系统配置接口
 * @author Timmy
 */
public interface SettingsService extends BaseService<Settings,String> {

    /**
     * 通过keyName获取
     * @param keyName
     * @return
     */
    List<Settings> selectListByKeyName(String keyName);

    /**
     * 通过keyName模糊获取
     * @param keyName
     * @return
     */
    List<Settings> selectListByLikeKeyName(String keyName);
}