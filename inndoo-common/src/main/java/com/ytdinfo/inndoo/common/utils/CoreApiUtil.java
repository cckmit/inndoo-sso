package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.ApiConstant;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import com.ytdinfo.inndoo.modules.core.entity.ExceptionLog;
import com.ytdinfo.inndoo.modules.core.service.ExceptionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CoreApiUtil {
    @XxlConf("core.api.rooturl4private")
   //@XxlConf("custom.core.api.rooturl")
    private String rootUrl ;
//     private String rootUrl = "http://localhost:5031/core-api";
    @XxlConf("core.api.appkey")
    private String appkey;

    @XxlConf("core.api.appsecret")
    private String appSecret;
    @Autowired
    private ExceptionLogService exceptionLogService;
    public Map generateSignMap() {
        Map paramMap = new HashMap<>();
        return generateSignMap(paramMap);
    }

    public Map generateSignMap(Map paramMap) {
        if (!paramMap.containsKey("tenantId")) {
            paramMap.put("tenantId", UserContext.getTenantId());
        }
        if (!paramMap.containsKey("wxappid")) {
            paramMap.put("wxappid", UserContext.getAppid());
        }
        paramMap.put("appkey", appkey);
        paramMap.put("timestamp", System.currentTimeMillis());
        paramMap.put("appsecret", appSecret);
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        paramMap.put("sign", sign);
        paramMap.remove("appsecret");
        return paramMap;
    }
    /**
     * 是否为员工校验
     *
     * @return
     */
    public boolean bindRegister(String coreAccountId, String actAccountId) {
        if (StrUtil.isEmpty(coreAccountId)) {
            return false;
        }
        Map map = new HashMap<>();
        map.put("id", coreAccountId);
        map.put("actAccountId", actAccountId);
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = rootUrl + ApiConstant.CORE_ACCOUNT_UNTIED + "?" + params;
        String content = HttpRequestUtil.get(url);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 活动平台和小核心平台账户绑定
     *
     * @return
     */
    public Result<String> bindAccount(String encryCoreAccountId, String encryActAccountId) {
        Map map = new HashMap<>();
        map.put("encryCoreAccountId", encryCoreAccountId);
        map.put("encryActAccountId", encryActAccountId);
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = rootUrl + ApiConstant.CORE_ACCOUNT_BINDACCOUNT + "?" + params;
        String content = HttpRequestUtil.get(url);
        String errMsg = null;
        if (StrUtil.isNotEmpty(content)) {
            Result result = JSONUtil.toBean(content, Result.class);
            return result;
        }
        throw new RuntimeException("bindAccount error from core, error url:" + url + "error message:" + content);
    }


    public AccountForm queryByName(String name, String tenantId, String appid){
        Map params = new HashMap();
        Map<String, String> headers = new HashMap<>();
        headers.put(SecurityConstant.TENANT_ID, tenantId);
        headers.put(SecurityConstant.WXAPPID, appid);
        params.put("name", name);
        Map paramMap = generateSignMap(params);
        String apiUrl = ApiConstant.CORE_QUERYBYNAME;
        String param = HttpUtil.toParams(paramMap);
        String url = rootUrl + apiUrl + "?" + param;
        HttpRequest httpRequest = HttpUtil.createGet(url);
        httpRequest.addHeaders(headers);
        String body = httpRequest.execute().body();
        Result result = JSONUtil.toBean(body, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toBean(result.getResult().toString(), AccountForm.class);
        }
        throw new RuntimeException("queryByName error from core, error url:" + url );
    }

    public AccountForm getIdentifierForm(String tenantId, String appid){
        Map params = new HashMap();
        Map<String, String> headers = new HashMap<>();
        headers.put(SecurityConstant.TENANT_ID, tenantId);
        headers.put(SecurityConstant.WXAPPID, appid);
        Map paramMap = generateSignMap(params);
        String apiUrl = ApiConstant.CORE_GETIDENTIFIERFORM;
        String param = HttpUtil.toParams(paramMap);
        String url = rootUrl + apiUrl + "?" + param;
        HttpRequest httpRequest = HttpUtil.createGet(url);
        httpRequest.addHeaders(headers);
        String body = httpRequest.execute().body();
        Result result = JSONUtil.toBean(body, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toBean(result.getResult().toString(), AccountForm.class);
        }
        throw new RuntimeException("queryByName error from core, error url:" + url );
    }

    public Account getCoreAccountByActAccountId(String actAccountId, String tenantId, String appid){
        Map params = new HashMap();
        params.put("actAccountId",actAccountId);
        Map<String, String> headers = new HashMap<>();
        headers.put(SecurityConstant.TENANT_ID, tenantId);
        headers.put(SecurityConstant.WXAPPID, appid);
        Map paramMap = generateSignMap(params);
        String apiUrl = ApiConstant.CORE_ACCOUNT_GETCOREACCOUNTBYACTACCOUNTID;
        String param = HttpUtil.toParams(paramMap);
        String url = rootUrl + apiUrl + "?" + param;
        HttpRequest httpRequest = HttpUtil.createGet(url);
        httpRequest.addHeaders(headers);
        String body = httpRequest.execute().body();
        Result result = JSONUtil.toBean(body, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toBean(result.getResult().toString(), Account.class);
        }
        throw new RuntimeException("queryByName error from core, error url:" + url );
    }

    public Result<String> saveAndUpdateAccount(Account account, String tenantId, String appid, String formId){
        Boolean boo = false;
        Map params = new HashMap();
        params.put("code","");
        params.put("phone","");
        params.put("formId",formId);
        Map<String, String> headers = new HashMap<>();
        headers.put(SecurityConstant.TENANT_ID, tenantId);
        headers.put(SecurityConstant.WXAPPID, appid);
        headers.put("userid",account.getActAccountId());
        Map paramMap = generateSignMap(params);
        String apiUrl = ApiConstant.CORE_ACCOUNT_SAVEANDUPDATE;
        String param = HttpUtil.toParams(paramMap);
        String url = rootUrl + apiUrl +  "?"+param;
        HttpRequest httpRequest = HttpUtil.createPost(url);
        httpRequest.addHeaders(headers);
        String Json = JSONUtil.toJsonStr(account);
        httpRequest.body(Json);
        String body = httpRequest.execute().body();
        Result result = JSONUtil.toBean(body, Result.class);
        return result;
    }

}
