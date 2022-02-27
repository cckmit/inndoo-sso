package com.ytdinfo.inndoo.common.utils;

import io.micrometer.core.instrument.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 判断电话号码是否正确工具类
 */
public class PhoneUtil {
    /**
     * 验证手机号是否合法
     * @return
     */
    public static boolean isMobileNO(String mobile){
        if (mobile.length() != 11)
        {
            return false;
        }else{
            /**
             * 11位简单判断
             */
            String pat1 = "^[1][0-9]{10}$";
            /**
             * 联通号段正则表达式
             */
            String pat2  = "^((13[0-2])|(145)|(15[5-6])|(176)|(18[5,6]))\\d{8}|(1709)\\d{7}$";
            /**
             * 电信号段正则表达式
             */
            String pat3  = "^((133)|(153)|(177)|(18[0,1,9])|(149))\\d{8}$";
            /**
             * 虚拟运营商正则表达式
             */
            String pat4 = "^((170))\\d{8}|(1718)|(1719)\\d{7}$";

            Pattern pattern1 = Pattern.compile(pat1);
            Matcher match1 = pattern1.matcher(mobile);
            boolean isMatch1 = match1.matches();
            if(isMatch1){
                return true;
            }
            Pattern pattern2 = Pattern.compile(pat2);
            Matcher match2 = pattern2.matcher(mobile);
            boolean isMatch2 = match2.matches();
            if(isMatch2){
                return true;
            }
            Pattern pattern3 = Pattern.compile(pat3);
            Matcher match3 = pattern3.matcher(mobile);
            boolean isMatch3 = match3.matches();
            if(isMatch3){
                return true;
            }
            Pattern pattern4 = Pattern.compile(pat4);
            Matcher match4 = pattern4.matcher(mobile);
            boolean isMatch4 = match4.matches();
            if(isMatch4){
                return true;
            }
            return false;
        }
    }

    /**
     * 处理电话特殊字符问题
     *
     * @param phone
     * @return
     */
    public static String dealPhoneNumber(String phone) {
        if (StringUtils.isNotBlank(phone)) {
            phone = removeNonAscii(phone);
            phone = removeSomeControlChar(phone);
            phone = removeFullControlChar(phone).trim();
            return phone;
        }
        return null;
    }

    /**
     * 数字过滤
     *
     * @param str
     * @return
     */
    public static String digitFileter(String str) {
        if (StringUtils.isNotBlank(str)) {
            char[] num = str.trim().toCharArray();
            char[] arr = new char[num.length];
            int j = 0;
            for (int i = 0; i < num.length; i++) {
                if (Character.isDigit(num[i])) {
                    arr[j] = num[i];
                    j++;
                }
            }
            return String.valueOf(arr).trim();
        }
        return null;
    }

    /**
     * 去除非ascii码字符
     *
     * @param str
     * @return
     */
    public static String removeNonAscii(String str) {
        return str.replaceAll("[^\\x00-\\x7F]", "");
    }

    /**
     * 去除不可打印字符
     *
     * @param str
     * @return
     */
    public static String removeNonPrintable(String str) {
        return str.replaceAll("[\\p{C}]", "");
    }

    /**
     * 去除一些控制字符 Control Char
     *
     * @param str
     * @return
     */
    public static String removeSomeControlChar(String str) {
        return str.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}]", ""); // Some Control Char
    }

    /**
     * 去除一些换行制表符
     *
     * @param str
     * @return
     */
    public static String removeFullControlChar(String str) {
        return removeNonPrintable(str).replaceAll("[\\r\\n\\t]", "");
    }

}
