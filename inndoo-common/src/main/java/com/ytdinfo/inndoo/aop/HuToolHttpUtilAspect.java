package com.ytdinfo.inndoo.aop;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

/**
 * Spring AOP实现动态接口日
 *
 * @author zhuzheng
 */
@Aspect
@Component
@Slf4j
public class HuToolHttpUtilAspect {

    private static final ThreadLocal<Long> requestTime = new NamedThreadLocal<Long>("HuToolHttpUtilRequestTime");

    @Autowired
    private RedisTemplate<String,ApiRequestLog> redisTemplate;

    private static final String CACHE_KEY = "batch:ApiRequestLogDelayInsert";

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.get(..)) && args(urlString))")
    public void urlStringGet(String urlString) {

    }

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.get(..)) && args(urlString,paramMap))")
    public void urlStringParamMapGet(String urlString, Map<String, Object> paramMap) {

    }

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.get(..)) && args(urlString,headerMap,paramMap))")
    public void urlStringHeaderMapParamMapGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {

    }

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.get(..)) && args(urlString,headerMap,paramMap,milliseconds))")
    public void urlStringHeaderMapParamMapMillisecondsGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, int milliseconds) {

    }

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.post(..)) && args(urlString,paramMap))")
    public void urlStringParamMapPost(String urlString, Map<String, Object> paramMap) {

    }

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.post(..)) && args(urlString,body))")
    public void urlStringBodyPost(String urlString, String body) {

    }

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.post(..)) && args(urlString,headerMap,paramMap))")
    public void urlStringHeaderMapParamMapPost(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {

    }

    @Pointcut("execution(public String com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService.post(..)) && args(urlString,headerMap,body))")
    public void urlStringHeaderMapBodyPost(String urlString, Map<String, String> headerMap, String body) {

    }

    @Before("urlStringGet(urlString)")
    public void doBeforeGet(String urlString) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringGet(urlString)", throwing = "exception")
    public void afterThrowingGet(String urlString, Exception exception) {
        apiRequestLog(Method.GET, urlString, null, null, exception);
    }

    @AfterReturning(value = "urlStringGet(urlString)", returning = "result")
    public void afterGet(String urlString, String result) {
        apiRequestLog(Method.GET, urlString,null,null ,result);
    }


    @Before("urlStringParamMapGet(urlString,paramMap)")
    public void doBeforeGet(String urlString, Map<String, Object> paramMap) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringParamMapGet(urlString,paramMap)", throwing = "exception")
    public void afterThrowingGet(String urlString, Map<String, Object> paramMap, Exception exception) {
        apiRequestLog(Method.GET, urlString, null, JSONUtil.toJsonStr(paramMap), exception);
    }

    @AfterReturning(value = "urlStringParamMapGet(urlString,paramMap)", returning = "result")
    public void afterGet(String urlString, Map<String, Object> paramMap, String result) {
        apiRequestLog(Method.GET, urlString,null, JSONUtil.toJsonStr(paramMap), result);
    }

    @Before("urlStringParamMapPost(urlString,paramMap)")
    public void doBeforePost(String urlString, Map<String, Object> paramMap) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringParamMapPost(urlString,paramMap)", throwing = "exception")
    public void afterThrowingPost(String urlString, Map<String, Object> paramMap, Exception exception) {
        apiRequestLog(Method.POST, urlString, null, JSONUtil.toJsonStr(paramMap), exception);
    }

    @AfterReturning(value = "urlStringParamMapPost(urlString,paramMap)", returning = "result")
    public void afterPost(String urlString, Map<String, Object> paramMap, String result) {
        apiRequestLog(Method.POST, urlString,null, JSONUtil.toJsonStr(paramMap), result);
    }

    @Before("urlStringBodyPost(urlString,body)")
    public void doBeforePost(String urlString, String body) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringBodyPost(urlString,body)", throwing = "exception")
    public void afterThrowingPost(String urlString, String body, Exception exception) {
        apiRequestLog(Method.POST, urlString, null, body, exception);
    }

    @AfterReturning(value = "urlStringBodyPost(urlString,body)", returning = "result")
    public void afterPost(String urlString, String body, String result) {
        apiRequestLog(Method.POST, urlString,null, body, result);
    }


    @Before("urlStringHeaderMapParamMapGet(urlString,headerMap,paramMap)")
    public void doBeforeGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringHeaderMapParamMapGet(urlString,headerMap,paramMap)", throwing = "exception")
    public void afterThrowingGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, Exception exception) {
        apiRequestLog(Method.GET, urlString, headerMap, JSONUtil.toJsonStr(paramMap), exception);
    }

    @AfterReturning(value = "urlStringHeaderMapParamMapGet(urlString,headerMap,paramMap)", returning = "result")
    public void afterGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, String result) {
        apiRequestLog(Method.GET, urlString, headerMap, JSONUtil.toJsonStr(paramMap), result);
    }

    @Before("urlStringHeaderMapParamMapMillisecondsGet(urlString,headerMap,paramMap, milliseconds)")
    public void doBeforeGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, int milliseconds) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringHeaderMapParamMapMillisecondsGet(urlString,headerMap,paramMap, milliseconds)", throwing = "exception")
    public void afterThrowingGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, int milliseconds, Exception exception) {
        apiRequestLog(Method.GET, urlString, headerMap, JSONUtil.toJsonStr(paramMap), exception);
    }

    @AfterReturning(value = "urlStringHeaderMapParamMapMillisecondsGet(urlString,headerMap,paramMap, milliseconds)", returning = "result")
    public void afterGet(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, int milliseconds, String result) {
        apiRequestLog(Method.GET, urlString, headerMap, JSONUtil.toJsonStr(paramMap), result);
    }


    @Before("urlStringHeaderMapParamMapPost(urlString,headerMap,paramMap)")
    public void doBeforePost(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringHeaderMapParamMapPost(urlString,headerMap,paramMap)", throwing = "exception")
    public void afterThrowingPost(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, Exception exception) {
        apiRequestLog(Method.POST, urlString, headerMap, JSONUtil.toJsonStr(paramMap), exception);
    }

    @AfterReturning(value = "urlStringHeaderMapParamMapPost(urlString,headerMap,paramMap)", returning = "result")
    public void afterPost(String urlString, Map<String, String> headerMap, Map<String, Object> paramMap, String result) {
        apiRequestLog(Method.POST, urlString, headerMap, JSONUtil.toJsonStr(paramMap), result);
    }

    @Before("urlStringHeaderMapBodyPost(urlString,headerMap,body)")
    public void doBeforePost(String urlString, Map<String, String> headerMap, String body) {
        requestTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(value = "urlStringHeaderMapBodyPost(urlString,headerMap,body)", throwing = "exception")
    public void afterThrowingPost(String urlString, Map<String, String> headerMap, String body, Exception exception) {
        apiRequestLog(Method.POST, urlString, headerMap, body, exception);
    }

    @AfterReturning(value = "urlStringHeaderMapBodyPost(urlString,headerMap,body)", returning = "result")
    public void afterPost(String urlString, Map<String, String> headerMap, String body, String result) {
        apiRequestLog(Method.POST, urlString, headerMap, body, result);
    }

    private void apiRequestLog(Method method, String urlString, Map<String, String> headerMap, String body, Exception ex) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        ex.printStackTrace(ps);
        String exception = ex.getMessage() + "\n" + os.toString();
        String error = ExceptionUtil.stacktraceToString(ex);
        apiRequestLog(method, urlString, headerMap, body, "",exception+"\n"+error);
    }

    private void apiRequestLog(Method method, String urlString, Map<String, String> headerMap, String body, String result) {
        apiRequestLog(method, urlString, headerMap, body, result,"");
    }

    private void apiRequestLog(Method method, String urlString, Map<String, String> headerMap, String body, String result,String exception) {
        // TODO:记录api请求日志
        ApiRequestLog log = new ApiRequestLog();
        log.setMethod(method.name());
        log.setUrl(urlString);
        if (headerMap != null) {
            log.setRequestHeader(JSONUtil.toJsonStr(headerMap));
        }
        if (body != null) {
            log.setRequestBody(body);
        }
        log.setResponseBody(result);
        log.setException(exception);
        log.setRequestTime(System.currentTimeMillis() - requestTime.get());
//        logService.save(log);
        redisTemplate.opsForSet().add(CACHE_KEY,log);

    }

}