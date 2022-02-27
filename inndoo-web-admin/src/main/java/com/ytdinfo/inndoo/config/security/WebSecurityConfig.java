package com.ytdinfo.inndoo.config.security;

import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceFilter;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.config.security.jwt.AuthenticationFailHandler;
import com.ytdinfo.inndoo.config.security.jwt.AuthenticationSuccessHandler;
import com.ytdinfo.inndoo.config.security.jwt.RestAccessDeniedHandler;
import com.ytdinfo.inndoo.core.filter.XxlSsoTokenFilter;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Security 核心配置类
 * 开启注解控制权限至Controller
 * @author Exrickx
 */
@Slf4j
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @XxlConf("core.inndoo.token.redis")
    private Boolean tokenRedis;

    @XxlConf("core.inndoo.tokenexpiretime")
    private Integer tokenExpireTime;

    @XxlConf("core.inndoo.token.storeperms")
    private Boolean storePerms;

    @Autowired
    private IgnoredUrlsProperties ignoredUrlsProperties;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthenticationSuccessHandler successHandler;

    @Autowired
    private AuthenticationFailHandler failHandler;

//    @Autowired
//    private RestAccessDeniedHandler accessDeniedHandler;
//
//    @Autowired
//    private MyFilterSecurityInterceptor myFilterSecurityInterceptor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private XxlSsoTokenFilter xxlSsoTokenFilter;

    @Autowired
    private UserService userService;

    @Autowired
    private MatrixApiUtil apiUtil;

    @Autowired
    private DynamicDataSourceFilter dynamicDataSourceFilter;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        for(String url:ignoredUrlsProperties.getUrls()){
            web.ignoring().antMatchers(url);
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 开启允许iframe 嵌套
        http.headers().frameOptions().disable();

        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry = http
                .authorizeRequests();
        registry.and()
                //表单登录方式
                .formLogin()
                .permitAll()
                //成功处理类
                .successHandler(successHandler)
                //失败
                .failureHandler(failHandler)
                .and()
                //允许网页iframe
                .headers().frameOptions().disable()
                .and()
                .logout()
                .permitAll()
                .and()
                .authorizeRequests()
                //任何请求
                .anyRequest()
                .permitAll()
                .and().csrf().disable();
        // 开启允许iframe 嵌套
//        http.headers().frameOptions().disable();
//
//        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry = http
//                .authorizeRequests();
//
//        //除配置文件忽略路径其它所有请求都需经过认证和授权
//        for(String url:ignoredUrlsProperties.getUrls()){
//            registry.antMatchers(url).permitAll();
//        }
//        registry.and()
//                //表单登录方式
//                .formLogin()
//                .loginPage("/base/common/needLogin")
//                //登录请求url
//                .loginProcessingUrl("/base/login")
//                .permitAll()
//                //成功处理类
//                .successHandler(successHandler)
//                //失败
//                .failureHandler(failHandler)
//                .and()
//                //允许网页iframe
//                .headers().frameOptions().disable()
//                .and()
//                .logout()
//                .permitAll()
//                .and()
//                .authorizeRequests()
//                //任何请求
//                .anyRequest()
//                //需要身份认证
//                .authenticated()
//                .and()
//                //允许跨域
//                .cors()
//                .and()
//                //关闭跨站请求防护
//                .csrf().disable()
//                //前后端分离采用JWT 不需要session
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
//                //自定义权限拒绝处理类
//                .exceptionHandling().accessDeniedHandler(accessDeniedHandler)
//                .and()
//                //添加自定义权限过滤器
//                .addFilterBefore(myFilterSecurityInterceptor, FilterSecurityInterceptor.class)
//                .addFilterBefore(xxlSsoTokenFilter, ChannelProcessingFilter.class)
//                .addFilterBefore(dynamicDataSourceFilter, XxlSsoTokenFilter.class)
//                //添加JWT过滤器
//                .addFilter(new JWTAuthenticationFilter(authenticationManager(), tokenRedis, tokenExpireTime, storePerms,
//                        redisTemplate, securityUtil, userService, apiUtil));
    }
}
