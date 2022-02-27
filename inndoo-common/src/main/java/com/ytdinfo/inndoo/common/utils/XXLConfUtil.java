package com.ytdinfo.inndoo.common.utils;

import com.ytdinfo.conf.core.annotation.XxlConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class XXLConfUtil {

    @XxlConf("inndoo-sso.redis.mode")
    public static String redisMode;

    @XxlConf("matrix.inndoo.upload.filepath")
    public static String uploadFilePath;

    @XxlConf("matrix.inndoo.upload.publicurl")
    public static String uploadPublicUrl;

    @XxlConf("matrix.inndoo.upload.serviceprovider")
    public static String serviceProvider;


}
