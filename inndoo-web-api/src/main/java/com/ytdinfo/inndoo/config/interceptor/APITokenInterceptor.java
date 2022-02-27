package com.ytdinfo.inndoo.config.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.extra.servlet.ServletUtil;
import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.modules.core.entity.ApiAccount;
import com.ytdinfo.inndoo.modules.core.service.ApiAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * token校验拦截器
 *
 * @author Exrickx
 */
@Slf4j
@Component
public class APITokenInterceptor extends HandlerInterceptorAdapter {


    @Autowired
    private ApiAccountService apiAccountService;

    /**
     * 预处理回调方法，实现处理器的预处理（如登录检查）
     * 第三个参数为响应的处理器，即controller
     * 返回true，表示继续流程，调用下一个拦截器或者处理器
     * 返回false，表示流程中断，通过response产生响应
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        try {
            if(handler instanceof HandlerMethod){
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                Method method = handlerMethod.getMethod();
                APIModifier apiModifier = method.getAnnotation(APIModifier.class);
                if(apiModifier == null){
                    apiModifier = method.getDeclaringClass().getAnnotation(APIModifier.class);
                }
                if (apiModifier == null || apiModifier.value() == null) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println("There is no APIModifier Annotation on api controller.");
                    return false;
                }
                if (APIModifierType.PRIVATE.equals(apiModifier.value())) {
                    //验证签名
                    boolean pass = validateSign(request);
                    if (!pass) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().println("{\"success\":false,\"code\":401, \"message\":\"签名认证失败\"}");
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 一个简单的签名认证，规则：
     * 1. 将请求参数按ascii码排序
     * 2. 拼接为a=value&b=value...这样的字符串（不包含sign）
     * 3. 混合密钥（secret）进行md5获得签名，与请求的签名进行比较
     */
    private boolean validateSign(HttpServletRequest request) {
        String requestSign = request.getParameter("sign");
        String requestAppkey = request.getParameter("appkey");
        if (StrUtil.isEmpty(requestSign) || StrUtil.isEmpty(requestAppkey)) {
            return false;
        }
        if(StrUtil.isNotEmpty(request.getParameter("appsecret"))){
            return false;
        }
        String appkey = XxlConfClient.get("core.api.appkey");
        String appSecret = XxlConfClient.get("core.api.appsecret");

        String appkey4Data = XxlConfClient.get("core.data.appkey");
        String appSecret4Data = XxlConfClient.get("core.data.appsecret");
        String secret = "";
        if (appkey.equals(requestAppkey)) {
            //微信认证
            secret = appSecret;
        } else if(StrUtil.isNotEmpty(appkey4Data) && appkey4Data.equals(requestAppkey)){
            secret = appSecret4Data;
        } else {
            ApiAccount apiAccount = apiAccountService.findByAppkey(requestAppkey);
            secret = apiAccount.getAppSecret();
            if (apiAccount == null) {
                return false;
            }
        }
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        paramMap.remove("sign");
        //混合密钥md5
        paramMap.put("appsecret", secret);
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        //比较
        return StringUtils.equals(sign, requestSign);
    }

    /**
     * 当前请求进行处理之后，也就是Controller方法调用之后执行，
     * 但是它会在DispatcherServlet 进行视图返回渲染之前被调用。
     * 此时我们可以通过modelAndView对模型数据进行处理或对视图进行处理。
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {

    }

    /**
     * 方法将在整个请求结束之后，也就是在DispatcherServlet渲染了对应的视图之后执行。
     * 这个方法的主要作用是用于进行资源清理工作的。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        UserContext.remove();
    }

}
