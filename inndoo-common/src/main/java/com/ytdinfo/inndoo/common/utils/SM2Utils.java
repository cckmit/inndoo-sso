package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.asymmetric.SM2Engine;
import com.ytdinfo.conf.core.annotation.XxlConf;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.springframework.stereotype.Component;

/**
 * 国密 SM2 加密
 *
 * @author zhulin
 * @Version v1.0.0
 * @date 2022-01-27 4:41 下午
 **/
@Slf4j
@Component
public class SM2Utils {

    @XxlConf("matrix.sm2.privatekey64")
    private String privatekey;

    @XxlConf("matrix.sm2.publickey64")
    private String publickey;

    private SM2 SM_2;


    /**
     * 创建密钥
     */
    public static void createSm2Key() {
        SM2 sm = SmUtil.sm2();
        // sm2的加解密时有两种方式即 C1C2C3、 C1C3C2
        // 默认使用 C1C3C2
        sm.setMode(SM2Engine.SM2Mode.C1C3C2);

        // 生成私钥 (后端使用base64)
        String privateKey64 = sm.getPrivateKeyBase64();
        System.out.println("私钥(后端使用base64)" + privateKey64);
        // 生成公钥 (后端使用base64)
        String publickey64 = sm.getPublicKeyBase64();
        System.out.println("公钥钥(后端使用base64)" + publickey64);

        // 生成私钥 (编码为Hex字符串)
        String privateKey = HexUtil.encodeHexStr(sm.getPrivateKey().getEncoded());
        System.out.println("私钥: {}" + privateKey);
        // 生成公钥 (编码为Hex字符串)
        String publicKey = HexUtil.encodeHexStr(sm.getPublicKey().getEncoded());
        System.out.println("公钥: {}" + publicKey);

        // 生成私钥D (前端使用)
        String privateKeyD = HexUtil.encodeHexStr(((BCECPrivateKey) sm.getPrivateKey()).getD().toByteArray()); // ((BCECPrivateKey) privateKey).getD().toByteArray();
        System.out.println("私钥D: {}" + privateKeyD);

        // 生成公钥Q，(前端使用) 以q值做为js端的加密公钥
        String publicKeyQ = HexUtil.encodeHexStr(((BCECPublicKey) sm.getPublicKey()).getQ().getEncoded(false));
        System.out.println("公钥Q: {}" + publicKeyQ);
    }

    /**
     * 获取SM2
     * @return
     */
    private SM2 getSM2() {
        if (SM_2 == null) {
            SM_2 = SmUtil.sm2(privatekey, publickey);
        }
        return SM_2;
    }


    /**
     * SM2 加密
     * 默认使用配置中心的公钥加密
     * 配置项 matrix.sm2.publickey64
     * @param content
     */
    public String encrypt(String content) throws Exception {
        if (StrUtil.isBlank(content)) {
            return "";
        } else {
            try {
                return getSM2().encryptBcd(content, KeyType.PublicKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * SM2 解密
     * 默认使用配置中心私钥解密
     * 配置项 matrix.sm2.privatekey64
     * @param content
     * @return
     */
    public String decrypt(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        } else {
            try {
                if (!content.startsWith("04")) {
                    content = "04".concat(content);
                }
                byte[] data = getSM2().decryptFromBcd(content, KeyType.PrivateKey);
                return StrUtil.utf8Str(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    /**
     * SM2 加密
     * @param content 名文
     * @param publickey64 公钥
     * @return
     * @throws Exception
     */
    public String encrypt(String content,String publickey64) throws Exception {
        if (StrUtil.isBlank(content)) {
            return "";
        } else {
            try {
                SM2 sm2 = SmUtil.sm2(null, publickey64);
                return sm2.encryptBcd(content, KeyType.PublicKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }


    /**
     * 解密
     * @param content 密文
     * @param privatekey64 私钥
     * @return
     */
    public String decrypt(String content ,String privatekey64){
        if (StrUtil.isBlank(content)) {
            return "";
        } else {
            try {
                if (!content.startsWith("04")) {
                    content = "04".concat(content);
                }
                SM2 sm2 = SmUtil.sm2(privatekey64, null);
                byte[] data = sm2.decryptFromBcd(content, KeyType.PrivateKey);
                return StrUtil.utf8Str(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }


    /**
     * 设定指定的公钥 私钥 （base64位）
     * @param privatekey
     * @param publickey
     */
    public void init(String privatekey,String publickey){
        SM_2 = new SM2(privatekey,publickey);
    }

    public static void main(String[] args) {
        SM2Utils sm2Utils = new SM2Utils();
        try {
            String privatekey = "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgn1ABWedtUoujWXCCwMfKnCXLgj1riMWf/S2SB6xodZKgCgYIKoEcz1UBgi2hRANCAASr46qtB3vXFtYPnSSzvN+KRTt5zrg7x+nR3rK4Rtp2ZRGhmXg/TsjVoSbhuMa/GY/pc/rm2o0laBMOiYuG+ulR";
            String publickey =  "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEq+OqrQd71xbWD50ks7zfikU7ec64O8fp0d6yuEbadmURoZl4P07I1aEm4bjGvxmP6XP65tqNJWgTDomLhvrpUQ==";
            // 设定公钥私钥 不执行init方法则则使用配置中心配置项 matrix.sm2.privatekey64 ，matrix.sm2.publickey64
            sm2Utils.init(privatekey,publickey);

            // 加密
            String content = sm2Utils.encrypt("123456abcd你好");
            System.out.println("加密后："+content);

            //解密
            String message = "0e6f7da2aabd4ead9d0aee5e85ba72bdd20f49f28e0f6653674fe235250eec9227c4dec6211289623f77563092fa7c2f36a6fb675eefee7fcb1185cf14c31bb1153875168487f23a400d0e9d25f71d37228cf1c42b76a1a6816771b5169af56c7194fd3c9d3ed7dc5f8bfa063de7b849";
            String decryptContext = sm2Utils.decrypt(message);
            System.out.println("解密后：" +decryptContext);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}