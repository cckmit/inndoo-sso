package com.ytdinfo.inndoo.config.jasypt;

import com.ytdinfo.inndoo.common.utils.AESEncryptor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by timmy on 2019/5/14.
 */
@Configuration
public class EncryptionConfig {
    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        AESEncryptor aesEncryptor = new AESEncryptor();//调用我们自己实现的类即可
        return aesEncryptor;
    }
}