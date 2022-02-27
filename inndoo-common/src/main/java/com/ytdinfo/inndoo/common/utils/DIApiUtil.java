package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.qcloud.cos.utils.Jackson;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.DIApiConstant;
import com.ytdinfo.inndoo.common.dto.DIResult;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.core.entity.ExceptionLog;
import com.ytdinfo.inndoo.modules.core.service.ExceptionLogService;
import com.ytdinfo.inndoo.modules.core.service.HuToolHttpUtilService;
import com.ytdinfo.util.MD5Util;
import com.ytdinfo.util.StringUtils;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by nolan on 2020/8/11.
 * 数据平台接口
 */
@Slf4j
@Component
public class DIApiUtil {


    @XxlConf("core.di.rooturl")
    private String rooturl;

    @XxlConf("core.di.tokenrooturl")
    private String tokenRootUrl;

    @XxlConf("core.di.privid")
    private Integer privid;

    @XxlConf("core.di.tag.projectid")
    private Integer projectId;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private DictDataService dictDataService;

    @Autowired
    private ExceptionLogService exceptionLogService;

    @Autowired
    private HuToolHttpUtilService httpUtilService;

    private String getRooturl() {
        DictData dictData = dictDataService.findByTitle("DataPlatformUrl");
        if (dictData != null && StrUtil.isNotEmpty(dictData.getValue())) {
            return dictData.getValue();
        } else {
            return rooturl;
        }
    }

    private String getTokenRooturl() {
        DictData dictData = dictDataService.findByTitle("DataPlatformTokenUrl");
        if (dictData != null && StrUtil.isNotEmpty(dictData.getValue())) {
            return dictData.getValue();
        } else {
            return tokenRootUrl;
        }
    }

