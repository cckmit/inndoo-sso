package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.StrUtil;

import cn.hutool.json.JSONUtil;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.ytdinfo.inndoo.common.vo.LiaoNingResult;
import com.ytdinfo.inndoo.common.vo.Result;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author yaochangning
 * 辽宁接口访问封装
 */
@Component
public class LiaoNingUrlUtil {

    private static String LN_BIAMS_TOKEN = "ln:biams:token";
//    @Autowired
//    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

//    //辽宁行内接口
//    public  String getLnToken(String url) throws Exception {
//        String redisKey = LN_BIAMS_TOKEN;
//        String token = redisTemplate.opsForValue().get(redisKey);
//        if (StrUtil.isEmpty(token)) {
////            String url = "http://ln-biams.bank-of-china.com:8060/openapi/commonapi/v2/accessToken";
//            HashMap<String, String> mapdata = Maps.newHashMap();
//            mapdata.put("user_key", "admin@admin.cn");
//            mapdata.put("priv_id", "2");
//            mapdata.put("expires_in", "1000");
//            mapdata.put("sign", "ad0960d5735058ae4e01242544852a0c");
//            String result = getContent(url, mapdata, false);
//            if (StrUtil.isEmpty(result)) {
//                throw new Exception("获取token数据失败！" + url + "==" + mapdata);
//            }
//            LiaoNingResult liaoNingResult = JSONUtil.toBean(result, LiaoNingResult.class);
//            if ( liaoNingResult.getSuccess() && liaoNingResult.getLogin()) {
//                Map<String, Object> data = liaoNingResult.getData().get(0);
//                token = data.get("token").toString();
//                String userId = data.get("userId").toString();
//                if (StrUtil.isNotEmpty(token) && StrUtil.isNotEmpty(userId)) {
//                    redisTemplate.delete(redisKey);
//                    redisTemplate.opsForValue().set(redisKey, token, 1000);
//                }
//            }
//            if (StrUtil.isEmpty(token)) {
//                throw new Exception("获取token数据失败！" + url + "==" + mapdata + "==" + result);
//            }
//
//        }
//
//        return token;
//
//    }

    /**
     * @param url
     * @param mapdata
     * @param flag    true 获取token
     * @return
     * @throws Exception
     */
    public String getContent(String url, Map<String, String> mapdata, Boolean flag) throws Exception {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建httppost
        HttpPost httpPost = new HttpPost(url);
        try {
            // 设置提交方式
            httpPost.addHeader("Content-type", "application/x-www-form-urlencoded");
            if (flag) {
                // 获取token
//                String tokenuserId = getToken(mapdata.get("staffNo"));
                Map<String, Object> data;
                String s = redisTemplate.opsForValue().get(LN_BIAMS_TOKEN);
                if (StrUtil.isEmpty(s)) {
                    //data = getToken("https://openapi-data.ytdinfo.cn/openapi/commonapi/v2/accessToken");
                    //data = getToken("http://ln-biams.bank-of-china.com:8060/openapi/commonapi/v2/accessToken");
                    data = getToken("https://cloud.bankofchina.com/ln/wdqys/openapi/commonapi/v2/accessToken");
                    redisTemplate.opsForValue().set(LN_BIAMS_TOKEN, JSONUtil.toJsonStr(data));
                    redisTemplate.expire(LN_BIAMS_TOKEN, 1000, TimeUnit.SECONDS);
                } else {
                    data = JSONUtil.toBean(s, Map.class);
                }
                httpPost.setHeader("ytdtoken", data.get("token").toString());
                httpPost.setHeader("ytdUserId", data.get("userId").toString());
            }

            // 添加参数
            String str = JSONUtil.toJsonStr(mapdata);
            //method.addParameter("params", str);
            List nameValuePairs = new ArrayList();
            if (!flag) {
                if (mapdata != null && mapdata.size() != 0) {
                    // 将mapdata中的key存在set集合中，通过迭代器取出所有的key，再获取每一个键对应的值
                    for (Map.Entry<String, String> entry : mapdata.entrySet()) {
                        nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                    }
                }
            } else {
                nameValuePairs.add(new BasicNameValuePair("params", str));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            System.out.println("nameValuePairs:" + nameValuePairs);
            // 执行http请求
            response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // 获得http响应体
                HttpEntity entity = response.getEntity();
                System.out.println("entity:" + entity);
                if (entity != null) {
                    // 响应的结果
                    String content = EntityUtils.toString(entity, "UTF-8");
                    return content;
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Map<String, Object> getToken(String url) throws Exception {
        HashMap<String, String> mapdata = Maps.newHashMap();
        mapdata.put("user_key", "admin@admin.cn");
        mapdata.put("priv_id", "2");
        mapdata.put("expires_in", "1000");
        mapdata.put("sign", "ad0960d5735058ae4e01242544852a0c");
        String result = getContent(url, mapdata, false);
        if (StrUtil.isEmpty(result)) {
            throw new Exception("获取token数据失败！" + url + "==" + mapdata);
        }
        LiaoNingResult liaoNingResult = JSONUtil.toBean(result, LiaoNingResult.class);
        if (liaoNingResult.getSuccess() && liaoNingResult.getLogin()) {
            Map<String, Object> data = liaoNingResult.getData().get(0);
            return data;
        }
        return null;
    }
}
