package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * AES加密工具类
 *
 * @author timmy
 */
@Component
public class AESUtil {

    @XxlConf("activity.aes.password")
    public static String PASSWORD;


    @XxlConf("core.private.aes.password")
    public static String PRIVATEPASSWORD;

    @XxlConf("activity.aes.password.wx")
    public static String WXLOGIN_PASSWORD;

    @XxlConf("activity.classicactivity.aes.password")
    public static String CLASSIC_ACTIVITY_AES_PASSWORD;

    private static Map<String, byte[]> keyMap = new HashMap<>();

    @Autowired
    private DictDataService dictDataService;


    //保存加密方法
    public static String encrypt(String message) {
        String currentpassword="";
        if(StringUtils.isNotEmpty(PRIVATEPASSWORD))
        {
            currentpassword=PRIVATEPASSWORD;
        }
        else
        {
            currentpassword=PASSWORD;
        }
        return encrypt(message, currentpassword);
    }

    //原文使用通用密码传出
    public static String comEncrypt(String message) {
        return encrypt(message, PASSWORD);
    }


    /**
     * AES加密字符串，采用默认密码
     *
     * @param message 待加密字符串
     * @return 加密后字符串
     */
    public static String encrypt4v1(String message) {
        return encrypt(message, CLASSIC_ACTIVITY_AES_PASSWORD);
    }

    /**
     * AES加密字符串
     *
     * @param message  待加密字符串
     * @param password 加密密码
     * @return 加密后字符串
     */
    public static String encrypt(String message, String password) {
        if (StrUtil.isEmpty(message) || StrUtil.isEmpty(password)) {
            return "";
        }
        byte[] key = getKey(password);
        return SecureUtil.aes(key).encryptHex(message);
    }

    private static byte[] getKey(String password) {
        byte[] key = keyMap.get(password);
        if (key == null) {
            byte[] keyBytes = Arrays.copyOf(password.getBytes(CharsetUtil.charset("ASCII")), 16);
            KeySpec keySpec = new SecretKeySpec(keyBytes, SymmetricAlgorithm.AES.getValue());
            key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), keySpec).getEncoded();
            keyMap.put(password, key);
        }
        return key;
    }


    //解密
    public static String decrypt(String message) {
        String currentpassword="";
        if(StringUtils.isNotEmpty(PRIVATEPASSWORD))
        {
            currentpassword=PRIVATEPASSWORD;
        }
        else
        {
            currentpassword=PASSWORD;
        }
        return decrypt(message, currentpassword);
    }


    //通用密码解密，用于处理旧数据
    public static String comDecrypt(String message) {
        return decrypt(message, PASSWORD);
    }


    /**
     * AES解密字符，采用默认密码
     *
     * @param message 待解密字符串
     * @return 解密后明文
     */
    public static String decrypt4v1(String message) {
        return decrypt(message, CLASSIC_ACTIVITY_AES_PASSWORD);
    }

    /**
     * AES解密字符
     *
     * @param message  待解密字符串
     * @param password 解密密码
     * @return 解密后明文
     */
    public static String decrypt(String message, String password) {
        if (StrUtil.isEmpty(message) || StrUtil.isEmpty(password)) {
            return "";
        }
        byte[] key = getKey(password);
        return SecureUtil.aes(key).decryptStr(message, CharsetUtil.CHARSET_UTF_8);
    }

    public static void main(String[] args) {

    }

}