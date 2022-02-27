package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.HttpRequestUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.ShzhActUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.dto.DynamicApiDto;
import com.ytdinfo.inndoo.modules.core.service.DynamicApiBaseService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 获取上海中行[查询活动参与权限_注册时间：yyyyMMdd-HHmmss]
 */
public class ShzhActRegisterTimeServiceImpl implements DynamicApiBaseService<String> {
    private static final String shzhActPermUrl ="https://cloud.bankofchina.com/sh/apis-zsc/activity/permission";
    //私钥
    private static String priKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQC7VJTUt9Us8cKjMzEfYyjiWA4R4/M2bS1GB4t7NXp98C3SC6dVMvDuictGeurT8jNbvJZHtCSuYEvuNMoSfm76oqFvAp8Gy0iz5sxjZmSnXyCdPEovGhLa0VzMaQ8s+CLOyS56YyCFGeJZqgtzJ6GR3eqoYSW9b9UMvkBpZODSctWSNGj3P7jRFDO5VoTwCQAWbFnOjDfH5Ulgp2PKSQnSJP3AJLQNFNe7br1XbrhV//eO+t51mIpGSDCUv3E0DDFcWDTH9cXDTTlRZVEiR2BwpZOOkE/Z0/BVnhZYL71oZV34bKfWjQIt6V/isSMahdsAASACp4ZTGtwiVuNd9tybAgMBAAECggEBAKTmjaS6tkK8BlPXClTQ2vpz/N6uxDeS35mXpqasqskVlaAidgg/sWqpjXDbXr93otIMLlWsM+X0CqMDgSXKejLS2jx4GDjI1ZTXg++0AMJ8sJ74pWzVDOfmCEQ/7wXs3+cbnXhKriO8Z036q92Qc1+N87SI38nkGa0ABH9CN83HmQqt4fB7UdHzuIRe/me2PGhIq5ZBzj6h3BpoPGzEP+x3l9YmK8t/1cN0pqI+dQwYdgfGjackLu/2qH80MCF7IyQaseZUOJyKrCLtSD/Iixv/hzDEUPfOCjFDgTpzf3cwta8+oE4wHCo1iI1/4TlPkwmXx4qSXtmw4aQPz7IDQvECgYEA8KNThCO2gsC2I9PQDM/8Cw0O983WCDY+oi+7JPiNAJwv5DYBqEZB1QYdj06YD16XlC/HAZMsMku1na2TN0driwenQQWzoev3g2S7gRDoS/FCJSI3jJ+kjgtaA7Qmzlgk1TxODN+G1H91HW7t0l7VnL27IWyYo2qRRK3jzxqUiPUCgYEAx0oQs2reBQGMVZnApD1jeq7n4MvNLcPvt8b/eU9iUv6Y4Mj0Suo/AU8lYZXm8ubbqAlwz2VSVunD2tOplHyMUrtCtObAfVDUAhCndKaA9gApgfb3xw1IKbuQ1u4IF1FJl3VtumfQn//LiH1B3rXhcdyo3/vIttEk48RakUKClU8CgYEAzV7W3COOlDDcQd935DdtKBFRAPRPAlspQUnzMi5eSHMD/ISLDY5IiQHbIH83D4bvXq0X7qQoSBSNP7Dvv3HYuqMhf0DaegrlBuJllFVVq9qPVRnKxt1Il2HgxOBvbhOT+9in1BzA+YJ99UzC85O0Qz06A+CmtHEy4aZ2kj5hHjECgYEAmNS4+A8Fkss8Js1RieK2LniBxMgmYml3pfVLKGnzmng7H2+cwPLhPIzIuwytXywh2bzbsYEfYx3EoEVgMEpPhoarQnYPukrJO4gwE2o5Te6T5mJSZGlQJQj9q4ZB2Dfzet6INsK0oG8XVGXSpQvQh3RUYekCZQkBBFcpqWpbIEsCgYAnM3DQf3FJoSnXaMhrVBIovic5l0xFkEHskAjFTevO86Fsz1C2aSeRKSqGFoOQ0tmJzBEs1R6KqnHInicDTQrKhArgLXX4v3CddjfTRJkFWDbE/CkvKZNOrcf1nhaGCPspRJj2KUkj1Fhl9Cncdn/RsYEONbwQSjIfMPkvxF+8HQ==";
    //公钥
    private static String pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1SU1LfVLPHCozMxH2Mo4lgOEePzNm0tRgeLezV6ffAt0gunVTLw7onLRnrq0/IzW7yWR7QkrmBL7jTKEn5u+qKhbwKfBstIs+bMY2Zkp18gnTxKLxoS2tFczGkPLPgizskuemMghRniWaoLcyehkd3qqGElvW/VDL5AaWTg0nLVkjRo9z+40RQzuVaE8AkAFmxZzow3x+VJYKdjykkJ0iT9wCS0DRTXu269V264Vf/3jvredZiKRkgwlL9xNAwxXFg0x/XFw005UWVRIkdgcKWTjpBP2dPwVZ4WWC+9aGVd+Gyn1o0CLelf4rEjGoXbAAEgAqeGUxrcIlbjXfbcmwIDAQAB";
    private static final String activityId = "471024162934906881";
    @Autowired
    private static ActivityApiUtil activityApiUtil;
    //@Autowired
   //private static ShzhActUtil shzhActUtil;

    @Override
    public Result<String> getValue(DynamicApiDto dto) {
        return dealDifferentType("registerTime",dto.getAccountId());
    }


