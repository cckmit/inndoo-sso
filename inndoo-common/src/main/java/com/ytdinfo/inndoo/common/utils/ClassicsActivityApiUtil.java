package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.EncryptUtils;
import com.google.common.base.Joiner;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.ClassicActivityApiConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.dto.SimulationStaffRegistrationDto;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * 活动平台1.0 API
 *
 * @author zhuzhneg
 * @date 2020/07/17
 */
@Component
@Slf4j
public class ClassicsActivityApiUtil {

    @XxlConf("activity.api.classicsactivity.rooturl")
    private String rootUrl;

    private static final String PLACEHOLDER = "${appId}";


    @XxlConf("core.inndoo.xmgj.accesstokencachekey")
    private  String ACCESS_TOKEN_CACHE_KEY;

    @XxlConf("core.inndoo.xmgj.appid")
    private  String APP_ID;
    @XxlConf("core.inndoo.xmgj.appsecret")
    private  String APP_SECRET;
    @XxlConf("core.inndoo.xmgj.domainurl")
    private  String DOMAIN_URL = "http://xibtest.xib.com.cn:3080/ifspopenapi";
    @XxlConf("core.inndoo.xmgj.refreshtoken")
    private  String REFRESH_TOKEN;
    //证书私钥
    @XxlConf("core.inndoo.xmgj.pfxcontent")
    private  String pfxContent;
    //证书密码
    @XxlConf("core.inndoo.xmgj.password")
    private String password;


    @Autowired
    private AccountService accountService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取活动平台1.0的用户是否注册
     *
     * @param openId
     * @return
     */
    public Result<Boolean> checkRegistration(@NonNull String appId, @NonNull String openId,@NonNull String agreeCode) {
        if (StringUtils.isEmpty(rootUrl) || !rootUrl.contains(PLACEHOLDER)) {
            return new ResultUtil<Boolean>().setErrorMsg("请检查活动平台1.0请求地址配置");
        }
        String localRootUrl = rootUrl.replace(PLACEHOLDER, appId);
        String url = localRootUrl + ClassicActivityApiConstant.CA_GET_CHECKREGISTRATION + "?openId=" + openId+"&agreeCode="+agreeCode;
        String response = HttpRequestUtil.get(url);
        if (StringUtils.isEmpty(response)) {
            return new ResultUtil<Boolean>().setErrorMsg("请求异常");
        }
        JSONObject json = JSONUtil.parseObj(response);
        boolean success = Boolean.parseBoolean(json.getStr("success"));
        if (success) {
            return new ResultUtil<Boolean>().setData(true);
        } else {
            String code = json.getStr("code");
            String msg = json.getStr("msg");
            return new ResultUtil<Boolean>().setErrorMsg(Integer.parseInt(code), msg);
        }
    }


    /**
     * 判断用户手机号是否在1.0白名单中
     *
     * @param
     * @return
     */
    public Result<Boolean> checkWhiteList(String phone,String appId,String paramcode) {
        if (StringUtils.isEmpty(rootUrl) || !rootUrl.contains(PLACEHOLDER)) {
            return new ResultUtil<Boolean>().setErrorMsg("请检查活动平台1.0请求地址配置");
        }
        String localRootUrl = rootUrl.replace(PLACEHOLDER, appId);
        String url = localRootUrl + ClassicActivityApiConstant.CA_GET_CHECKWHITELIST + "?phone=" +phone+"&paramcode="+paramcode;
        String response = HttpRequestUtil.get(url);
        if (StringUtils.isEmpty(response)) {
            return new ResultUtil<Boolean>().setErrorMsg("请求异常");
        }
        JSONObject json = JSONUtil.parseObj(response);

        boolean success = Boolean.parseBoolean(json.getStr("data"));
        if (success) {
            return new ResultUtil<Boolean>().setData(true);
        } else {
            String code = json.getStr("code");
            String msg = json.getStr("msg");
            return new ResultUtil<Boolean>().setErrorMsg(Integer.parseInt(code), msg);
        }
    }