    /**
     * 获取token与userId
     *
     * @return
     */
    public Map<String, String> getToken() {
        String key = DIApiConstant.DI_COMPONENT_TOKEN_KEY + privid;
        String tokenJson = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(tokenJson)) {
            return new Gson().fromJson(tokenJson, new TypeToken<Map<String, String>>() {
            }.getType());
        }
        String msg = DIApiConstant.DI_SECRET_KEY + DIApiConstant.DI_TOKEN_USER_KEY + privid;
        String sign = MD5Util.md5(msg).toLowerCase();
        Map<String, Object> map = new HashMap<>();
        map.put("priv_id", privid);
        map.put("user_key", DIApiConstant.DI_TOKEN_USER_KEY);
        map.put("expires_in", DIApiConstant.DI_TOKEN_EXPIRES_IN);
        map.put("sign", sign);
//        String json = HttpRequestUtil.post(getTokenRooturl() + DIApiConstant.DI_COMPONENT_TOKEN, map);
        String json = httpUtilService.post(getTokenRooturl() + DIApiConstant.DI_COMPONENT_TOKEN, map);
        if (StringUtils.isEmpty(json)) {
            ExceptionLog log = new ExceptionLog();
            log.setUrl(DIApiConstant.DI_COMPONENT_TOKEN);
            log.setException("请求结果为空");
            log.setMsgBody("请求结果为空");
            exceptionLogService.save(log);
            return null;
        }
        DIResult diResult = Jackson.fromJsonString(json, DIResult.class);
        if (diResult.success()) {
            if (Objects.nonNull(diResult.getData())) {
                Map<String, String> tokenMap = new Gson().fromJson(diResult.getData().toString(), new TypeToken<Map<String, String>>() {
                }.getType());
                if (Objects.isNull(tokenMap) || !tokenMap.containsKey("token") || !tokenMap.containsKey("userId")) {
                    ExceptionLog log = new ExceptionLog();
                    log.setUrl(DIApiConstant.DI_COMPONENT_TOKEN);
                    log.setException("请求结果为空");
                    log.setMsgBody(json);
                    exceptionLogService.save(log);
                    return null;
                }
                String data = diResult.getData().toString();
                redisTemplate.opsForValue().set(key, data, DIApiConstant.DI_TOKEN_EXPIRES_IN - 100, TimeUnit.SECONDS);
                return tokenMap;
            } else {
                ExceptionLog log = new ExceptionLog();
                log.setUrl(DIApiConstant.DI_COMPONENT_TOKEN);
                log.setException("请求结果为空");
                log.setMsgBody(json);
                exceptionLogService.save(log);
            }
        } else {
            ExceptionLog log = new ExceptionLog();
            log.setUrl(DIApiConstant.DI_COMPONENT_TOKEN);
            log.setException(diResult.getErrorMsg());
            log.setMsgBody(diResult.getErrorMsg());
            exceptionLogService.save(log);
        }
        return null;
    }


    /**
     * 获取headMap
     *
     * @return
     */
    public Map<String, String> getHeadMap() {
        Map<String, String> tokenMap = getToken();
        if (Objects.isNull(tokenMap) || !tokenMap.containsKey("token") || !tokenMap.containsKey("userId")) {
            return null;
        }
        Map<String, String> headMap = new HashMap<>();
        String token = tokenMap.get("token");
        String userId = tokenMap.get("userId");
        if (userId.contains(".")) {
            userId = userId.substring(0, userId.indexOf("."));
        }
        headMap.put("ytdtoken", token);
        headMap.put("ytdUserId", userId);
        return headMap;
    }

    /**
     * 获取标签列表
     *
     * @param pid 父级分组id，顶级传-1
     * @return
     */
    public DIResult getTagList(String tenantId, Integer pid) {
        if (Objects.isNull(pid)) {
            pid = -1;
        }
        Map<String, String> headMap = getHeadMap();
        if (Objects.isNull(headMap)) {
            DIResult diResult = new DIResult();
            diResult.setSuccess(false);
            diResult.setErrorMsg("获取token失败");
            return diResult;
        }
        Map<String, Object> paramMap = new HashMap<>();
        if (pid > -2) {
            paramMap.put("pid", pid);
        }
        paramMap.put("projectId", tenantId);
//        String json = HttpRequestUtil.get(getRooturl() + DIApiConstant.DI_TAG_LIST, headMap, paramMap);
        String json = httpUtilService.get(getRooturl() + DIApiConstant.DI_TAG_LIST, headMap, paramMap);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return Jackson.fromJsonString(json, DIResult.class);
    }

    /**
     * 获取标签枚举值
     *
     * @param tagId 标签id
     * @return
     */
    public DIResult getTagValueEnum(Integer tagId) {
        if (Objects.isNull(tagId)) {
            DIResult result = new DIResult();
            result.setSuccess(false);
            result.setErrorMsg("参数错误");
            return result;
        }
        Map<String, String> headMap = getHeadMap();
        if (Objects.isNull(headMap)) {
            DIResult diResult = new DIResult();
            diResult.setSuccess(false);
            diResult.setErrorMsg("获取token失败");
            return diResult;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("tagId", tagId);
//        String json = HttpRequestUtil.get(getRooturl() + DIApiConstant.DI_TAG_VALUE_ENUM, headMap, paramMap);
        String json = httpUtilService.get(getRooturl() + DIApiConstant.DI_TAG_VALUE_ENUM, headMap, paramMap);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return Jackson.fromJsonString(json, DIResult.class);

    }

    /**
     * 获取缓存中标签的值
     *
     * @param tagId      标签id
     * @param distinctId 用户唯一标识符
     * @return
     */
    public DIResult getTagUserValue(Integer tagId, String distinctId) {
        if (Objects.isNull(tagId) || StringUtils.isEmpty(distinctId)) {
            DIResult result = new DIResult();
            result.setSuccess(false);
            result.setErrorMsg("参数错误");
            return result;
        }
        Map<String, String> headMap = getHeadMap();
        if (Objects.isNull(headMap)) {
            DIResult diResult = new DIResult();
            diResult.setSuccess(false);
            diResult.setErrorMsg("获取token失败");
            return diResult;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("tagId", tagId);
        paramMap.put("distinctId", distinctId);
//        String json = HttpRequestUtil.get(getRooturl() + DIApiConstant.DI_TAG_USER_VALUE, headMap, paramMap);
        String json = httpUtilService.get(getRooturl() + DIApiConstant.DI_TAG_USER_VALUE, headMap, paramMap, 5000);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return Jackson.fromJsonString(json, DIResult.class);
    }

    /**
     * 获取缓存中标签的值
     *
     * @param tagId      标签id
     * @param distinctId 用户唯一标识符
     * @return
     */
    public DIResult getTagUserValueBulkHead(Integer tagId, String distinctId) {
        ThreadPoolBulkhead poolBulkhead = BulkheadContainter.get("tag");
        if (poolBulkhead == null) {
            return getTagUserValue(tagId, distinctId);
        }
        Supplier<CompletionStage<DIResult>> completionStageSupplier = ThreadPoolBulkhead.decorateSupplier(poolBulkhead, () -> getTagUserValue(tagId, distinctId));
        Try<CompletionStage<DIResult>> completionStages = Try.ofSupplier(completionStageSupplier);
        try {
            return completionStages.get().toCompletableFuture().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置标签的失效时间
     *
     * @param tagId  标签id
     * @param expire 失效时间，格式：yyyy-MM-dd HH:mm:ss
     * @return
     */
    public DIResult setTagExpireTime(Integer tagId, String expire) {
        if (Objects.isNull(tagId) || StringUtils.isEmpty(expire)) {
            DIResult result = new DIResult();
            result.setSuccess(false);
            result.setErrorMsg("参数错误");
            return result;
        }
        Map<String, String> headMap = getHeadMap();
        if (Objects.isNull(headMap)) {
            DIResult diResult = new DIResult();
            diResult.setSuccess(false);
            diResult.setErrorMsg("获取token失败");
            return diResult;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("tagId", tagId);
        paramMap.put("expire", expire);
//        String json = HttpRequestUtil.post(getRooturl() + DIApiConstant.DI_TAG_EXPIRE_SET, headMap, paramMap);
        String json = httpUtilService.post(getRooturl() + DIApiConstant.DI_TAG_EXPIRE_SET, headMap, paramMap);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return Jackson.fromJsonString(json, DIResult.class);
    }

    /**
     * 更新标签缓存
     *
     * @param tagId 标签id
     * @return updateStatus是更新状态： -1 未更新 0 更新中 1 成功 2 失败
     */
    public DIResult refreshTagCache(Integer tagId) {
        if (Objects.isNull(tagId)) {
            DIResult result = new DIResult();
            result.setSuccess(false);
            result.setErrorMsg("参数错误");
            return result;
        }
        Map<String, String> headMap = getHeadMap();
        if (Objects.isNull(headMap)) {
            DIResult diResult = new DIResult();
            diResult.setSuccess(false);
            diResult.setErrorMsg("获取token失败");
            return diResult;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("tagId", tagId);
//        String json = HttpRequestUtil.get(getRooturl() + DIApiConstant.DI_TAG_CACHE_REFRESH, headMap, paramMap);
        String json = httpUtilService.get(getRooturl() + DIApiConstant.DI_TAG_CACHE_REFRESH, headMap, paramMap);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return Jackson.fromJsonString(json, DIResult.class);
    }

    /**
     * 获取缓存更新情况
     *
     * @param tagId 标签id
     * @return
     */
    public DIResult getTagCacheProgress(Integer tagId) {
        if (Objects.isNull(tagId)) {
            DIResult result = new DIResult();
            result.setSuccess(false);
            result.setErrorMsg("参数错误");
            return result;
        }
        Map<String, String> headMap = getHeadMap();
        if (Objects.isNull(headMap)) {
            DIResult diResult = new DIResult();
            diResult.setSuccess(false);
            diResult.setErrorMsg("获取token失败");
            return diResult;
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("tagId", tagId);
//        String json = HttpRequestUtil.get(getRooturl() + DIApiConstant.DI_TAG_CACHE_PROGRESS, headMap, paramMap);
        String json = httpUtilService.get(getRooturl() + DIApiConstant.DI_TAG_CACHE_PROGRESS, headMap, paramMap);
        System.out.println(json);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return Jackson.fromJsonString(json, DIResult.class);
    }

    /**
     * 清除token缓存
     */
    public void cleanTokenCache() {
        String key = DIApiConstant.DI_COMPONENT_TOKEN_KEY + privid;
        redisTemplate.delete(key);
    }
}
