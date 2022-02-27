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
    private ClearContextInterceptor clearContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration irClearContextInterceptor = registry.addInterceptor(clearContextInterceptor);
        // 配置拦截的路径
        irClearContextInterceptor.addPathPatterns("/**");
        irClearContextInterceptor.order(0);
    }
}
