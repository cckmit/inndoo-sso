package com.ytdinfo.inndoo.config.xxlsso;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.core.filter.ActSsoFilter;
import com.ytdinfo.core.login.SsoTokenLoginHelper;
import com.ytdinfo.core.security.SsoManager;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.config.security.ActFilter;
import com.ytdinfo.inndoo.config.security.IgnoredUrlsProperties;
import com.ytdinfo.inndoo.core.filter.XxlSsoTokenFilter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author xuxueli 2018-04-03 20:41:07
 */
@Configuration
public class XxlSsoConfig implements InitializingBean, DisposableBean {

//    @XxlConf("inndoo-sso.xxl.sso.server")
//    private String xxlSsoServer;
//
//    @XxlConf("inndoo-sso.xxl.sso.redis.expire.minite")
//    private int redisExpireMinite;
//
//    @XxlConf("core.server.appcode")
//    private String appCode;
//
    @XxlConf("ytd-sso.sso.server.url")
    private String xxlSsoServer;

    @XxlConf("ytd-sso.sso.server.logout.url")
    private  String logoutUrl;

    @XxlConf("activity.admin.rooturl")
    private String actSsoServer;

    @XxlConf("activity.adminfront.rooturl")
    private  String actLogoutUrl;

    @XxlConf("ytd-sso.client.expire.seconds")
    private int ssoExpireSecond;


    @XxlConf("activity.actsso.client.expire.seconds")
    private int actSsoExpireSecond;

    @XxlConf("core.server.appcode")
    String appCode;

    @Autowired
    private XxlSsoIgnorePaths xxlSsoIgnorePaths;

    @Autowired
    private IgnoredUrlsProperties ignoredUrlsProperties;

    @Autowired
    private MatrixApiUtil matrixApiUtil;

//    @Bean
//    public FilterRegistrationBean xxlSsoFilterRegistration() {
//
//        // xxl-sso, filter init
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//
//        registration.setName("XxlSsoTokenFilter");
//        registration.setOrder(1);
//        registration.addUrlPatterns("/*");
//        registration.setFilter(new XxlSsoTokenFilter());
//        registration.addInitParameter(Conf.SSO_SERVER, xxlSsoServer);
//        List<String> urls = ignoredUrlsProperties.getUrls();
//        if (urls == null || urls.size() == 0) {
//            urls = xxlSsoIgnorePaths.getPaths();
//        }
//        registration.addInitParameter(Conf.SSO_EXCLUDED_PATHS, StrUtil.join(",", urls));
//        registration.addInitParameter("appCode",appCode);
//        return registration;
//    }
@Bean
public FilterRegistrationBean xxlSsoFilterRegistration() {

    // xxl-sso, filter init
    FilterRegistrationBean registration = new FilterRegistrationBean();
//       启用redis
    SsoManager.setSsoTokenStore(new SsoTokenLoginHelper());
    registration.setName("ActSsoFilter");
    registration.setOrder(2);
    registration.addUrlPatterns("/*");
    registration.setFilter(new ActSsoFilter());
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_SERVER, xxlSsoServer);
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_LOGOUT_PATH, logoutUrl);
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_EXPIRE_TIME, String.valueOf(ssoExpireSecond));
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.ACT_SSO_SERVER, actSsoServer);
    String actLogoutUrlNew=actLogoutUrl.concat("/login");
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.ACT_SSO_LOGOUT_PATH, actLogoutUrlNew);
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_ACT_EXPIRE_TIME,  String.valueOf(actSsoExpireSecond));

    List<String> urls = ignoredUrlsProperties.getUrls();
    if (urls == null || urls.size() == 0) {
        urls = xxlSsoIgnorePaths.getPaths();
    }
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_EXCLUDED_PATHS, StrUtil.join(",", urls));
    registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_APPCODE,appCode);

    return registration;
}
    @Override
    public void afterPropertiesSet() throws Exception {
//        SsoLoginStore.setRedisExpireMinite(redisExpireMinite);
    }
    @Bean
    public FilterRegistrationBean actFilter() {

        // xxl-sso, filter init
        FilterRegistrationBean registration = new FilterRegistrationBean();

        registration.setName("ActFilter");
        registration.setOrder(1);
        registration.addUrlPatterns("/*");
        ActFilter actFilter=new ActFilter();
        actFilter.setMatrixApiUtil(matrixApiUtil);
        registration.setFilter(actFilter);
        registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_SERVER, xxlSsoServer);
        registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_LOGOUT_PATH, logoutUrl);
        registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_EXPIRE_TIME, String.valueOf(ssoExpireSecond));

        List<String> urls = ignoredUrlsProperties.getUrls();
        if (urls == null || urls.size() == 0) {
            urls = xxlSsoIgnorePaths.getPaths();
        }
        registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_EXCLUDED_PATHS, StrUtil.join(",", urls));
        registration.addInitParameter(com.ytdinfo.core.conf.Conf.SSO_APPCODE,appCode);

        return registration;
    }
    @Override
    public void destroy() throws Exception {
    }

    @Bean
    public XxlSsoTokenFilter getXxlSsoTokenFilter() {
        return new XxlSsoTokenFilter();
    }

}
