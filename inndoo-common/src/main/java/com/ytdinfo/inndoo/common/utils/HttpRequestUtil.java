package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.inndoo.common.constant.SettingConstant;
import com.ytdinfo.inndoo.modules.base.vo.ProxySetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author zhuzheng
 */
@Slf4j
public class HttpRequestUtil {

    public enum RequestMethod {
        GET, POST
    }

    private static final String CHARSET = "UTF-8";

    private static final CloseableHttpClient httpClient = HttpClientUtil.getHttpClient();

    public static String get(String requestUrl) {
        return doRequestBySecretKey(Method.GET, requestUrl, null, null, null);
    }

    public static String get(String requestUrl, Map<String, Object> paramMap) {
        return doRequestBySecretKey(Method.GET, requestUrl, null, paramMap, null);
    }

    public static String get(String requestUrl, Map<String, String> headerMap, Map<String, Object> paramMap) {
        return doRequestBySecretKey(Method.GET, requestUrl, headerMap, paramMap, null);
    }

    public static String post(String requestUrl, Map<String, Object> paramMap) {
        return doRequestBySecretKey(Method.POST, requestUrl, null, paramMap, null);
    }

    public static String post(String requestUrl, Map<String, String> headerMap,
                              Map<String, Object> paramMap) {
        return doRequestBySecretKey(Method.POST, requestUrl, headerMap, paramMap, null);
    }

    public static String post(String requestUrl, String body) {
        if (StringUtils.isEmpty(requestUrl)) {
            return null;
        }
        try {
            HttpPost httpPost = new HttpPost(requestUrl);
            String contentType = HttpUtil.getContentTypeByRequestBody(body);
            Map<String,String> headerMap = new HashMap();
            contentType = ContentType.build(contentType, CharsetUtil.charset(CHARSET));
            headerMap.put(Header.CONTENT_TYPE.toString(), contentType);
            addHeader(httpPost,headerMap);
            httpPost.setEntity(new StringEntity(body, CHARSET));
            RequestConfig.Builder configBuilder = RequestConfig.custom()
                    .setSocketTimeout(5000)
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000);
            Map<String,Object> proxyMap = getProxyMap(requestUrl);
            if(proxyMap != null){
                String host = proxyMap.get("host").toString();
                Integer port =  (Integer) proxyMap.get("port");
                configBuilder.setProxy(new HttpHost(host,port) );
            }
            RequestConfig config =configBuilder.build();
            httpPost.setConfig(config);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
                    HttpEntity entity = httpResponse.getEntity();
                    String content = EntityUtils.toString(entity, CHARSET);
                    EntityUtils.consume(entity);
                    return content;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                httpPost.abort();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String post(String requestUrl, Map<String, String> headerMap, String body) {
        if (StringUtils.isEmpty(requestUrl)) {
            return null;
        }
        try {
            HttpPost httpPost = new HttpPost(requestUrl);
            String contentType = HttpUtil.getContentTypeByRequestBody(body);
            contentType = ContentType.build(contentType, CharsetUtil.charset(CHARSET));
            headerMap.put(Header.CONTENT_TYPE.toString(), contentType);
            addHeader(httpPost, headerMap);
            httpPost.setEntity(new StringEntity(body, CHARSET));
            RequestConfig.Builder configBuilder = RequestConfig.custom()
                    .setSocketTimeout(5000)
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000);
            Map<String,Object> proxyMap = getProxyMap(requestUrl);
            if(proxyMap != null){
                String host = proxyMap.get("host").toString();
                Integer port =  (Integer) proxyMap.get("port");
                configBuilder.setProxy(new HttpHost(host,port) );
            }
            RequestConfig config =configBuilder.build();
            httpPost.setConfig(config);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
                    HttpEntity entity = httpResponse.getEntity();
                    String content = EntityUtils.toString(entity, CHARSET);
                    EntityUtils.consume(entity);
                    return content;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                httpPost.abort();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param method
     * @param requestUrl
     * @param headerMap
     * @param paramMap
     * @param secretKey
     * @return httpEntityString
     */
    public static String doRequestBySecretKey(Method method, String requestUrl,
                                              Map<String, String> headerMap, Map<String, Object> paramMap,
                                              String secretKey) {
        HttpResponse httpResponse = doRequestWithSecretKey(method,
                requestUrl,
                headerMap,
                paramMap,
                secretKey);
        if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            try {
                return EntityUtils.toString(httpResponse.getEntity(), CHARSET);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param method
     * @param requestUrl
     * @param headerMap
     * @param paramMap
     * @param secretKey
     * @return httpResponse
     */
    public static HttpResponse doRequestWithSecretKey(Method method, String requestUrl,
                                                      Map<String, String> headerMap,
                                                      Map<String, Object> paramMap,
                                                      String secretKey) {
        if (method == null || StringUtils.isEmpty(requestUrl)) {
            return null;
        }
        HttpRequestBase requestlog = null;
        try {
            HttpRequestBase request;
            if (method.equals(Method.GET)) {
                request = httpGet(requestUrl, headerMap, paramMap);
            } else {
                request = httpPost(requestUrl, headerMap, paramMap, secretKey);
            }
            RequestConfig.Builder configBuilder = RequestConfig.custom()
                    .setSocketTimeout(5000)
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000);

            StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
            String v = redisTemplate.opsForValue().get(SettingConstant.PROXY_SETTING);
            ProxySetting setting = new Gson().fromJson(v, ProxySetting.class);
            if (null != setting) {
                String hosts = setting.getHosts();
                String[] hostsArr = hosts.split(",");
                if (hostsArr.length > 0) {
                    String host = request.getURI().getHost();
                    for (int k = 0; k < hostsArr.length; k++) {
                        if (hostsArr[k].equals(host)) {
                            configBuilder.setProxy(new HttpHost(setting.getIp(), setting.getPort()));
                            break;
                        }
                    }
                }
            }

            String set = XxlConfClient.get("inndoo-sso.proxy.set");
            String host = XxlConfClient.get("inndoo-sso.proxy.host");
            String port = XxlConfClient.get("inndoo-sso.proxy.port");
            String nonHosts = XxlConfClient.get("inndoo-sso.proxy.nonhosts");
            boolean igoneProxy = true;
            if(StrUtil.isBlank(nonHosts)){
                nonHosts = "localhost";
            }
            String theUrl = requestUrl;
            if(requestUrl.startsWith("http://")){
                theUrl = theUrl.replace("http://","");
            }
            if(requestUrl.startsWith("https://")){
                theUrl = theUrl.replace("https://","");
            }
            List<String> nonHostList =  Arrays.asList(nonHosts.split("\\|") );
            for(String temp : nonHostList ){
                if(theUrl.startsWith(temp)){
                    igoneProxy = false;
                    break;
                }
            }
            if("true".equals(set) && igoneProxy){
                configBuilder.setProxy(new HttpHost(host, Integer.valueOf(port)));
            }
            RequestConfig config =configBuilder.build();
            request.setConfig(config);
            requestlog = request;
            CloseableHttpResponse httpResponse = httpClient.execute(request);
            if(httpResponse == null || httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                request.abort();
            }
            return httpResponse;
        } catch (UnsupportedEncodingException e) {
            log.error(JSONUtil.toJsonStr(requestlog));
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            log.error(JSONUtil.toJsonStr(requestlog));
            e.printStackTrace();
        } catch (IOException e) {
            log.error(JSONUtil.toJsonStr(requestlog));
            e.printStackTrace();
        }
        return null;
    }

    private static HttpPost httpPost(String requestUrl, Map<String, String> headerMap, Map<String, Object> paramMap, String secretKey) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(requestUrl);
        String contentType = null;
        if (headerMap != null) {
            Iterator<Map.Entry<String, String>> iterator = headerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                httpPost.addHeader(entry.getKey(), entry.getValue());
                if(entry.getKey().equalsIgnoreCase("Content-Type")){
                    contentType = entry.getValue();
                }
            }
        }
        if (paramMap != null) {
            if (StringUtils.isEmpty(secretKey)) {
                if(StringUtils.isEmpty(contentType)){
                    List<NameValuePair> formParams = new ArrayList<>();
                    Iterator<Map.Entry<String, Object>> iterator = paramMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Object> entry = iterator.next();
                        formParams.add(new BasicNameValuePair(entry.getKey(),
                                entry.getValue() == null ? null : entry.getValue().toString()));
                    }
                    httpPost.setEntity(new UrlEncodedFormEntity(formParams, CHARSET));
                }else {
                    String[] contentTypes = contentType.split(";");
                    for (int i = 0; i < contentTypes.length; i++) {
                        if(contentTypes[i].equalsIgnoreCase("application/json")){
                            GsonBuilder gsonBuilder = new GsonBuilder();
                            String json = gsonBuilder.create().toJson(paramMap);
                            httpPost.setEntity(new StringEntity(json, CHARSET));
                            break;
                        }
                    }
                }
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                String json = gsonBuilder.create().toJson(paramMap);
                httpPost.setEntity(new StringEntity(AESUtil.encrypt(json, secretKey), CHARSET));
            }
        }
        return httpPost;
    }

