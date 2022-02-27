package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.ApiConstant;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.vo.ActAccountVo;
import com.ytdinfo.inndoo.common.vo.ModifyDepartmentVo;
import com.ytdinfo.inndoo.common.vo.ModifyStaffVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.consumer.AchieveListPushActOutVo;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author timmy
 * @date 2019/10/30
 */
@Component
public class ActivityApiUtil {

    @XxlConf("activity.api.rooturl4private")
//    @XxlConf("activity.api.rooturl")
    private String rootUrl;
    //private String rootUrl = "http://localhost:5041/activity-api";

    @XxlConf("activity.api.appkey")
    private String appkey;

    @XxlConf("activity.api.appsecret")
    private String appSecret;

    private Map generateSignMap() {
        Map paramMap = new HashMap<>();
        return generateSignMap(paramMap);
    }

    private Map generateSignMap(Map paramMap) {
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
     * 活动平台绑定小核心账户
     *
     * @param coreAccountId
     * @param actAccountId
     * @return
     */
    public Result bindAccount(String coreAccountId, String actAccountId, String formId,Date bindTime) {
        Map map = new HashMap<>();
        map.put("coreAccountId", coreAccountId);
        map.put("actAccountId", actAccountId);
        map.put("formId", formId);
        map.put("bindTime", DateUtil.format(bindTime,"yyyy-MM-dd HH:mm:ss"));
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = rootUrl + ApiConstant.ACTIVITY_ACCOUNT_BIND + "?" + params;
        String content = HttpRequestUtil.get(url);
        if(StrUtil.isEmpty(content)){
            return null;
        }
        return JSONUtil.toBean(content, Result.class);
    }

    /**
     * 解绑act账户和core账户关系
     *
     * @param ids
     * @return
     */
    public Boolean untied(List<String> ids) {
        Map map = new HashMap<>();
        String coreAccountIds = JSONUtil.toJsonStr(ids);
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(rootUrl + ApiConstant.ACTIVITY_ACCOUNT_UNTIED, coreAccountIds);
        String content = HttpUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return true;
        }
        return false;
    }

