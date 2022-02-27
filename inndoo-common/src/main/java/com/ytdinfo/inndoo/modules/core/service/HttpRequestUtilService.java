package com.ytdinfo.inndoo.modules.core.service;

import java.util.Map;

/**
 * @author zhuzheng
 * @desc 包装com.ytdinfo.inndoo.common.utils.HttpRequestUtil以便SPRING_AOP
 */
public interface HttpRequestUtilService {

    String get(String urlString);

    String get(String urlString, Map<String, Object> paramMap);

    /**
     * aop
     * @param urlString
     * @param headerMap
     * @param paramMap
     * @return
     */
    String get(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap);

    String post(String urlString, Map<String, Object> paramMap);

    /**
     * aop
     * @param urlString
     * @param headerMap
     * @param paramMap
     * @return
     */
    String post(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap);

}