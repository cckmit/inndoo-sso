package com.ytdinfo.inndoo.common.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.vo.ApiCheckResult;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @Author fw
 * 调微应用接口
 */
@Component
public class MicroApplicationUtil {


    /**
     * @param rootUrl
     *
     * @return
     * @throws Exception
     */
    public String checkIdent(String rootUrl, Map<String, String> headers, String phone,String secretKey){

        String url = rootUrl + "coreApi/queryRegister";
        Map<String,Object> body = generateSignMap(phone, secretKey, null);
//        String str = HttpUtil.toParams(body);
        String content = HttpRequestUtil.post(url, body);
        try{
            ApiCheckResult result = JSONUtil.toBean(content, ApiCheckResult.class);
            if ("000000".equals(result.getErr_code())) {
                return result.getErr_msg();
            }
            return "N";
        }catch (Exception e){
            return "N";
        }
    }


    private String generateSign(String phone,String secret ,String iv) {
        HashMap<String, String> data = new HashMap<>();
        data.put("phone", phone);
        String body = HttpUtil.toParams(data);
        String encrypt = AESUtil.encrypt(body, secret);
        return encrypt;
    }

    private Map generateSignMap(String phone,String secret ,String iv) {
        HashMap<String, String> data = new HashMap<>();
        data.put("phone", AESUtil.encrypt(phone, secret));
        return data;
    }



}
