package com.ytdinfo.inndoo.config.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Exrickx
 */
@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    @Autowired
    private LimitRaterInterceptor limitRaterInterceptor;

    @Autowired
    private APITokenInterceptor apiTokenInterceptor;

    @Autowired
    private IgnoredUrlsProperties ignoredUrlsProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 注册拦截器
        InterceptorRegistration ir = registry.addInterceptor(limitRaterInterceptor);
        // 配置拦截的路径
        ir.addPathPatterns("/**");
        // 配置不拦截的路径
        ir.excludePathPatterns(ignoredUrlsProperties.getUrls());
        ir.order(1);

        InterceptorRegistration irApiTokenInterceptor = registry.addInterceptor(apiTokenInterceptor);
        // 配置拦截的路径
        irApiTokenInterceptor.addPathPatterns("/**");
        irApiTokenInterceptor.excludePathPatterns(ignoredUrlsProperties.getUrls());
        irApiTokenInterceptor.order(2);
    }
}
