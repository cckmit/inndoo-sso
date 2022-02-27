package com.ytdinfo.inndoo.common.utils;

import org.jasypt.encryption.StringEncryptor;

/**
 * Created by timmy on 2019/5/14.
 */
public class AESEncryptor implements StringEncryptor {
    @Override
    public String encrypt(String message) {
        return AESUtil.encrypt(AESUtil.PASSWORD,message);
    }

    @Override
    public String decrypt(String encryptedMessage) {
        return AESUtil.decrypt(AESUtil.PASSWORD,encryptedMessage);
    }
}