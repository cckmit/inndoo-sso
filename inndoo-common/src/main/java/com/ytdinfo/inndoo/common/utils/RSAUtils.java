package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.ytdinfo.conf.core.annotation.XxlConf;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Decoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Rsa拓展工具
 *
 * @author zhulin
 * @Version v1.0.0
 * @date 2022-01-27 9:13 上午
 **/
@Component
public class RSAUtils {

    @XxlConf("matrix.rsa.privatekey")
    private String privatekey;

    @XxlConf("matrix.rsa.publickey")
    private String publickey;

    private RSA rsa;

    private RSA getTsa() throws Exception {
        if (rsa == null) {
            rsa = new RSA();
            RSAPrivateKey rsaPrivateKey = getRSAPrivateKeyBybase64(privatekey);
            RSAPublicKey rsaPublicKey = getRSAPublidKeyBybase64(publickey);
            rsa.setPrivateKey(rsaPrivateKey);
            rsa.setPublicKey(rsaPublicKey);
        }
        return rsa;
    }


    /**
     * 加密
     * @param content
     * @return
     */
    public String encrypt(String content) {
        if(StrUtil.isBlank(content)){
            return content;
        }
        try {
            return getTsa().encryptBcd(content, KeyType.PublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    /**
     * 解密
     * @param content
     * @return
     */
    public String decrypt(String content) {
        if(StrUtil.isBlank(content)){
            return content;
        }
        try {
            return getTsa().decryptStr(content, KeyType.PrivateKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * @param
     * @return
     * @desc: 将字符串转换成RSAPublicKey类型
     * @date 2020-6-12 11:03:05
     */
    private static RSAPublicKey getRSAPublidKeyBybase64(String base64s) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec((new BASE64Decoder()).decodeBuffer(base64s));
        RSAPublicKey publicKey = null;
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        try {
            publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException var4) {

        }
        return publicKey;
    }

    /**
     * @param
     * @return
     * @desc: 将字符串转换成RSAPrivateKey类型
     * @date 2020-6-12 11:03:01
     */
    private static RSAPrivateKey getRSAPrivateKeyBybase64(String base64s) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec((new BASE64Decoder()).decodeBuffer(base64s));
        RSAPrivateKey privateKey = null;
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        try {
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException var4) {
        }
        return privateKey;
    }


}