    /**
     * 厦门国际接口访问 -判断是否行内客户
     *
     * @param mobile
     * @return 客户身份标识
     * @throws Exception
     */
    public Result<Boolean> checkConsumerMobile(String mobile)  {

        String queryId =String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId());// UidUtil.generateOrderNo();
        //报文体body参数
        Map<String, String> bodyMap = new HashMap<>(1);
        bodyMap.put("mobile", mobile);
        try {
            JSONObject result = accessOpen("IOPEN113009", "CIM", queryId, bodyMap);
            if (result == null) {
                return new ResultUtil<Boolean>().setErrorMsg("请求异常：queryId=" + queryId);
            }
            String responseCode = result.getStr("responseCode");
            String responseMessage = result.getStr("responseMessage");
            if ("00000".equals(responseCode)) {
                JSONObject body = result.getJSONObject("body");
                String authCode = body.getStr("authCode");
                if (StringUtils.isNotEmpty(authCode)) {
                    //行内客户
                    return new ResultUtil<Boolean>().setData(true);
                } else {
                    //"非行内客户"
                    return new ResultUtil<Boolean>().setData(false);
                }
            } else {
                //返回不成功
                // return new ResultUtil<Boolean>().setData(false);
                return new ResultUtil<Boolean>().setErrorMsg(Integer.parseInt(responseCode), responseMessage);
            }
        }
        catch (Exception e)
        {
            return new ResultUtil<Boolean>().setErrorMsg(500, e.getMessage());
        }
    }


    /**
     * @param code
     * @param scope
     * @param queryId 请求服务流水号，注意要保证唯一，方便定位问题
     * @param bodyMap 报文体body参数
     * @throws Exception
     */
    private JSONObject accessOpen(String code, String scope, String queryId, Map<String, String> bodyMap) throws Exception {
        //init();
        //请求头
        Map<String, String> headers = new HashMap<>(1);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        //报文体header参数
        Map<String, String> headerMap = new HashMap<>(3);
        headerMap.put("version", "1.0");
        headerMap.put("queryId", queryId);
        headerMap.put("encrypt", "0");

        //报文体map
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("header", headerMap);
        paramMap.put("body", bodyMap);

        PrivateKey privateKey = DigitalCertificateEncryptUtils.readPrivateKey(pfxContent, password, "PKCS12");
        String plainText = getOrderByLexicographic(paramMap);
        String sign = DigitalCertificateEncryptUtils.sign(plainText, "SHA256WithRSA", privateKey);
        //报文体sign参数
        paramMap.put("sign", sign);
        String accessToken = getAccessToken();
        if (StringUtils.isEmpty(accessToken)) {
            return null;
        }
        String url = DOMAIN_URL + "/open/api?access_token=" + accessToken + "&client_id=" + APP_ID + "&code=" + code + "&scope=" + scope;
        String result = HttpRequestUtil.post( url, headers, paramMap);
        if (StringUtils.isNotEmpty(result)) {
            return JSONUtil.parseObj(result);
        } else {
            return null;
        }
    }



    private String getAccessToken() {
        if (redisTemplate.hasKey(ACCESS_TOKEN_CACHE_KEY)) {
            String accessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
            if (StringUtils.isNotBlank(accessToken)) {
                return accessToken;
            }
        }
        AjaxResult<HashMap<String, String>> result = accessToken();
        if (result.isSuccess()) {
            String accessToken = result.getData().get("accessToken");
            String expiresIn = result.getData().get("expiresIn");
            Date expiresDate = DateUtil.parse(expiresIn, "yyyy-MM-dd HH:mm:ss");
            // - 60 * 1000 :使缓存提前1分钟失效
            long timeout = expiresDate.getTime() - System.currentTimeMillis() - 60 * 1000;
            if (timeout > Integer.MAX_VALUE) {
                timeout = Integer.MAX_VALUE;
            }
            if (StringUtils.isEmpty(accessToken) || timeout <= 0) {
                result = refreshAccessToken();
                if (result.isSuccess()) {
                    accessToken = result.getData().get("accessToken");
                    expiresIn = result.getData().get("expiresIn");
                    expiresDate = DateUtil.parse(expiresIn, "yyyy-MM-dd HH:mm:ss");
                    timeout = expiresDate.getTime() - System.currentTimeMillis() - 60 * 1000;
                    if (timeout > Integer.MAX_VALUE) {
                        timeout = Integer.MAX_VALUE;
                    }
                    if (timeout <= 0) {
                        return null;
                    }
                    //redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, accessToken, timeout);
                    redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, accessToken, timeout,  TimeUnit.MILLISECONDS);

                    return accessToken;
                }
                return null;
            }
           //redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, accessToken, timeout);
            redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, accessToken, timeout,  TimeUnit.MILLISECONDS);
            return accessToken;
        }
        return null;
    }


    /**
     * 获取accessToken
     */
    private AjaxResult<HashMap<String, String>> accessToken() {
        //init();
        String queryStr = "client_id=" + APP_ID + "&client_secret=" + APP_SECRET;
        String sign = MD5Util.md5(queryStr + "&key=" + new StringBuilder(REFRESH_TOKEN).reverse().toString()).toUpperCase();
        String url = DOMAIN_URL + "/oauth/getAccessToken?" + queryStr + "&sign=" + sign;
        Map<String, String> headers = new HashMap<>(1);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        String resultStr = HttpRequestUtil.post(url, headers, new HashMap<>(0));
        if (StringUtils.isNotEmpty(resultStr)) {
            JSONObject result =JSONUtil.parseObj(resultStr);
            String responseCode = result.getStr("responseCode");
            String responseMessage = result.getStr("responseMessage");
            if ("00000".equals(responseCode)) {
                JSONObject body = result.getJSONObject("body");
                HashMap<String, String> map = new HashMap<>();
                map.put("accessToken", body.getStr("access_token"));
                map.put("refreshToken", body.getStr("refresh_token"));
                map.put("scope", body.getStr("scope"));
                map.put("expiresIn", body.getStr("expires_in"));
                map.put("clientId", body.getStr("client_id"));
                return AjaxResultUtil.success(responseMessage, map);
            }
            return AjaxResultUtil.fail(responseCode, responseMessage);
        } else {
            return AjaxResultUtil.fail("9999", "请求异常");
        }
    }

    /**
     * 刷新accessToken
     */
    private AjaxResult<HashMap<String, String>> refreshAccessToken() {
        //init();
        String queryStr = "client_id=" + APP_ID + "&client_secret=" + APP_SECRET + "&refresh_token=" + REFRESH_TOKEN;
        String sign = MD5Util.md5(queryStr + "&key=" + new StringBuilder(REFRESH_TOKEN).reverse().toString()).toUpperCase();
        String url = DOMAIN_URL + "/oauth/refreshAccessToken?" + queryStr + "&sign=" + sign;
        Map<String, String> headers = new HashMap<>(1);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        String resultStr = HttpRequestUtil.post( url, headers, new HashMap<>(0));
        if (StringUtils.isNotEmpty(resultStr)) {
            JSONObject result = JSONUtil.parseObj(resultStr);
            String responseCode = result.getStr("responseCode");
            String responseMessage = result.getStr("responseMessage");
            if ("00000".equals(responseCode)) {
                JSONObject body = result.getJSONObject("body");
                HashMap<String, String> map = new HashMap<>();
                map.put("accessToken", body.getStr("access_token"));
                map.put("refreshToken", body.getStr("refresh_token"));
                map.put("scope", body.getStr("scope"));
                map.put("expiresIn", body.getStr("expires_in"));
                map.put("clientId", body.getStr("client_id"));
                return AjaxResultUtil.success(responseMessage, map);
            }
            return AjaxResultUtil.fail(responseCode, responseMessage);
        } else {
            return AjaxResultUtil.fail("9999", "请求异常");
        }
    }


    /**
     * 获取参数的字典排序
     *
     * @param map 参数key-value map集合
     * @return String 排序后的字符串
     */
    private static String getOrderByLexicographic(Map<String, Object> map) throws Exception {
        map = new TreeMap<String, Object>(map);//map =  Maps.newTreeMap();
        // 拼接成字符串
        String join = Joiner.on('&').useForNull("").withKeyValueSeparator("=").join(map) + "&";
        // sha256 哈希，并转16进制（大写）
        join = Hex.encodeHexString(DigitalCertificateEncryptUtils.getSha256Data(join)).toUpperCase();
        return join;
    }

}