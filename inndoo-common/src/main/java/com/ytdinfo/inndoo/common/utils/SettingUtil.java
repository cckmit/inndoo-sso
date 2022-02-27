package com.ytdinfo.inndoo.common.utils;


import com.ytdinfo.inndoo.modules.base.entity.Settings;
import com.ytdinfo.inndoo.modules.core.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SettingUtil {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * redis查询不到的系统配置信息走数据库查询
     * @param keyName
     * @return
     */
    public String getSettingValue(String keyName){
        String v = "";
        List<Settings> settings = settingsService.selectListByKeyName(keyName);
        if(null != settings && settings.size() > 0){
            v = settings.get(0).getValue();
            redisTemplate.opsForValue().set(keyName,v);
        }
        return v;
    }

}
