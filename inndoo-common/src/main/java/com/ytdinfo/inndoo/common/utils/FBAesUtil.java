package com.ytdinfo.inndoo.common.utils;

import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
//import org.apache.commons.lang.StringUtils;

/**
 * @ClassName FBAesUtil
 * @Author ML
 * @Author 2020/11/16 9:05
 **/

public class FBAesUtil {

    //密钥 (需要前端和后端保持一致)十六位作为密钥
    private static final String KEY = "ytdInfForTodoKey";

    //密钥偏移量 (需要前端和后端保持一致)十六位作为密钥偏移量
    private static final String IV = "ytdInfForTodo_Iv";

    //算法
    private static final String ALGORITHMSTR = "AES/CBC/PKCS5Padding";

    //SSO的TOKEN
    private static final String token = "4AaWABnPB5/1Z2EIEXwSfigudpiM4kzLkmIrKQPmS9w=";
    /**
     * base 64 decode
     * @param base64Code 待解码的base 64 code
     * @return 解码后的byte[]
     * @throws Exception
     */
    public static byte[] base64Decode(String base64Code) throws Exception{
        return StringUtils.isEmpty(base64Code) ? null : new Base64().decodeBase64(new String(base64Code).getBytes());
    }

    /**
     * AES解密
     * @param encryptBytes 待解密的byte[]
     * @return 解密后的String
     * @throws Exception
     */
    public static String aesDecryptByBytes(byte[] encryptBytes) throws Exception {

        Cipher cipher = Cipher.getInstance(ALGORITHMSTR);

        byte[] temp = IV.getBytes("UTF-8");
        IvParameterSpec iv = new IvParameterSpec(temp);

        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY.getBytes(), "AES"), iv);
        byte[] decryptBytes = cipher.doFinal(encryptBytes);

        System.out.print(new String(decryptBytes));
        return new String(decryptBytes);
    }

    /**
     * 将base 64 code AES解密
     * @param encryptStr 待解密的base 64 code
     * @return 解密后的string
     * @throws Exception
     */
    public static String aesDecrypt(String encryptStr) {

        String backsstr="";
        if ( StringUtils.isEmpty(encryptStr))
        {
            return null;
        }
        else
        {
            try {
                backsstr= aesDecryptByBytes(base64Decode(encryptStr));
            }
            catch ( Exception e)
            {
                return null;
            }
        }
        return backsstr;
    }

    //AES加密
    public static String aesEncrypt(String str) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
            IvParameterSpec iv = new IvParameterSpec(IV.getBytes("UTF-8"));
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY.getBytes("UTF-8"), "AES"), iv);
            byte[] encryptBytes = cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));

            return new String(Base64.encodeBase64(encryptBytes),StandardCharsets.UTF_8);
            //return java.util.Base64.getEncoder().encodeToString(encryptBytes);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String[] args) throws Exception {
        String kk=  aesDecrypt("wfTHI7TmQG2wPP4mPeqGzCVxFmW4+v5f4mg69P600nBPKLxxy+kdnPdkcfkxdkWfxOp3WRLozdg1XC4+nD52vb22PoNAHUiz1Ir+t39oUys=");
//        String kkl6l=  aesDecrypt("OLuCobSF3zncmu9VTLD+ew==");
//        String kde = aesEncrypt("2a2d701517733c6ecdefedb036737858ea351109a5a6ce2072a67169cc811a12");
//        String a1=aesEncrypt("338770894050320390");
        System.out.println(kk);
//        try {
//            String a2 =aesDecryptByBytes(base64Decode(a1));
//            System.out.println(a2);
//            System.out.println(a2);
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }
}