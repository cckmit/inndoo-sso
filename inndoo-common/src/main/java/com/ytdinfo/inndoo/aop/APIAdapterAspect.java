package com.ytdinfo.inndoo.aop;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.apiadapter.APIRequest;
import com.ytdinfo.inndoo.apiadapter.APIResponse;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Spring AOP实现动态接口日
 *
 * @author zhuzheng
 */
@Aspect
@Component
@Slf4j
public class APIAdapterAspect {

    private static final ThreadLocal<Long> requestTime = new NamedThreadLocal<Long>("APIAdapterRequestTime");

    @Autowired
    private RedisTemplate<String,ApiRequestLog> redisTemplate;

    private static final String CACHE_KEY = "batch:ApiRequestLogDelayInsert";

    @Pointcut("execution(public com.ytdinfo.inndoo.apiadapter.APIResponse com.ytdinfo.inndoo.modules.core.service.APIAdapterService.request(..)) && args(request,clazz))")
    public void apiAdapterRequest(APIRequest request, Class clazz) {

    }

    @Before("apiAdapterRequest(request,clazz)")
    public void doBeforeGet(APIRequest request, Class clazz) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "apiAdapterRequest(request,clazz)", throwing = "exception")
    public void afterThrowingGet(APIRequest request, Class clazz, Exception exception) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        exception.printStackTrace(ps);
        String ex = exception.getMessage() + "\n" + os.toString();
        String error = ExceptionUtil.stacktraceToString(exception);
        
        ApiRequestLog log = new ApiRequestLog();
        log.setUrl(request.getRootUrl());
        log.setRequestBody(JSONUtil.toJsonStr(request));
        log.setResponseBody("");
        log.setException(ex + "\n" + error);
        log.setRequestTime(System.currentTimeMillis()-requestTime.get());
//        logService.save(log);
        redisTemplate.opsForSet().add(CACHE_KEY,log);
    }

    @AfterReturning(value = "apiAdapterRequest(request,clazz)", returning = "response")
    public void afterGet(APIRequest request, Class clazz, APIResponse response) {
        // TODO:记录api请求日志
        ApiRequestLog log = new ApiRequestLog();
        log.setUrl(request.getRootUrl()+response.getUrl());
        log.setRequestBody(JSONUtil.toJsonStr(request));
        log.setResponseBody(JSONUtil.toJsonStr(response));
        log.setRequestTime(System.currentTimeMillis()-requestTime.get());
//        logService.save(log);
        redisTemplate.opsForSet().add(CACHE_KEY,log);
    }

}