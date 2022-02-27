package com.ytdinfo.inndoo.controller.base.manage;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.ytdinfo.inndoo.common.constant.SettingConstant;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SettingUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Settings;
import com.ytdinfo.inndoo.modules.base.vo.UnbindingSetting;
import com.ytdinfo.inndoo.modules.base.vo.ProxySetting;
import com.ytdinfo.inndoo.modules.core.service.SettingsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "基本配置接口")
@RequestMapping("/base/setting")
public class SettingController {
    @Autowired
    private SettingsService settingsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SettingUtil settingUtil;

    @RequestMapping(value = "/seeSecret/{settingName}", method = RequestMethod.GET)
    @ApiOperation(value = "查看私密配置")
    public Result<Object> seeSecret(@PathVariable String settingName) {
        String result = "", v;
        if (SettingConstant.PROXY_SETTING.equals(settingName)) {
            v = redisTemplate.opsForValue().get(SettingConstant.PROXY_SETTING);
            if (StrUtil.isBlank(v)) {
                v = settingUtil.getSettingValue(SettingConstant.PROXY_SETTING);
                ProxySetting proxySetting = new Gson().fromJson(v, ProxySetting.class);
                result = proxySetting.getPassword();
            }

        }
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/proxy", method = RequestMethod.GET)
    @ApiOperation(value = "查看proxy配置")
    public Result<ProxySetting> proxy() {
        String v = redisTemplate.opsForValue().get(SettingConstant.PROXY_SETTING);
        if (StrUtil.isBlank(v)) {
            v = settingUtil.getSettingValue(SettingConstant.PROXY_SETTING);
        }
        if (StrUtil.isBlank(v)) {
            return new ResultUtil<ProxySetting>().setData(null);
        }
        ProxySetting proxySetting = new Gson().fromJson(v, ProxySetting.class);
        if (StringUtils.isNotEmpty(proxySetting.getPassword())) {
            proxySetting.setPassword("**********");
        }
        return new ResultUtil<ProxySetting>().setData(proxySetting);
    }

    @RequestMapping(value = "/proxy/set", method = RequestMethod.POST)
    @ApiOperation(value = "proxy配置")
    public Result<Object> proxySet(@ModelAttribute ProxySetting proxySetting) {
        Settings sets;
        String v = redisTemplate.opsForValue().get(SettingConstant.PROXY_SETTING);
        if (StrUtil.isNotBlank(v) && !proxySetting.getChanged()) {
            String key = new Gson().fromJson(v, ProxySetting.class).getPassword();
            proxySetting.setPassword(key);
        }
        List<Settings> selectSettings = settingsService.selectListByKeyName(SettingConstant.PROXY_SETTING);
        if (null != selectSettings && selectSettings.size() > 0) {
            sets = selectSettings.get(0);
            sets.setUpdateTime(new Date());
        } else {
            sets = new Settings();
            sets.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
            sets.setCreateTime(new Date());
        }
        sets.setKeyName(SettingConstant.PROXY_SETTING);
        sets.setValue(new Gson().toJson(proxySetting));
        settingsService.save(sets);
        redisTemplate.opsForValue().set(SettingConstant.PROXY_SETTING, sets.getValue());
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/unbindingSetting/query", method = RequestMethod.GET)
    @ApiOperation(value = "查看绑定配置")
    public Result<UnbindingSetting> queryUnbindingSetting() {
        String v = redisTemplate.opsForValue().get(SettingConstant.UNBINDING_SETTING);
        if (StrUtil.isBlank(v)) {
            v = settingUtil.getSettingValue(SettingConstant.UNBINDING_SETTING);
        }
        if (StrUtil.isBlank(v)) {
            return new ResultUtil<UnbindingSetting>().setData(null);
        }
        return new ResultUtil<UnbindingSetting>().setData(JSONUtil.toBean(v, UnbindingSetting.class));
    }

    @RequestMapping(value = "/unbindingSetting/set", method = RequestMethod.POST)
    @ApiOperation(value = "绑定配置")
    public Result<Object> setUnbindingSetting(@ModelAttribute UnbindingSetting bindingSetting) {
        Long accountUnbindTimes = bindingSetting.getAccountUnbindTimes();
        Long phoneUnbindTimes = bindingSetting.getPhoneUnbindTimes();
        Long accountUnbindDayInterval = bindingSetting.getAccountUnbindDayInterval();
        Long phoneUnbindDayInterval = bindingSetting.getPhoneUnbindDayInterval();
        if (accountUnbindTimes == 0L && phoneUnbindTimes == 0L && accountUnbindDayInterval == 0L && phoneUnbindDayInterval == 0L) {
            List<Settings> selectSettings = settingsService.selectListByKeyName(SettingConstant.UNBINDING_SETTING);
            if (null != selectSettings && selectSettings.size() > 0) {
                Settings sets = selectSettings.get(0);
                settingsService.delete(sets);
            }
            redisTemplate.delete(SettingConstant.UNBINDING_SETTING);
            return new ResultUtil<Object>().setData("设置成功");
        }
        if (accountUnbindTimes >= 0) {
            if (accountUnbindDayInterval == null || accountUnbindDayInterval.intValue() < 0) {
                return new ResultUtil<Object>().setErrorMsg("账户解绑时间间隔设置有误");
            }
        } else {
            return new ResultUtil<Object>().setErrorMsg("账户解绑次数设置有误");
        }

        if (phoneUnbindTimes >= 0) {
            if (phoneUnbindDayInterval == null || phoneUnbindDayInterval.intValue() < 0) {
                return new ResultUtil<Object>().setErrorMsg("手机号解绑时间间隔设置有误");
            }
        } else {
            return new ResultUtil<Object>().setErrorMsg("手机号解绑次数设置有误");
        }

        Settings sets;
        List<Settings> selectSettings = settingsService.selectListByKeyName(SettingConstant.UNBINDING_SETTING);
        if (null != selectSettings && selectSettings.size() > 0) {
            sets = selectSettings.get(0);
            sets.setUpdateTime(new Date());
        } else {
            sets = new Settings();
            sets.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
            sets.setCreateTime(new Date());
        }
        sets.setKeyName(SettingConstant.UNBINDING_SETTING);
        sets.setValue(new Gson().toJson(bindingSetting));
        settingsService.save(sets);
        redisTemplate.opsForValue().set(SettingConstant.UNBINDING_SETTING, sets.getValue());
        return new ResultUtil<Object>().setData(null);
    }
}
