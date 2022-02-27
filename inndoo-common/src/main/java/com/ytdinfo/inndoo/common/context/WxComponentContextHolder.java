package com.ytdinfo.inndoo.common.context;

import com.ytdinfo.inndoo.common.vo.WxopenComponentInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author timmy
 * @date 2019/9/3
 */
public class WxComponentContextHolder {
    private static final Map<String, WxopenComponentInfo> map = new HashMap<>();

    public static synchronized void init(List<WxopenComponentInfo> wxopenComponentInfoList) {
        for (WxopenComponentInfo wxopenComponentInfo : wxopenComponentInfoList) {
            map.put(wxopenComponentInfo.getId(), wxopenComponentInfo);
        }
    }

    public static synchronized void add(WxopenComponentInfo wxopenComponentInfo){
        map.put(wxopenComponentInfo.getId(),wxopenComponentInfo);
    }

    public static synchronized void remove(String id){
        map.remove(id);
    }

    public static WxopenComponentInfo get(String id){
        return map.get(id);
    }
}