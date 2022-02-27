package com.ytdinfo.inndoo.common.utils;

import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.SettingConstant;
import com.ytdinfo.inndoo.core.util.HttpClientUtil;
import com.ytdinfo.inndoo.modules.base.entity.Settings;
import com.ytdinfo.inndoo.modules.base.vo.SmsSetting;
import com.ytdinfo.inndoo.common.exception.InndooException;
import cn.hutool.core.util.StrUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.google.gson.Gson;
import com.ytdinfo.inndoo.modules.core.service.SettingsService;
import com.ytdinfo.model.request.SendSMSRequest;
import com.ytdinfo.model.response.SendSMSResponse;
import com.ytdinfo.util.APIRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Exrickx
 */
@Component
@Slf4j
public class SmsUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SettingUtil settingUtil;

    @XxlConf("wxapi.aeskey")
    private String aeskey;
    @XxlConf("wxapi.rooturl")
    private String apirooturl;
    @XxlConf("wxapi.appkey.sms")
    private String appkey;

    public SmsSetting getSmsSetting(){

        String v = redisTemplate.opsForValue().get(SettingConstant.ALI_SMS);
        if(StrUtil.isBlank(v)){
            throw new InndooException("您还未配置阿里云短信");
        }
        return new Gson().fromJson(v, SmsSetting.class);
    }

    /**
     * 发送验证码 模版变量为 code
     * @param mobile
     * @param code
     * @param templateCode
     * @return
     * @throws ClientException
     */
    public SendSMSResponse sendCode(String mobile, String code, String templateCode) throws Exception {

        return sendSms(mobile,code, templateCode);
    }

    /**
     * 发送工作流消息 模版变量为 content
     * @param mobile
     * @param content
     * @return
     * @throws ClientException
     */
    public SendSMSResponse sendActMessage(String mobile, String content) throws Exception {

        // 获取工作流消息模板
        String templateCode = redisTemplate.opsForValue().get(SettingConstant.ALI_SMS_ACTIVITI);
        if(StrUtil.isBlank(templateCode)){
            templateCode = settingUtil.getSettingValue(SettingConstant.ALI_SMS_ACTIVITI);
        }
        return sendSms(mobile,content, templateCode);
    }

//    /**
//     * 发送短信
//     * @param mobile 手机号
//     * @param param 替换短信模板 变量
//     * @param value 变量值
//     * @param templateCode 短信模板code
//     * @return
//     * @throws ClientException
//     */
//    public SendSmsResponse sendSms(String mobile, String param, String value, String templateCode) throws ClientException {
//
//        SmsSetting s = getSmsSetting();
//
//        //设置超时时间-可自行调整
//        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
//        System.setProperty("sun.net.client.defaultReadTimeout", "10000");
//        final String product = "Dysmsapi";
//        final String domain = "dysmsapi.aliyuncs.com";
//        //初始化ascClient,暂时不支持多region（请勿修改）
//        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", s.getAccessKey(),
//                s.getSecretKey());
//        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
//        IAcsClient acsClient = new DefaultAcsClient(profile);
//        //组装请求对象
//        SendSmsRequest request = new SendSmsRequest();
//        //使用post提交
//        request.setMethod(MethodType.POST);
//        //必填:待发送手机号。支持以逗号分隔的形式进行批量调用，批量上限为1000个手机号码,批量调用相对于单条调用及时性稍有延迟,验证码类型的短信推荐使用单条调用的方式；发送国际/港澳台消息时，接收号码格式为00+国际区号+号码，如“0085200000000”
//        request.setPhoneNumbers(mobile);
//        //必填:短信签名-可在短信控制台中找到
//        request.setSignName(s.getSignName());
//        //必填:短信模板-可在短信控制台中找到，发送国际/港澳台消息时，请使用国际/港澳台短信模版
//        request.setTemplateCode(templateCode);
//        //可选:模板中的变量替换JSON串,如模板内容为"您的验证码为${code}"时,此处的值为
//        //友情提示:如果JSON中需要带换行符,请参照标准的JSON协议对换行符的要求,比如短信内容中包含\r\n的情况在JSON中需要表示成\\r\\n,否则会导致JSON在服务端解析失败
//        request.setTemplateParam("{\""+ param +"\":\""+ value +"\"}");
//        //请求失败这里会抛ClientException异常
//        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
//
//        return sendSmsResponse;
//    }

    public SendSMSResponse sendSms(String mobile, String value, String templateCode) throws Exception {

        CloseableHttpClient httpClient = HttpClientUtil.get().getHttpClient();
        APIRequest<SendSMSRequest, SendSMSResponse> apiRequest = new APIRequest<SendSMSRequest, SendSMSResponse>();
        SendSMSRequest request = new SendSMSRequest();
        request.setUrl(apirooturl);
        request.setAesKey(aeskey);
        request.setAppkey(appkey);
        request.setPhone(mobile);
        request.setCode(value);
        request.setTplId(templateCode);
        request.setType(SendSMSRequest.EnumSMSType.text.toString());
        String proxySet = XxlConfClient.get("inndoo-sso.proxy.set");
        SendSMSResponse response;
        if("true".equals(proxySet)){
            String proxyHost = XxlConfClient.get("inndoo-sso.proxy.host");
            String proxyPort = XxlConfClient.get("inndoo-sso.proxy.port");
            response = apiRequest.request(request, SendSMSResponse.class, httpClient,proxyHost,Integer.parseInt(proxyPort));
        }else{
            response = apiRequest.request(request, SendSMSResponse.class, httpClient);
        }
        return response;
    }
}
