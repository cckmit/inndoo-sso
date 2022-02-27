package com.ytdinfo.inndoo.utils;

import com.ytdinfo.util.StringUtils;

/**
 * @FileName: PrivacyUtil
 * @Author: zhulin
 * @Date: 2020/9/16 9:57 AM
 * @Description: 隐私敏感信息处理工具类
 */
public class PrivacyUtil {

    //身份证前三后四脱敏
    public static String idNumberEncrypt(String idNumber) {
        if (StringUtils.isEmpty(idNumber) || (idNumber.length() < 8)) {
            return idNumber;
        }
        return idNumber.replaceAll("(?<=\\w{3})\\w(?=\\w{4})", "*");
    }

    // 手机号码前三后四脱敏

    /**
     * 功能描述: <br>
     * 〈手机号加密〉
     *  tip： 总长度小于6位不加密
     *        总长度6到10位  4-6位加密
     *        总长度大于等于11位  除前三后四 其余加密
     * @param mobile
     * @return:
     * @since: 1.0.0
     * @Author: zhulin
     * @Date: 2020/9/16 10:29 AM
     */
    public static String phoneEncrypt(String mobile) {
        if (StringUtils.isEmpty(mobile) || (mobile.length() < 6)) {
            return mobile;
        }
        if(mobile.length() < 7 ){
            return mobile.replaceAll("(?<=\\w{3})\\w(?)", "*");
        }else{
            Integer lastNum = mobile.length() - 6;
            if(lastNum > 4){
                lastNum = 4;
            }
            return mobile.replaceAll("(?<=\\w{3})\\w(?=\\w{"+lastNum+"})", "*");
        }

    }

    // 姓名脱敏
    public static String nameEncrypt(String name) {
        if (StringUtils.isEmpty(name) ) {
            return name;
        }
        if ( name.length()==1) {
            return "*";
        }
        if(name.length() == 2){
            return name.replaceAll("(?<=\\S{1})\\S(?)", "*");
        }else {
            return name.replaceAll("(?<=\\S{1})\\S(?=\\S{1})", "*");
        }
    }


    //其他脱敏
    public static String formatToMask(String src){
        src=src.replaceAll(" ","");
        if (StringUtils.isEmpty(src)) {
            return "";
        }
        int start=0;
        int end=0;
        if ( src.length()==1) {
            return "*";
        }
        else if ( src.length()==2) {start=1;end=0; }
        else if ( src.length()==3) { start=1;end=1;}
        else if ( src.length()==4) { start=1;end=1;}
        else if ( src.length()==5) { start=1;end=1;}
        else if ( src.length()==6) { start=2;end=2;}
        else if ( src.length()==7) { start=2;end=2; }
        else if ( src.length()==8) { start=2;end=3; }
        else if ( src.length()==9) {start=2;end=3; }
        else  {start=3;end=4; }
        String regex = "(?<=\\S{"+start+"})\\S(?=\\S{"+end+"})";
        return src.trim().replaceAll(regex, "*");



    }


    public static void main(String[] args) {
//        String name1 = "张无";
//        String name2 = "张无是忌";
//        String name3 = "张无忌";
//        System.out.println(nameEncrypt(name1) );
//        System.out.println(nameEncrypt(name2) );
        System.out.println(formatToMask("北京中关村分行 营业部2") );
        System.out.println(formatToMask("北京中关村分行    营业部2") );
        System.out.println(formatToMask("12") );
        System.out.println(formatToMask("123") );
        System.out.println(formatToMask("1234") );
        System.out.println(formatToMask("12345") );
        System.out.println(formatToMask("123456") );
        System.out.println(formatToMask("1234567") );
        System.out.println(formatToMask("12345678") );
        System.out.println(formatToMask("123456789") );
        System.out.println(formatToMask("1234567890") );
        System.out.println(formatToMask("12345678901") );
        System.out.println(formatToMask("123456789012") );
        System.out.println(formatToMask("1234567890123") );
        System.out.println(formatToMask("12345678901234") );
    }
}
