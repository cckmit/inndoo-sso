package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.common.utils.HttpRequestUtil;
import com.ytdinfo.inndoo.modules.core.service.HttpRequestUtilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class HttpRequestUtilServiceImpl implements HttpRequestUtilService {

    @Override
    public String get(String urlString) {
        return HttpRequestUtil.get(urlString);
    }

    @Override
    public String get(String urlString, Map<String, Object> paramMap) {
        return HttpRequestUtil.get(urlString,paramMap);
    }

    @Override
    public String get(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {
        return HttpRequestUtil.get(urlString,headerMap,paramMap);
    }

    @Override
    public String post(String urlString, Map<String, Object> paramMap) {
        return HttpRequestUtil.post(urlString,paramMap);
    }

    @Override
    public String post(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {
        return HttpRequestUtil.post(urlString,headerMap,paramMap);
    }

}