    public Boolean untiedActAccountId(String actAccountId) {
        Map map = new HashMap<>();
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(rootUrl + ApiConstant.ACTIVITY_ACCOUNT_UNTIEDACT, actAccountId);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return true;
        }
        return false;
    }

    /***
     *
     * 接收小核心达标名单时间接口
     * @param achieveListPushActOutVo
     * @return
     */
    public boolean achieveListPushAct(AchieveListPushActOutVo achieveListPushActOutVo) {
        Map paramMap = generateSignMap(new HashMap<>());
        String params = HttpUtil.toParams(paramMap);
        String content = HttpRequestUtil.post(rootUrl + ApiConstant.ACTIVITY_ACHIEVELISTRECEIVE_RECEIVE + "?" + params,
                JSONUtil.toJsonStr(achieveListPushActOutVo));
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return true;
        }
        return false;
    }
    /**
     * 1.0数据注册数据迁移到2.0注册
     *
     * @return
     */
    public void dataMigration() {
        Map map = new HashMap<>();
        map.put("tenantId", UserContext.getTenantId());
        map.put("wxappid", UserContext.getAppid());
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url =rootUrl + ApiConstant.ACTIVITY_DATAMIGRATION + "?" + params;
        String content = HttpRequestUtil.get(url);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {

        }else {
            throw new RuntimeException("dataMigration error from core, error url:" + url + ",content:" + content);
        }
    }
    /**
     * 获取act绑定账户
     *
     * @param coreAccountId
     * @return
     */
    public List<ActAccountVo> getCoreAccountId(String coreAccountId) {
        Map map = new HashMap<>();
        map.put("tenantId", UserContext.getTenantId());
        map.put("wxappid", UserContext.getAppid());
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = StrUtil.format(rootUrl + ApiConstant.ACTIVITY_GETCOREACCOUNTID, coreAccountId);
        String content = HttpRequestUtil.get(url + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return JSONUtil.toList((JSONArray) result.getResult(), ActAccountVo.class);
        }
        throw new RuntimeException("getCoreAccountId error from act, error url:" + url + "error message:" + coreAccountId);
    }

    public Map getActCustomAPIData(String urlSuffix, Map<String, String> map) {
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String content = HttpRequestUtil.get(rootUrl + urlSuffix + "?" + params);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess() && result.getResult() != null && !"null".equals(result.getResult().toString())) {
            return JSONUtil.toBean(result.getResult().toString(), Map.class);
        }
        return null;
    }

    /**
     * 记录租户接口费用
     */
    public String noteApiCost(Byte costType){
        Map map = new HashMap<>();
        map.put("costType", costType);
        map.put("expenseTime", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = rootUrl + ApiConstant.ACTIVITY_APICOST_NOTE + "?" + params;
        String content = HttpRequestUtil.get(url);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            return result.getResult().toString();
        }
        return StrUtil.EMPTY;
    }

    /**
     * 修改员工信息
     * @param staff
     * @return
     */
    public Boolean modifyStaff(ModifyStaffVo staff) {
        try{
            if(staff == null){
                return false;
            }
            //推出的数据，先解成明文，然后用通用密码加密
            //先解密，再加密
            if(StringUtils.isNotEmpty(AESUtil.PRIVATEPASSWORD)) {
                if(StringUtils.isNotEmpty(staff.getOldName()))
                {
                    staff.setOldName(AESUtil.comEncrypt(AESUtil.decrypt(staff.getOldName())));
                }
            }
            Map paramMap =  new HashMap<>();
            paramMap.put("modifyStaff",JSONUtil.toJsonStr(staff));
            String params = HttpUtil.toParams(paramMap);
            String url = StrUtil.format(rootUrl + ApiConstant.ACTIVITY_STAFF_MODIFY);
            String content = HttpRequestUtil.post(url,params);
            Result result = JSONUtil.toBean(content, Result.class);
            if (result.isSuccess()) {
                return true;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * 变更机构信息
     * @param department
     * @return
     */
    public Boolean modiifyDepartment(ModifyDepartmentVo department){
        try{
            if(department == null){
                return false;
            }
            Map paramMap =  new HashMap<>();
            String  toJsonStr=new Gson().toJson(department);
            //String toJsonStr =JSONUtil.toJsonStr(department);
            paramMap.put("modiifyDepartment",toJsonStr);
            String params = HttpUtil.toParams(paramMap);
            String url = StrUtil.format(rootUrl + ApiConstant.ACTIVITY_DEPARTMENT_MODIFY);
            String content = HttpRequestUtil.post(url,params);
            Result result = JSONUtil.toBean(content, Result.class);
            if (result.isSuccess()) {
                return true;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }
    public ActAccountVo getByActAccountId(String tenantId, String appid, String actAccountId) {
        Map map = new HashMap<>();
        map.put("tenantId", tenantId);
        map.put("wxappid", appid);
        map.put("actAccountId", actAccountId);
        Map paramMap = generateSignMap(map);
        Map<String, String> headers = new HashMap<>();
        headers.put(SecurityConstant.TENANT_ID, tenantId);
        headers.put(SecurityConstant.WXAPPID, appid);
        String params = HttpUtil.toParams(paramMap);
        String apiUrl = ApiConstant.ACTIVITY_ACCOUNT_GETBYACTACCOUNTID;
        String url = rootUrl + apiUrl + "?" + params;
        HttpRequest httpRequest = HttpUtil.createGet(url);
        httpRequest.addHeaders(headers);
        String body = httpRequest.execute().body();
        Result result = JSONUtil.toBean(body, Result.class);;
        if (result.isSuccess()) {
            return JSONUtil.toBean(result.getResult().toString(), ActAccountVo.class);
        }
        throw new RuntimeException("queryByName error from core, error url:" + url );
    }

    /**
     * 判断校验名单是否被占用
     * @param listType
     * @param listId
     * @return
     */
    public Result checkEmployList(@RequestParam String listType, @RequestParam String listId) {
        Map map = new HashMap<>();
        map.put("listType", listType);
        map.put("listId", listId);
        Map paramMap = generateSignMap(map);
        String params = HttpUtil.toParams(paramMap);
        String url = rootUrl + ApiConstant.ACTIVITY_COMMON_CHECKEMPLOYLIST + "?" + params;
        String content = HttpRequestUtil.get(url);
        if(StrUtil.isEmpty(content)){
            return null;
        }
        return JSONUtil.toBean(content, Result.class);
    }

    /**
     * 根据accountId获取uuid
     * @param accountId
     * @return
     */
    public String getShzhUuid(@RequestParam String accountId) {
        String url = rootUrl + ApiConstant.ACTIVITY_SHZH_GETUUID ;
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("accountId", accountId);
        String bodyJson = JSONUtil.toJsonStr(bodyMap);
        String resultStr = HttpRequestUtil.post(url, headers, bodyJson);
        return resultStr;
    }

}