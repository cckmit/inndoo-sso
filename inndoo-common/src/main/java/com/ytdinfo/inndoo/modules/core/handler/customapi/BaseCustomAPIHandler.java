package com.ytdinfo.inndoo.modules.core.handler.customapi;

import com.ytdinfo.inndoo.common.vo.ResultCustomVo;

import java.util.Map;

public interface BaseCustomAPIHandler {
    ResultCustomVo process(String accountId, Map<String, String> defineMap, Map actMap, Map resultMap);

    Map<String,Object> getParams(String accountId);
}
