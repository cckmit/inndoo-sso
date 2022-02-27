package com.ytdinfo.inndoo.config.xxlconf;

import com.ytdinfo.conf.core.spring.XxlConfFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxl-conf config
 *
 * @author xuxueli 2017-04-28
 */
@Configuration
@Slf4j
public class XxlConfConfig {
    private Logger logger = LoggerFactory.getLogger(XxlConfConfig.class);


    @Value("${xxl.conf.admin.address}")
    private String adminAddress;

    @Value("${xxl.conf.env}")
    private String env;

    @Value("${xxl.conf.access.token}")
    private String accessToken;

    @Value("${xxl.conf.mirrorfile}")
    private String mirrorfile;


    @Bean
    public XxlConfFactory xxlConfFactory() {

        XxlConfFactory xxlConf = new XxlConfFactory();
        xxlConf.setAdminAddress(adminAddress);
        xxlConf.setEnv(env);
        xxlConf.setAccessToken(accessToken);
        String basePath = System.getProperty("user.dir");
        xxlConf.setMirrorfile(basePath + "/" + mirrorfile);

        logger.info(">>>>>>>>>>> xxl-conf config init.");
        return xxlConf;
    }

}