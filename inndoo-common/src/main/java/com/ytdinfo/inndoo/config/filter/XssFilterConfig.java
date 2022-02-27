package com.ytdinfo.inndoo.config.filter;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.DispatcherType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author timmy
 * @date 2020/4/23
 */
@Configuration
public class XssFilterConfig {

    @XxlConf("activity.xss.enabled")
    private String enabled;

    @XxlConf("activity.xss.excludes")
    private String excludes;

    @XxlConf("activity.xss.urlPatterns")
    private String urlPatterns;

    @Bean
    public FilterRegistrationBean xssFilterRegistration(){
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        registrationBean.setFilter(new XssFilter());
        registrationBean.addUrlPatterns(StrUtil.split(urlPatterns, ","));
        registrationBean.setName("XssFilter");
        registrationBean.setOrder(9999);
        Map<String,String> initParameters = new HashMap<>();
        initParameters.put("excludes",StrUtil.isEmpty(excludes)?StrUtil.EMPTY:excludes);
        initParameters.put("enabled",StrUtil.isEmpty(enabled)?StrUtil.EMPTY:enabled);
        registrationBean.setInitParameters(initParameters);
        return registrationBean;
    }
}