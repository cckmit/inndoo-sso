package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.http.HttpUtil;
import com.ytdinfo.inndoo.common.utils.HttpRequestUtil;
import com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class HuToolHttpUtilServiceImpl implements HuToolHttpUtilService {

    @Override
    public String get(String urlString) {
        return HttpRequestUtil.get(urlString);
    }

    @Override
    public String get(String urlString, Map<String, Object> paramMap) {
        return HttpRequestUtil.get(urlString, paramMap);
    }

    @Override
    public String get(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {
        return HttpUtil.createGet(urlString).addHeaders(headerMap).form(paramMap).execute().body();
    }

    @Override
    public String get(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, int milliseconds) {
        return HttpUtil.createGet(urlString).timeout(milliseconds).addHeaders(headerMap).form(paramMap).execute().body();
    }

    @Override
    public String post(String urlString, Map<String, Object> paramMap) {
        return HttpRequestUtil.post(urlString, paramMap);
    }

    @Override
    public String post(String urlString, String body) {
        return HttpRequestUtil.post(urlString, body);
    }

    @Override
    public String post(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {
        return HttpRequestUtil.post(urlString,headerMap,paramMap);
    }

    @Override
    public String post(String urlString, Map<String, String> headerMap, String body) {
        return HttpRequestUtil.post(urlString,headerMap,body);
    }

}