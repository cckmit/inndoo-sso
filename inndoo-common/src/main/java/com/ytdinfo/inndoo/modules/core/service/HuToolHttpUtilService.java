package com.ytdinfo.inndoo.modules.core.service;

import java.util.Map;

/**
 * @author zhuzheng
 * @desc 包装cn.hutool.http.HttpUtil以便SPRING_AOP
 */
public interface HuToolHttpUtilService {

    String get(String urlString);

    String get(String urlString, Map<String, Object> paramMap);

    /**
     * aop
     * @param urlString
     * @param headerMap
     * @param paramMap
     * @return
     */
    String get(String urlString, Map<String, String> headerMap,Map<String, Object> paramMap);

    /**
     * aop
     * @param urlString
     * @param headerMap
     * @param paramMap
     * @param milliseconds
     * @return
     */
    String get(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, int milliseconds);

    String post(String urlString, Map<String, Object> paramMap);

    String post(String urlString, String body);

    /**
     * aop
     * @param urlString
     * @param headerMap
     * @param paramMap
     * @return
     */
    String post(String urlString, Map<String, String> headerMap,Map<String, Object> paramMap);

    /**
     * aop
     * @param urlString
     * @param headerMap
     * @param body
     * @return
     */
    String post(String urlString, Map<String, String> headerMap,String body);

}