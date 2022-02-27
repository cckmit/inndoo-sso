package com.ytdinfo.inndoo.common.utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * @author zhuzheng
 */

public class DESedeUtil {

    public static void main(String[] args) {
        String key = "9elLyFDHCLBDtAE0";
        String str1 = "3011612";//需要加密的字符串
        String str2 = "iOqfDP6wxkE=";//需要解密的字符串

        try {
            //加密
            String encryptString = encrypt(key, str1);
            //解密
            String decryptString = decrypt(key,str2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(String key, String src)
            throws Exception {
        DESedeKeySpec spec = new DESedeKeySpec(getKeyByte(key));
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
        Key desKey = keyfactory.generateSecret(spec);
        Cipher cipher = Cipher.getInstance("desede" + "/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, desKey);
        byte[] bOut = cipher.doFinal(src.getBytes());

        return Base64.encode(bOut);
    }

    public static String decrypt(String key, String src) throws Exception {
        DESedeKeySpec spec = new DESedeKeySpec(getKeyByte(key));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("desede");
        Key desKey = keyFactory.generateSecret(spec);

        Cipher cipher = Cipher.getInstance("desede" + "/ECB/PKCS5Padding");

        cipher.init(Cipher.DECRYPT_MODE, desKey);

        byte[] bOut = cipher.doFinal(Base64.decode(src));

        return new String(bOut, "UTF-8");
    }

    private static byte[] getKeyByte(String key) {
        if (key == null) {
            return new byte[0];
        }
        int length = key.length();
        if (length >= 24) {
            return key.substring(0, 24).getBytes();
        } else {
            for (int i = 0; i < (24 - length); i++) {
                key += "0";
            }
            return key.getBytes();
        }
    }

    public static byte[] getSha256Data(String oriText) throws Exception{
        return getSha256Data(oriText, 1);
    }

    private static byte[] getSha256Data(String oriText, int iterations)  throws Exception{
        try {
            MessageDigest e = MessageDigest.getInstance("SHA-256");

            byte[] result = e.digest(oriText.getBytes());

            for(int i = 1; i < iterations; ++i) {
                e.reset();
                result = e.digest(result);
            }

            return result;
        } catch (GeneralSecurityException var7) {
            throw new Exception(var7);
        }

    }

    /**
     * 签名
     * @param plainText 签名原文
     * @param algorithm 签名算法
     * @param privateKey    私钥
     * @return  签名结果(BASE64)
     * @throws Exception 签名异常
     */
    public static String sign(String plainText, String algorithm, PrivateKey privateKey ) throws Exception {
        try {
            Signature sig = Signature.getInstance(algorithm);
            sig.initSign(privateKey);
            sig.update(plainText.getBytes());
            byte[] b = sig.sign();
            return java.util.Base64.getEncoder().encodeToString(b);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("签名异常，不支持算法[{}]"+algorithm+e);
            throw new Exception(e);
        } catch (SignatureException e) {
            System.out.println("签名异常，异常原因[{}]"+e.getMessage());
            throw new Exception(e);
        } catch (InvalidKeyException e) {
            System.out.println("签名异常，无效密钥[{}]"+privateKey);
            throw new Exception(e);
        }
    }

    /**
     * 从classpath下读取文件内容
     * @param path  资源路径
     * @return  文件内容（base64编码)
     * @throws IOException
     */
    public static String readBase64FromResource(String path) throws IOException, URISyntaxException {
        URL url = ClassLoader.getSystemResource(path);
        return readBase64FromFile(url.getPath());
    }

    /**
     * 根据指定的文件路径读取文件内容
     * @param path  文件绝对路径或相对路径
     * @return  文件内容（base64编码)
     * @throws IOException
     * @throws URISyntaxException
     */
    public static String readBase64FromFile(String path) throws IOException {
        File file = new File(path);
        return readBase64FromFile(file);
    }

    /**
     * 读取指定文件的内容
     * @param file  文件
     * @return  文件内容(Base64编码)
     * @throws IOException
     */
    public static String readBase64FromFile(File file) throws IOException {
        int iBuffSize = (int)file.length();
        InputStream is = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(iBuffSize);
        byte[] buffer = new byte[iBuffSize];
        int readLength = 0;
        while( (readLength = is.read(buffer)) >= 0){
            baos.write(buffer, 0, readLength);
        }
        try {
            is.close();
        }catch (IOException ex){
            System.out.println("关闭文件流发生异常!{}"+file.getPath());
        }
        String fileContent = Base64.encode(baos.toByteArray());
        return fileContent;
    }

    /**
     * 获取私钥
     * @param pfxContent   私钥证书Base64密文
     * @param password      私钥密码
     * @return  私钥
     * @throws Exception 解析异常
     */
    public static PrivateKey readPrivateKey(String pfxContent, String password, String keyStoreType) throws Exception{
        char[] pass = password.toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            ByteArrayOutputStream os = new ByteArrayOutputStream(pfxContent.length());
            ks.load(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(pfxContent)), pass);
            Enumeration<String> aliases = ks.aliases();
            String alias = aliases.nextElement();
            if (null == alias) {
                throw new Exception("解析证书失败，找不到证书别名");
            }
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, pass);
            return privateKey;
        }catch(KeyStoreException e){
            System.out.println("读取私钥异常KeyStoreType[{}]"+keyStoreType);
            throw new Exception(e);
        } catch (CertificateException e) {
            throw new Exception(e);
        } catch (UnrecoverableKeyException e) {
            throw new Exception(e);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("读取私钥失败，找不到支持的算法");
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        }
    }

    /**
     * 获取公钥证书
     * @param pemContent    公钥证书Base64密文
     * @return  公钥证书
     * @throws Exception 解析异常
     */
    public static X509Certificate loadCertificate(String pemContent) throws Exception {
        CertificateFactory factory = null;
        try {
            factory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream is = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(pemContent));
            X509Certificate certificate = (X509Certificate)factory.generateCertificate(is);
            return certificate;
        } catch (CertificateException e) {
            System.out.println("加载证书失败,{}"+pemContent+e);
            throw new Exception(e);
        }
    }

    /**
     * 验签
     * @param plainText 签名原文
     * @param signedText 签名
     * @param algorithm  签名算法
     * @param publicKey 公钥证书
     * @return  验签结果    通过返回true,不通过返回false
     * @throws Exception 验签异常
     */
    public static boolean verify(String plainText, String signedText, String algorithm, PublicKey publicKey ) throws Exception{
        try {
            Signature sig = Signature.getInstance(algorithm);
            sig.initVerify(publicKey);
            sig.update(plainText.getBytes());
            byte[] b = java.util.Base64.getDecoder().decode(signedText);
            return sig.verify(b);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("不支持的算法[{}]"+algorithm);
            throw new Exception(e);
        } catch (SignatureException e) {
            System.out.println("验签失败,原文[{}]，签名[{}]"+plainText+signedText);
            throw new Exception(e);
        } catch (InvalidKeyException e) {
            System.out.println("证书不可用[{}]"+publicKey);
            throw new Exception(e);
        }catch(Exception e){
            System.out.println("验签失败："+e);
            throw new Exception(e);
        }
    }

}