    /**根据accountId查询活动平台uuid*/
    public String getUuid(String accountId){
        String uuid= "";
//        String url = "http://localhost:5041/activity-api" + ApiConstant.ACTIVITY_SHZH_GETUUID ;
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Content-Type", "application/json;charset=UTF-8");
//        Map<String, String> bodyMap = new HashMap<>();
//        bodyMap.put("accountId", accountId);
//        String bodyJson = JSONUtil.toJsonStr(bodyMap);
//        String resultStr = HttpRequestUtil.post(url, headers, bodyJson);

        String resultStr = activityApiUtil.getShzhUuid(accountId);
        if(StrUtil.isBlank(resultStr)){
            return uuid;
        }
        JSONObject resultJson =JSONUtil.parseObj(resultStr);
        Boolean success = resultJson.getBool("success");
        if(!success){
            return uuid;
        }
        JSONObject body = resultJson.getJSONObject("result");
        uuid = body.getStr("uuid");
        return uuid;
    }

    /**生成token*/
    public String getToken(String uuid,String projectid){
        String token = "";
        //指定jwt签名的时候使用的签名算法，这里指定RS256，即SHA256withRSA算法。
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
        //生成JWT的时间
        Date now = new Date();
        //获取私钥
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(priKey));
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey pri = keyFactory.generatePrivate(keySpec);
            //获取公钥
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pubKey));
            PublicKey pub = keyFactory.generatePublic(pubSpec);
            //签发人,填写平台分配的对应id
            String issuer = "ytd";
            //待签名数据
            String plainText = "POST/activity/permission{\"uuid\":\"" + uuid + "\",\"projectid\":\"" +projectid  + "\"}";
            //生成sha1摘要
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] cipherBytes = md.digest(plainText.getBytes(StandardCharsets.UTF_8));
            String digest = Base64.getEncoder().encodeToString(cipherBytes);
            System.out.println("数字摘要：" + digest);
            //组装JwtBuilder
            JwtBuilder builder = Jwts.builder()
                    //私有声明，这里申明业务约定的数字摘要
                    .claim("digest", digest)
                    //设置jti(JWT ID)：是JWT的唯一标识，根据业务需要，这个可以设置为一个不重复的值，主要用来作为一次性token,从而回避重放攻击。
                    .setId(UUID.randomUUID().toString())
                    //iat: jwt的签发时间
                    .setIssuedAt(now)
                    //jwt的签发人
                    .setIssuer(issuer)
                    //设置签名使用的签名算法和签名使用的秘钥
                    .signWith(signatureAlgorithm, pri);

            token = builder.compact();
            System.out.println("jwt令牌：" + token);
            //验证jwt的token，若验证出错会抛异常
            Claims claims1 = Jwts.parser()
                    //设置解密的公钥
                    .setSigningKey(pub)
                    //设置需要解析的jwt
                    .parseClaimsJws(token).getBody();
//            System.out.println("从令牌中取出的摘要：" + claims1.get("digest"));
//            System.out.println("从令牌中取出的签发人：" + claims1.getIssuer());
//            System.out.println("从令牌中取出的签发时间：" + claims1.getIssuedAt());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  token;
    }

    /**上海中行[查询活动参与权限]接口交互*/
    public String getShzhActPermission(String uuid,  String token){
        String resultStr ="";
        try {
            Map<String, String> headers = new HashMap<>(1);
            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put("token", token);
            Map<String, String> bodyMap = new HashMap<>();
            bodyMap.put("uuid", uuid);
            bodyMap.put("projectid", activityId);
            String bodyJson = JSONUtil.toJsonStr(bodyMap);
            System.out.println("request_headers==>" + JSONUtil.toJsonStr(headers));
            System.out.println("request_body==>" + JSONUtil.toJsonStr(bodyJson));
            resultStr = HttpRequestUtil.post(shzhActPermUrl, headers, bodyJson);
            System.out.println("resultStr==>" + resultStr);
            return resultStr;
        } catch (Exception e) {
            return "";
        }
    }

    /**处理上海中行下一个动态接口*/
    public Result<String> dealDifferentType(String type,String accountId){
        String getUuid = getUuid(accountId);
        if(StrUtil.isBlank(getUuid)){
            return new ResultUtil<String>().setErrorMsg("get UUID Error");
        }
        String getToken = getToken(getUuid,activityId);
        if(StrUtil.isBlank(getToken)){
            return new ResultUtil<String>().setErrorMsg("get Token Error");
        }
        String resultStr = getShzhActPermission(getUuid,getToken);
        if (StrUtil.isEmpty(resultStr)) {
            return new ResultUtil<String>().setErrorMsg("get shzh Act Permission Error");
        }
        JSONObject jsonObject = JSONUtil.parseObj(resultStr);
        String returnStatus = jsonObject.getStr("status");
        if(StrUtil.isNotEmpty(returnStatus)){
            return new ResultUtil<String>().setErrorMsg("get shzh Act Permission  status Error");
        }
        ResultUtil<String> resultData = new ResultUtil<String>();
        if("highFre".equals(type)){
            boolean highFre	= jsonObject.getBool("highFre");//是否为上一年度手机银行交易高频用户
            if(highFre){
                resultData.setData("Y");
            }
        }else if("register".equals(type)){
            boolean register	 = jsonObject.getBool("register");//是否注册
            if(register){
                resultData.setData("Y");
            }
        }else if("sh".equals(type)){
            boolean sh	= jsonObject.getBool("sh");//是否为上海中行用户
            if(sh){
                resultData.setData("Y");
            }
        }else if("shMob".equals(type)){
            boolean shMob = jsonObject.getBool("shMob");//是否为上海中行手机银行用户
            if(shMob){
                resultData.setData("Y");
            }
        }else if("registerTime".equals(type)){
            String registerTime	= 	jsonObject.getStr("registerTime");//注册时间：yyyyMMdd-HHmmss
            resultData.setData(registerTime);
        }
        return new ResultUtil<String >().setData("N");
    }

}
