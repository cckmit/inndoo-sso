package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.modules.base.dao.mapper.SettingsMapper;
import com.ytdinfo.inndoo.modules.core.dao.SettingsDao;
import com.ytdinfo.inndoo.modules.base.entity.Settings;
import com.ytdinfo.inndoo.modules.core.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置接口实现
 * @author Timmy
 */
@Slf4j
@Service
public class SettingsServiceImpl implements SettingsService {

    @Autowired
    private SettingsDao settingsDao;
    @Autowired
    private SettingsMapper settingsMapper;
    @Override
    public SettingsDao getRepository() {
        return settingsDao;
    }

    @Override
    public List<Settings> selectListByKeyName(String keyName) {
        List<Settings> list = settingsMapper.selectByKeyName(keyName);
        return list;
    }

    @Override
    public List<Settings> selectListByLikeKeyName(String keyName) {
        return settingsMapper.selectListByLikeKeyName(keyName + "%");
    }

}