    private static HttpGet httpGet(String requestUrl, Map<String, String> headerMap, Map<String, Object> paramMap) {
        HttpGet httpGet = new HttpGet(concatUrlByParams(requestUrl, paramMap));
        addHeader(httpGet, headerMap);
        return httpGet;
    }

    private static void addHeader(HttpRequestBase httpRequestBase, Map<String, String> headerMap) {
        if (headerMap != null && !headerMap.isEmpty()) {
            Iterator<Map.Entry<String, String>> iterator = headerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                httpRequestBase.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * @param url
     * @param paramMap
     * @return url
     */
    private static <K, V> String concatUrlByParams(String url, Map<K, V> paramMap) {
        if (paramMap == null || paramMap.isEmpty()) {
            return url;
        }

        StringBuilder buffer = new StringBuilder(url);
        if (url.contains("?")) {
            buffer.append("&");
        } else {
            buffer.append("?");
        }

        Iterator<Map.Entry<K, V>> it = paramMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            buffer.append(entry.getKey()).append('=').append(entry.getValue());
            if (it.hasNext()) {
                buffer.append("&");
            }
        }
        return buffer.toString();
    }

    public static Map<String,Object> getProxyMap(String requestUrl){
        String set = XxlConfClient.get("inndoo-sso.proxy.set");
        String host = XxlConfClient.get("inndoo-sso.proxy.host");
        String port = XxlConfClient.get("inndoo-sso.proxy.port");
        String nonHosts = XxlConfClient.get("inndoo-sso.proxy.nonhosts");
        boolean igoneProxy = true;
        if(StrUtil.isBlank(nonHosts)){
            nonHosts = "localhost";
        }
        String theUrl = requestUrl;
        if(requestUrl.startsWith("http://")){
            theUrl = theUrl.replace("http://","");
        }
        if(requestUrl.startsWith("https://")){
            theUrl = theUrl.replace("https://","");
        }
        List<String> nonHostList =  Arrays.asList(nonHosts.split("\\|") );
        for(String temp : nonHostList ){
            if(theUrl.startsWith(temp)){
                igoneProxy = false;
                break;
            }
        }
        if("true".equals(set) && igoneProxy){
            Map<String,Object> map = new HashMap<>();
            map.put("host",host);
            map.put("port", Integer.valueOf(port));
            return map;
        }
        return null;
    }

}
