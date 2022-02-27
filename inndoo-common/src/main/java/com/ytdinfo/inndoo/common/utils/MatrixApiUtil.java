package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.ApiConstant;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.entity.WxAuthorizer;
import com.ytdinfo.inndoo.modules.core.entity.ActivityDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by timmy on 2019/8/12.
 */
@Component
@Slf4j
public class MatrixApiUtil {

    @XxlConf("matrix.matrixapi.rooturl")
    private String matrixRootUrl;
//    private String matrixRootUrl = "http://localhost:5024/matrix-api";

    @XxlConf("core.matrixapi.appkey")
    private String appkey;

    @XxlConf("core.matrixapi.appsecret")
    private String appSecret;

    @XxlConf("core.verifyimage.verifyimageurl")
    private static String VALIDATE = "/api/verifyimage";

    @Autowired
    private SecurityUtil securityUtil;

    public Map generateSignMap() {
        Map paramMap = new HashMap<>();
        return generateSignMap(paramMap);
    }
    /**
     * 根据手机号授权的租户清单
     *
     * @return
     */
    public List<Tenant> getTenantByMobile(String mobile) {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_TENANT_MOBILELIST, mobile);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray) result.getResult(), Tenant.class);
        }
        throw new RuntimeException("get usertenent from matrix error" + result.getMessage());
    }
    public Map generateSignMap(Map paramMap) {
        paramMap.put("appkey", appkey);
        paramMap.put("timestamp", System.currentTimeMillis());
        paramMap.put("appsecret", appSecret);
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        paramMap.put("sign", sign);
        paramMap.remove("appsecret");
        return paramMap;
    }

    /**
     * 根据id获取开放平台信息
     * @return
     */
    public WxopenComponentInfo getComponentInfo(String id) {
        Map map = new HashMap<>();
        map.put("id",id);
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_COMPONENT_QUERY, id);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toBean((JSONObject) result.getResult(), WxopenComponentInfo.class);
        }
        throw new RuntimeException("get getComponentInfoList from matrix error" + result.getMessage());
    }

    /**
     * 获取所有接入开放平台清单
     * @return
     */
    public List<WxopenComponentInfo> getComponentInfoList() {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String content = HttpRequestUtil.get(matrixRootUrl + ApiConstant.MATRIX_COMPONENT_LIST + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray)result.getResult(), WxopenComponentInfo.class);
        }
        throw new RuntimeException("get getComponentInfoList from matrix error" + result.getMessage());
    }

    /**
     * 获取所有授权公众号清单
     * @return
     */
    public List<WxAuthorizer> getWxAuthorizerList() {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String content = HttpRequestUtil.get(matrixRootUrl + ApiConstant.MATRIX_WXAUTHORIZER_LIST + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray)result.getResult(), WxAuthorizer.class);
        }
        throw new RuntimeException("get getWxAuthorizerList from matrix error" + result.getMessage());
    }

    /**
     * 获取所有授权公众号清单
     * @return
     */
    public List<WxAuthorizer> getWxAuthorizerListByTenant(String tenantId) {
        User currUser = securityUtil.getCurrUser();
        Map map = new HashMap<>();
        map.put("userid",currUser.getId());
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_WXATHORIZER_LIST_BYTENANT, tenantId);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray)result.getResult(), WxAuthorizer.class);
        }
        throw new RuntimeException("get getWxAuthorizerList from matrix error" + result.getMessage());
    }
    /**
     * Mobile获取所有授权公众号清单
     * @return
     */
    public List<WxAuthorizer> getWxAuthorizerListByTenantAndMobile(String tenantId) {
        User currUser = securityUtil.getCurrUser();
        Map map = new HashMap<>();
        map.put("mobile",currUser.getMobile());
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_WXATHORIZER_LIST_BYTENANTANDMOBILE, tenantId);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray)result.getResult(), WxAuthorizer.class);
        }
        throw new RuntimeException("get getWxAuthorizerList from matrix error" + result.getMessage());
    }
    /**
     * 更新授权token
     * @param wxAuthorizerToken
     */
    public void updateWxAuthorizerToken(WxAuthorizerToken wxAuthorizerToken) {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String content = HttpRequestUtil.post(matrixRootUrl + ApiConstant.MATRIX_UPDATE_WXAUTHORIZERTOKEN + "?" + params, JSONUtil.toBean(JSONUtil.toJsonStr(wxAuthorizerToken),HashMap.class));
        Result result = JSONUtil.toBean(content, Result.class);
        if(!result.isSuccess()){
            throw new RuntimeException("get updateWxAuthorizerToken error from matrix, error message:" + result.getMessage());
        }
    }
    /**
     * 根据appid获取授权token
     * @param appid
     * @return
     */
    public WxAuthorizerToken getWxAuthorizerToken(String appid) {
        Map paramMap = new HashMap<>();
        paramMap.put("appid", appid);
        paramMap = generateSignMap(paramMap);
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_GET_WXAUTHORIZERTOKEN, appid);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if(result.isSuccess()){
            return JSONUtil.toBean((JSONObject)result.getResult(),WxAuthorizerToken.class);
        }
        throw new RuntimeException("get WxAuthorizerToken error from matrix, error message:" + result.getMessage());
    }

    /**
     * 获取授权公众号信息
     *
     * @return
     */
    public WxAuthorizer getWxAuthorizer(String appid) {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_WXATHORIZER_QUERY, appid);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            WxAuthorizer wxAuthorizer = JSONUtil.toBean((JSONObject) result.getResult(), WxAuthorizer.class);
            return wxAuthorizer;
        }else{
            throw new RuntimeException("get getWxAuthorizer from matrix error" + result.getMessage());
        }
    }
    /**
     * 获取所有租户清单
     * @return
     */
    public List<Tenant> getTenantList() {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = matrixRootUrl + ApiConstant.MATRIX_TENANT_LIST;
//        log.error("getTenantListurl:"+url);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray)result.getResult(), Tenant.class);
        }
        log.error("getTenantList:"+content);
        throw new RuntimeException("get tenent from matrix error" + result.getMessage());
    }
    public List<Tenant> getTenantByMoblie(String mobile) {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_TENANT_USERLIST_MOBILE, mobile);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray) result.getResult(), Tenant.class);
        }
        throw new RuntimeException("get usertenent from matrix error" + result.getMessage());
    }
    /**
     * 根据Id获取租户信息
     * @param id
     * @return
     */
    public Tenant getTenant(String id) {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_TENANT_QUERY, id);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if(result.isSuccess()){
            return JSONUtil.toBean((JSONObject)result.getResult(),Tenant.class);
        }
        throw new RuntimeException("get tenant error from matrix, error message:" + result.getMessage());
    }
    /**
     * 获取所有数据源清单
     * @return
     */
    public List<ActivityDataSource> getActivityDataSourceList() {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = matrixRootUrl + ApiConstant.MATRIX_ACTIVITYDATASOURCE_LIST;
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray)result.getResult(), ActivityDataSource.class);
        }
        throw new RuntimeException("get tenent from matrix error" + result.getMessage());
    }
    /**
     * 根据id获取数据源信息
     * @param id
     * @return
     */
    public ActivityDataSource getActivityDataSource(String id) {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_ACTIVITYDATASOURCE_QUERY, id);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if(result.isSuccess()){
            return JSONUtil.toBean((JSONObject)result.getResult(),ActivityDataSource.class);
        }
        throw new RuntimeException("get activitydatasource error from matrix, error message:" + result.getMessage());
    }


    public TenantStaffRoleLimitVo getTenantStaffRoleLimitByRoleName(String roleName){
        Map map = new HashMap();
        map.put("roleName",roleName);
        map.put("tentId",UserContext.getTenantId());
        Map paramMap =generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String content = HttpRequestUtil.get(matrixRootUrl + ApiConstant.MATRIX_TENANTSTAFFROLELIMIT_GETTENANTSTAFFROLELIMITBYROLENAME + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if(result.isSuccess()){
            if(null != result.getResult() && !"null".equals(result.getResult().toString()) ){
                return JSONUtil.toBean((JSONObject)result.getResult(),TenantStaffRoleLimitVo.class);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 获取当前用户授权的租户清单
     * @return
     */
    public List<Tenant> getTenantByCurrentUser(){
        User currUser = securityUtil.getCurrUser();
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_TENANT_USERLIST, currUser.getId());
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray) result.getResult(), Tenant.class);
        }
        throw new RuntimeException("get usertenent from matrix error" + result.getMessage());
    }

    /**
     * 获取当前用户授权的租户清单
     * @return
     */
    public List<Tenant> getTenantByUserId(String userId){
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_TENANT_USERLIST, userId);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray) result.getResult(), Tenant.class);
        }
        throw new RuntimeException("get usertenent from matrix error" + result.getMessage());
    }

    /**
     * 获取授权公众号信息
     *
     * @return
     */
    @Cacheable(cacheNames = "WorkerId", key = "#host")
    @CacheExpire(expire = CommonConstant.SECOND_8HOUR)
    public Integer getWorkerId(String host) {
        Map paramMap = generateSignMap();
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(matrixRootUrl + ApiConstant.MATRIX_WORKERID_QUERY, host);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return Integer.parseInt(result.getResult().toString());
        }
        System.out.println("getWorkerId exception，return default value: 0");
        return 0;
    }

    /**
     * 验证图形验证码是否真确
     * @param ticket
     * @param urlprefix
     * @return
     */
    public static boolean check(String ticket,String urlprefix) {
        boolean pass=false;
        HashMap<String,String> map =new HashMap<String,String>();
        map.put("ticket",ticket);
        String content = HttpRequestUtil.post(urlprefix+VALIDATE ,JSONUtil.toJsonStr(map));
        try {
                if (Boolean.valueOf(content))
                {
                    pass=true;
                }

        } catch (Exception e) {

        }
        return pass;
    }

}