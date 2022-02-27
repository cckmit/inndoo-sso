package com.ytdinfo.inndoo.common.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.*;
import java.util.*;

/**
 * @author wangkai
 * @2016年4月7日 下午12:53:55
 * @desc:日期工具
 */
public class DateUtils {

    public static final int              REALTIME     = 0;
    public static final int              DAILY        = 1;
    public static final int              WEEKLY       = 2;
    public static final int              MONTHLY      = 3;
    public static final int              BIWEEKLY     = 4;
    public static final int              HOURLY       = 5;
    public static final int              YEARLY       = 6;
    public static final int              QUARTLY      = 7;
    public static final int              BIYEARLY     = 8;
    public static final int              TOTALYEARLY  = 9;
    public static final int              REPORTWEEKLY = 10;
    public static final int              MINIUTELY    = 11;
    public static final int              SECONDLY     = 12;
    public static final String           PERCENT      = "@percent@";
    public static final int              TEMPMONTHLY  = 11;
    public static final String           SHORT_DATE   = "yyyy-MM-dd";
    public static final String           LONG_DATE    = "yyyy-MM-dd HH:mm:ss";
    public static final SimpleDateFormat DF_SHORT_CN  = new SimpleDateFormat(SHORT_DATE, Locale.US);
    public static final SimpleDateFormat DF_CN        = new SimpleDateFormat(LONG_DATE, Locale.US);

    /**
     * 获取当前系统的时间戳
     * 
     * @return
     */
    public static long getCurrentTimestamp() {

        long timeStamp = new Date().getTime();
        return timeStamp;
    }

    public static String getCurrentTimestamp10() {

        long timeStamp = new Date().getTime() / 1000;
        String timestr = String.valueOf(timeStamp);
        return timestr;
    }

    public static String getTimeStamp() {
        int time = (int) (System.currentTimeMillis() / 1000);
        return String.valueOf(time);
    }

    /**
     * @param String DATE1
     * @param String DATE2
     * @return 1 -- Date1 在 Date2之后; -1 -- Date1 在 Date2之前; 0 -- 时间相等
     */
    public static int compare_date(String DATE1, String DATE2) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date dt1 = df.parse(DATE1);
            Date dt2 = df.parse(DATE2);
            if (dt1.getTime() > dt2.getTime()) {
                return 1;
            } else if (dt1.getTime() < dt2.getTime()) {
                return -1;
            } else {
                return 0;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    /**
     * @param String DATE1
     * @param String DATE2
     * @return 1 -- Date1 在 Date2之后; -1 -- Date1 在 Date2之前; 0 -- 时间相等
     */
    public static int compare_datetime(String DATE1, String DATE2) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date dt1 = df.parse(DATE1);
            Date dt2 = df.parse(DATE2);
            if (dt1.getTime() > dt2.getTime()) {
                return 1;
            } else if (dt1.getTime() < dt2.getTime()) {
                return -1;
            } else {
                return 0;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    public static int compare_date(Date dt1, Date dt2) {

        try {
            if (dt1.getTime() > dt2.getTime()) {
                // System.out.println("dt1 在dt2前");
                return 1;
            } else if (dt1.getTime() < dt2.getTime()) {
                // System.out.println("dt1在dt2后");
                return -1;
            } else {
                return 0;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    /**
     * Calendar -> String
     */
    public static String format(Calendar cal) {
        return format(cal.getTime());
    }

    /**
     * Calendar,String -> String
     */
    public static String format(Calendar cal, String pattern) {
        return format(cal.getTime(), pattern);
    }

    /**
     * Calendar,DateFormat -> String
     */
    public static String format(Calendar cal, DateFormat df) {
        return format(cal.getTime(), df);
    }

    /**
     * Date -> String
     */
    public static String format(Date date) {
        return format(date, DF_CN);
    }

    /**
     * Date,String -> String
     */
    public static String format(Date date, String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        return format(date, df);
    }

    /**
     * Date,DateFormat -> String
     */
    public static String format(Date date, DateFormat df) {
        if (date == null) return "";

        if (df != null) {
            return df.format(date);
        }
        return DF_CN.format(date);
    }

    /**
     * String -> Calendar
     */
    public static Calendar parse(String strDate) {
        return parse(strDate, null);
    }

    /**
     * String,DateFormate -> Calendar
     */
    public static Calendar parse(String strDate, DateFormat df) {
        Date date = parseDate(strDate, df);
        if (date == null) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    /**
     * String -> Date
     */
    public static Date parseDate(String strDate) {
        return parseDate(strDate, null);
    }

    /**
     * String,DateFormate -> Date
     */
    public static Date parseDate(String strDate, DateFormat df) {
        if (df == null) df = DF_CN;
        ParsePosition parseposition = new ParsePosition(0);

        return df.parse(strDate, parseposition);
    }

    /**
     * returns the current date in the default format
     */
    public static String getToday() {
        return format(new Date());
    }

    public static Date getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);

        return cal.getTime();
    }

    public static Calendar getFirstDayOfMonth() {
        Calendar cal = getNow();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        return cal;
    }

    public static Calendar getNow() {
        // TimeZone tz = TimeZone.getTimeZone("ETC/GMT-8");
        // TimeZone.setDefault(tz);

        return Calendar.getInstance();
    }

    /**
     * add some month from the date
     */
    public static Date addMonth(Date date, int n) throws Exception {
        Calendar cal = getNow();
        cal.setTime(date);
        cal.add(Calendar.MONTH, n);
        return cal.getTime();
    }

    public static int daysBetween(Date returnDate) {
        return daysBetween(null, returnDate);
    }

    public static int daysBetween(Date now, Date returnDate) {
        if (returnDate == null) return 0;

        Calendar cNow = getNow();
        Calendar cReturnDate = getNow();
        if (now != null) {
            cNow.setTime(now);
        }
        cReturnDate.setTime(returnDate);
        setTimeToMidnight(cNow);
        setTimeToMidnight(cReturnDate);
        long nowMs = cNow.getTimeInMillis();
        long returnMs = cReturnDate.getTimeInMillis();
        return millisecondsToDays(nowMs - returnMs);
    }

    private static int millisecondsToDays(long intervalMs) {
        return (int) (intervalMs / (1000 * 86400));
    }

    private static void setTimeToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
    }

    public static String replaceAllPercent(String percent) {
        if (percent == null) return null;
        return percent.replaceAll(PERCENT, "%");
    }

    public static String EncodeQueryString(String value) {
        String result = "";
        try {
            result = URLEncoder.encode(value != null ? value.replaceAll("%", PERCENT) : null, "GBK");
        } catch (Exception e) {

        }
        return result;
    }

    public static String DecodeQueryString(String value) {
        String result = "";
        try {
            result = URLDecoder.decode(value, "GBK");
            result = new String(result.getBytes("ISO-8859-1"), "gb2312");
            result = replaceAllPercent(result);
        } catch (Exception e) {

        }
        return result;
    }

    public static boolean validateInteger(Object value) {

        boolean result = false;
        try {
            Integer.parseInt(value.toString());
            result = true;
        } catch (Exception e) {

        }
        return result;
    }

    public static boolean validateInterval(Object interval) {
        boolean result = false;
        try {
            int value = Integer.parseInt(interval.toString());
            if (value >= REALTIME && value <= TEMPMONTHLY) {
                result = true;
            }
        } catch (Exception e) {

        }

        return result;
    }

    public static String getNow(String format) {

        // TimeZone tz = TimeZone.getTimeZone("ETC/GMT-8");
        // TimeZone.setDefault(tz);

        Date dat = new Date();
        long ts = dat.getTime();
        return getTime(format, ts);
    }

    public static String getTime(String format, long ts) {
        Date dt = new Date(ts);
        SimpleDateFormat sf = new SimpleDateFormat(format);
        String now = sf.format(dt);
        return now;
    }

    public static Date getFormatDate(String format, String date) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date res = null;
        try {
            res = formatter.parse(date);
        } catch (ParseException ex1) {
            // ex1.printStackTrace();
        }
        return res;
    }

    public static String getDateString(Calendar cal, String dateformat) {
        String result = null;
        if (cal == null) return result;
        try {
            SimpleDateFormat df = new SimpleDateFormat(dateformat);
            result = df.format(cal.getTime());
        } catch (Exception e) {

        }
        return result;
    }

    public static String getDateString(Calendar cal, String dateformat, Locale local) {
        String result = null;
        if (cal == null) return result;
        try {
            SimpleDateFormat df = new SimpleDateFormat(dateformat, local);
            result = df.format(cal.getTime());
        } catch (Exception e) {

        }
        return result;
    }

    public static String getDateString(Date date, String dateformat) {
        String result = null;
        if (date == null) return result;
        try {
            SimpleDateFormat df = new SimpleDateFormat(dateformat);
            result = df.format(date);
        } catch (Exception e) {

        }
        return result;
    }

    public static Calendar parseDateString(String str, String format) {
        if (str == null) {
            return null;
        }
        Date date = null;
        SimpleDateFormat df = new SimpleDateFormat(format);
        try {
            date = df.parse(str);
        } catch (ParseException ex) {

        }
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public static String getYesterday(String format) {
        long ts = System.currentTimeMillis() - 86400000; // 24*60*60*1000
        return getTime(format, ts);
    }

    public static Calendar getFirstTime(String format, String str) {
        Calendar cal = Calendar.getInstance();

        try {
            SimpleDateFormat df = new SimpleDateFormat(format);
            Date date = df.parse(str);
            cal.setTime(date);
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
        }

        return cal;
    }

    public static String[] removeDublicatedElements(String[] strs) {
        if (strs == null || strs.length <= 1) {
            return strs;
        }
        List result = new ArrayList(0);
        for (int i = 0; i < strs.length; i++) {
            if (strs[i] == null) continue;
            if (!exists((String[]) result.toArray(new String[0]), strs[i])) {
                result.add(strs[i]);
            }
        }
        return (String[]) result.toArray(new String[0]);
    }

    public static Calendar getStartTime(Calendar calendar, int interval) {
        if (calendar == null) return null;
        Calendar fromtime = Calendar.getInstance();
        fromtime.setTimeZone(calendar.getTimeZone());
        fromtime.set(Calendar.MILLISECOND, 0);
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);
        if (interval == DAILY || interval == HOURLY) {
            fromtime.set(y, m, d, 0, 0, 0);
        } else if (interval == WEEKLY || interval == REPORTWEEKLY) {
            fromtime.set(y, m, d, 0, 0, 0);
            if (interval == REPORTWEEKLY) {
                if (fromtime.get(Calendar.DAY_OF_WEEK) <= 4) {
                    fromtime.add(Calendar.WEEK_OF_YEAR, -1);
                }
            }
            fromtime.add(Calendar.DATE, Calendar.SUNDAY - fromtime.get(Calendar.DAY_OF_WEEK));
            if (interval == REPORTWEEKLY) {
                fromtime.add(Calendar.DATE, 4);
            }
        } else if (interval == MONTHLY || interval == TEMPMONTHLY) {
            fromtime.set(y, m, 1, 0, 0, 0);
        } else if (interval == BIWEEKLY) {
            fromtime.set(y, m, d, 0, 0, 0);
            fromtime.add(Calendar.WEEK_OF_YEAR, (-1) * (fromtime.get(Calendar.WEEK_OF_YEAR) + 1) % 2);
            fromtime.add(Calendar.DATE, Calendar.SUNDAY - fromtime.get(Calendar.DAY_OF_WEEK));
        } else if (interval == YEARLY) {
            fromtime.set(y, m, d, 0, 0, 0);
        } else if (interval == QUARTLY) {
            fromtime.set(y, (m / 3) * 3, 1, 0, 0, 0);
        } else if (interval == BIYEARLY) {
            fromtime.set(y, (m / 6) * 6, 1, 0, 0, 0);
        } else if (interval == TOTALYEARLY) {
            fromtime.set(y, 0, 1, 0, 0, 0);
        }
        return fromtime;
    }

    public static Calendar getEndTime(Calendar calendar, int interval) {
        if (calendar == null) return null;
        Calendar endtime = Calendar.getInstance();
        endtime.setTimeZone(calendar.getTimeZone());
        endtime.set(Calendar.MILLISECOND, 0);
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);
        if (interval == DAILY) {
            endtime.set(y, m, d, 0, 0, 0);
            endtime.add(Calendar.DAY_OF_MONTH, 1);
        } else if (interval == WEEKLY || interval == REPORTWEEKLY) {
            endtime.set(y, m, d, 0, 0, 0);
            if (interval == REPORTWEEKLY) {
                if (endtime.get(Calendar.DAY_OF_WEEK) <= 4) {
                    endtime.add(Calendar.WEEK_OF_YEAR, -1);
                }
            }
            endtime.add(Calendar.DATE, 1 + Calendar.SATURDAY - endtime.get(Calendar.DAY_OF_WEEK));
            if (interval == REPORTWEEKLY) {
                endtime.add(Calendar.DATE, 4);
            }
        } else if (interval == MONTHLY || interval == TEMPMONTHLY) {
            endtime.set(y, m, 1, 0, 0, 0);
            endtime.add(Calendar.MONTH, 1);
        } else if (interval == BIWEEKLY) {
            endtime.set(y, m, d, 0, 0, 0);
            endtime.add(Calendar.WEEK_OF_YEAR, endtime.get(Calendar.WEEK_OF_YEAR) % 2);
            endtime.add(Calendar.DATE, 1 + Calendar.SATURDAY - endtime.get(Calendar.DAY_OF_WEEK));
        } else if (interval == YEARLY) {
            endtime.set(y + 1, m, d, 0, 0, 0);
        } else if (interval == QUARTLY) {
            if (m / 3 == 3) {
                endtime.set(y + 1, 0, 1, 0, 0, 0);
            } else {
                endtime.set(y, (m / 3 + 1) * 3, 1, 0, 0, 0);
            }
        } else if (interval == BIYEARLY) {
            if (m / 6 == 1) {
                endtime.set(y + 1, 0, 1, 0, 0, 0);
            } else {
                endtime.set(y, (m / 6 + 1) * 6, 1, 0, 0, 0);
            }
        } else if (interval == TOTALYEARLY) {
            endtime.set(y + 1, 0, 1, 0, 0, 0);
        }
        return endtime;
    }

    public static String replaceAllString(String source, String search, String replace) {
        StringBuffer sb = new StringBuffer(source);
        while (sb.indexOf(search) != -1) {
            int pos = sb.indexOf(search);
            sb.replace(pos, pos + search.length(), replace);
        }
        return sb.toString();
    }

    public static boolean isNumber(String s) {
        if (s == null) {
            return false;
        }
        try {
            Long.parseLong(s.trim());
            return true;
        } catch (NumberFormatException numberformatexception) {
            return false;
        }
    }

    public static String htmlEncoding(String source) {
        if (source == null) {
            return null;
        }
        return source.replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\'", "&apos;")
            .replaceAll("\"", "&quote;")
            .replaceAll(" ", "&nbsp;");
    }

    public static String urlEncode(String str) {
        if (str == null) {
            return null;
        }
        StringBuffer tmp = new StringBuffer();
        char a;
        for (int i = 0; i < str.length(); i++) {
            a = str.charAt(i);
            if ((a < 58 && a > 47) || (a < 91 && a > 64) || (a < 123 && a > 96) || a > 255) {
                tmp.append(a);
            } else if (a < 16) {
                tmp.append("%0" + Integer.toHexString(a));
            } else {
                tmp.append("%" + Integer.toHexString(a));
            }
        }
        return tmp.toString();
    }

    public static String urlDecode(String str) {
        if (str == null) {
            return null;
        }
        StringBuffer tmp = new StringBuffer();
        char a;
        for (int i = 0; i < str.length(); i++) {
            a = str.charAt(i);
            if (a != '%') {
                tmp.append(a);
            } else {
                char tmps = (char) Integer.valueOf(str.substring(i + 1, i + 3), 16).intValue();
                i += 2;
                tmp.append(tmps);
            }
        }
        return tmp.toString();
    }

    public static String toByteString(String source) {
        byte[] bytes = source.getBytes();
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) bytes[i] & 0xff, 16));
        }
        return buf.toString();
    }

    public static String addValueToString(String orignal, String newValue, String seperator) {
        String result = orignal;
        String[] array = string2Array(orignal, seperator);
        array = addValueToArray(array, newValue);
        result = array2String(array, seperator);
        return result;
    }

    public static String removeValueFromString(String orignal, String removeValue, String seperator) {
        String result = orignal;
        String[] array = string2Array(orignal, seperator);
        array = removeValueFromArray(array, removeValue);
        result = array2String(array, seperator);
        return result;
    }

    public static String[] addValueToArray(String[] orignal, String newValue) {
        String[] result = orignal;
        if (exists(orignal, newValue)) {
            return result;
        }
        List list = array2List(result);
        list.add(newValue);
        result = list2Array(list);
        return result;
    }

    public static boolean exists(String[] strs, String str) {
        boolean exists = false;
        for (int i = 0; strs != null && i < strs.length; i++) {
            if (equalsIgnoreCase(strs[i], str)) {
                exists = true;
                break;
            }
        }
        return exists;

    }

    public static boolean exists(List strs, String str) {
        boolean exists = false;
        for (int i = 0; strs != null && i < strs.size(); i++) {
            if (equalsIgnoreCase((String) strs.get(i), str)) {
                exists = true;
                break;
            }
        }
        return exists;

    }

    public static boolean include(String str, String substr, String seperator) {
        String[] strs = string2Array(str, seperator);
        return exists(strs, substr);
    }

    public static String[] removeValueFromArray(String[] orignal, String oldValue) {
        String[] result = orignal;
        List list = array2List(result);
        int i = list.size() - 1;
        while (i >= 0) {
            String item = (String) list.get(i);
            if (equalsIgnoreCase(item, oldValue)) {
                list.remove(i);
            }
            i--;
        }
        result = list2Array(list);
        return result;
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        boolean result = false;

        if (str1 == null && str2 == null) {
            result = true;
        }
        if (str1 != null && str2 != null && str1.trim().equalsIgnoreCase(str2)) {
            result = true;
        }
        return result;

    }

    public static String[] list2Array(List list) {
        String[] result = new String[0];
        try {
            result = (String[]) list.toArray(new String[list.size()]);
        } catch (Exception e) {

        }
        return result;

    }

    public static List array2List(String[] strs) {
        List result = new ArrayList(0);
        try {
            for (int i = 0; i < strs.length; i++) {
                result.add(strs[i]);
            }
        } catch (Exception e) {
        }
        return result;
    }

    public static String[] string2Array(String str, String seperator) {
        String[] result = new String[0];
        try {
            if (!str.trim().equalsIgnoreCase("")) result = str.split(seperator);
        } catch (Exception e) {

        }
        return result;
    }

    public static String array2String(String[] str, String seperator) {
        String result = "";
        if (str == null || str.length == 0) {
            return result;
        }
        boolean isFirst = true;
        for (int i = 0; i < str.length; i++) {
            if (isFirst) {
                isFirst = false;
            } else {
                result += seperator == null ? "" : seperator;
            }
            result += str[i] == null ? "" : str[i];

        }
        return result;
    }

    public static String getMaxValue(List results) {
        String result = null;
        Object tmp = null;
        if (results == null || results.size() == 0) {
            return result;
        }
        for (int i = 0; i < results.size() - 1; i++) {
            if (!isNumber(results.get(i))) continue;
            if (!isNumber(results.get(i + 1))
                || Double.parseDouble(results.get(i).toString()) > Double.parseDouble(results.get(i + 1).toString())) {
                tmp = results.get(i);
                results.set(i, results.get(i + 1));
                results.set(i + 1, tmp);
            }
        }
        return results.get(results.size() - 1) == null ? null : results.get(results.size() - 1).toString();
    }

    public static List getDates(String startdate, String enddate, int interval) {
        if (interval == DateUtils.TEMPMONTHLY) {
            List result = new ArrayList(0);
            result.add(startdate);
            return result;
        }
        String format = "yyyy-MM-dd";
        List result = new ArrayList(0);
        String sdate = DateUtils
            .getDateString(DateUtils.getStartTime(DateUtils.parseDateString(startdate, format), interval), format);
        String edate = DateUtils
            .getDateString(DateUtils.getStartTime(DateUtils.parseDateString(enddate, format), interval), format);
        if (sdate != null) {
            result.add(sdate);
        }
        while (edate != null && (DateUtils.parseDateString(sdate, format)
            .getTime()
            .getTime() < DateUtils.parseDateString(edate, format).getTime().getTime())) {
            sdate = AddDate(sdate, interval, 1);
            result.add(sdate);
        }
        return result;
    }

    public static String AddDate(String date, int interval, int value) {
        String result = "";
        String format = "yyyy-MM-dd";
        Calendar cal = DateUtils.parseDateString(date, format);
        if (cal == null) {
            return result;
        }
        if (interval == DAILY || interval == HOURLY) {
            cal.add(Calendar.DAY_OF_MONTH, value);
        } else if (interval == WEEKLY || interval == REPORTWEEKLY) {
            cal.add(Calendar.WEEK_OF_YEAR, value);
        } else if (interval == MONTHLY || interval == TEMPMONTHLY) {
            cal.add(Calendar.MONTH, value);
        } else if (interval == BIWEEKLY) {
            if (value > 0) {
                cal.add(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR) % 2);
            } else if (value < 0) {
                cal.add(Calendar.WEEK_OF_YEAR, (-1) * (cal.get(Calendar.WEEK_OF_YEAR) + 1) % 2);
            }
            cal.add(Calendar.WEEK_OF_YEAR, value * 2);
        } else if (interval == YEARLY) {
            cal.add(Calendar.YEAR, value);
        } else if (interval == QUARTLY) {
            cal.add(Calendar.MONTH, 3 * value);
        } else if (interval == BIYEARLY) {
            cal.add(Calendar.MONTH, 6 * value);
        } else if (interval == TOTALYEARLY) {
            cal.add(Calendar.YEAR, value);
        }
        result = DateUtils.getDateString(cal, format);
        return result;
    }

    public static Timestamp AddDateForTimestamp(String date, int interval, int value) {

        String format = "yyyy-MM-dd";
        Calendar cal = DateUtils.parseDateString(date, format);
        if (cal == null) {
            return null;
        }
        if (interval == DAILY || interval == HOURLY) {
            cal.add(Calendar.DAY_OF_MONTH, value);
        } else if (interval == WEEKLY || interval == REPORTWEEKLY) {
            cal.add(Calendar.WEEK_OF_YEAR, value);
        } else if (interval == MONTHLY || interval == TEMPMONTHLY) {
            cal.add(Calendar.MONTH, value);
        } else if (interval == BIWEEKLY) {
            if (value > 0) {
                cal.add(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR) % 2);
            } else if (value < 0) {
                cal.add(Calendar.WEEK_OF_YEAR, (-1) * (cal.get(Calendar.WEEK_OF_YEAR) + 1) % 2);
            }
            cal.add(Calendar.WEEK_OF_YEAR, value * 2);
        } else if (interval == YEARLY) {
            cal.add(Calendar.YEAR, value);
        } else if (interval == QUARTLY) {
            cal.add(Calendar.MONTH, 3 * value);
        } else if (interval == BIYEARLY) {
            cal.add(Calendar.MONTH, 6 * value);
        } else if (interval == TOTALYEARLY) {
            cal.add(Calendar.YEAR, value);
        }

        return new Timestamp(cal.getTimeInMillis());
    }

    public static String AddTime(String date, int interval, int value) {
        String result = "";
        String format = LONG_DATE;
        Calendar cal = DateUtils.parseDateString(date, format);
        if (cal == null) {
            return result;
        }
        if (interval == HOURLY) {
            cal.add(Calendar.HOUR, value);
        } else if (interval == MINIUTELY) {
            cal.add(Calendar.MINUTE, value);
        } else if (interval == SECONDLY) {
            cal.add(Calendar.SECOND, value);
        }

        result = DateUtils.getDateString(cal, format);
        return result;
    }

    public static String AddDateyear(String date, int interval, int value) // 年计算
    {
        String result = "";
        String format = "yyyy";
        Calendar cal = DateUtils.parseDateString(date, format);
        if (cal == null) {
            return result;
        }
        if (interval == DAILY || interval == HOURLY) {
            cal.add(Calendar.DAY_OF_MONTH, value);
        } else if (interval == WEEKLY || interval == REPORTWEEKLY) {
            cal.add(Calendar.WEEK_OF_YEAR, value);
        } else if (interval == MONTHLY || interval == TEMPMONTHLY) {
            cal.add(Calendar.MONTH, value);
        } else if (interval == BIWEEKLY) {
            if (value > 0) {
                cal.add(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR) % 2);
            } else if (value < 0) {
                cal.add(Calendar.WEEK_OF_YEAR, (-1) * (cal.get(Calendar.WEEK_OF_YEAR) + 1) % 2);
            }
            cal.add(Calendar.WEEK_OF_YEAR, value * 2);
        } else if (interval == YEARLY) {
            cal.add(Calendar.YEAR, value);
        } else if (interval == QUARTLY) {
            cal.add(Calendar.MONTH, 3 * value);
        } else if (interval == BIYEARLY) {
            cal.add(Calendar.MONTH, 6 * value);
        } else if (interval == TOTALYEARLY) {
            cal.add(Calendar.YEAR, value);
        }
        result = DateUtils.getDateString(cal, format);
        return result;
    }

    public static Calendar AddDate(Calendar calendar, int interval, int value) {
        if (calendar == null) return null;
        Calendar result = Calendar.getInstance();
        result.setTime(calendar.getTime());
        if (interval == DAILY || interval == REALTIME || interval == HOURLY) {
            result.add(Calendar.DAY_OF_MONTH, value);
        } else if (interval == WEEKLY || interval == REPORTWEEKLY) {
            result.add(Calendar.WEEK_OF_YEAR, value);
        } else if (interval == MONTHLY || interval == TEMPMONTHLY) {
            result.add(Calendar.MONTH, value);
        } else if (interval == BIWEEKLY) {
            if (value > 0) {
                result.add(Calendar.WEEK_OF_YEAR, result.get(Calendar.WEEK_OF_YEAR) % 2);
            } else if (value < 0) {
                result.add(Calendar.WEEK_OF_YEAR, (-1) * (result.get(Calendar.WEEK_OF_YEAR) + 1) % 2);
            }
            result.add(Calendar.WEEK_OF_YEAR, value * 2);
        } else if (interval == YEARLY) {
            result.add(Calendar.YEAR, value);
        } else if (interval == QUARTLY) {
            result.add(Calendar.MONTH, 3 * value);
        } else if (interval == BIYEARLY) {
            result.add(Calendar.MONTH, 6 * value);
        } else if (interval == TOTALYEARLY) {
            result.add(Calendar.YEAR, value);
        }
        return result;
    }

    public static boolean isNumber(Object obj) {
        boolean result = false;
        try {
            Double.parseDouble(obj.toString());
            result = true;
        } catch (Exception e) {

        }
        return result;
    }

    public static String getDecimalFormat(Object value) {
        String result = null;
        DecimalFormat myformat = new DecimalFormat();
        myformat.applyPattern("##,###.0");
        try {
            result = myformat.format(Float.parseFloat(value.toString()));
        } catch (Exception e) {
            return value == null ? "-" : value.toString();
        }
        if (result != null && result.toString().startsWith(".")) {
            result = "0" + result;
        }
        if (result != null && result.toString().startsWith("-.")) {
            result = result.replaceFirst("-.", "-0.");
        }
        return result;
    }

    public static String getDecimalFormat2(Object value) {
        String result = null;
        DecimalFormat myformat = new DecimalFormat();
        myformat.applyPattern("##,###.00");
        try {
            result = myformat.format(Float.parseFloat(value.toString()));
        } catch (Exception e) {
            return value == null ? "-" : value.toString();
        }
        if (result != null && result.toString().startsWith(".")) {
            result = "0" + result;
        }
        if (result != null && result.toString().startsWith("-.")) {
            result = result.replaceFirst("-.", "-0.");
        }
        return result;
    }

    public static boolean BothZero(Object lastValue, Object currentValue) {
        boolean result = false;
        try {
            float zero = 0.00000000000001f;
            result = (Math.abs(Float.parseFloat(lastValue.toString())) <= zero)
                     && (Math.abs(Float.parseFloat(currentValue.toString())) <= zero);
        } catch (Exception e) {

        }
        return result;
    }

    public static String getFloatFormat(Object value) {
        String result = null;
        DecimalFormat myformat = new DecimalFormat();
        myformat.applyPattern("###.0");
        try {
            result = myformat.format(value);
        } catch (Exception e) {
            return value == null ? null : value.toString();
        }
        return result;
    }

    public static void showList(List list) {
        try {
            for (int i = 0; i < list.size(); i++) {
                // System.out.print("," + list.get(i));
            }
            // System.out.println("");
        } catch (Exception e) {

        }
    }

    public static String[][] VectorArray2StringArray(Vector[] datas) {
        String[][] result = null;
        try {
            result = new String[datas.length][datas[0].size()];
            for (int i = 0; i < datas.length; i++) {
                result[i] = (String[]) datas[i].toArray(new String[0]);
            }
        } catch (Exception e) {

        }
        return result;
    }

    public static List getDates(String date, String interval) {
        String format = "yyyy-MM-dd";
        List result = new ArrayList(0);
        Calendar cal = parseDateString(date, "yyyy-MM-dd");
        if (cal == null || !validateInteger(interval)) {
            return result;
        }
        try {
            String startdate = getDateString(getStartTime(cal, Integer.parseInt(interval)), format);
            String enddate = getDateString(getEndTime(cal, Integer.parseInt(interval)), format);
            while (!startdate.trim().equalsIgnoreCase(enddate)) {
                result.add(startdate);
                startdate = AddDate(startdate, DAILY, 1);
            }
        } catch (Exception e) {

        }

        return result;
    }

    public static String removeEmptyString(String str) {
        String result = str;
        if (str == null) return result;
        result = str.trim();
        while (str.startsWith(" ")) {
            str = str.substring(1);
        }
        return result;
    }

    public static Calendar getNowCalendar() {
        return Calendar.getInstance();
    }

    /**
     * @param startDateStr 开始时间
     * @param nowDateStr 当前时间
     * @param endDateStr 结束时间
     * @return 0 合法时间段
     * @return 1 未开始
     * @return -1 已结束
     */
    public static int checkBetweenDate(Date startDate, Date nowDate, Date endDate) {
        if (nowDate.getTime() >= startDate.getTime() && nowDate.getTime() <= endDate.getTime()) {
            return 0;
        } else if (nowDate.getTime() < startDate.getTime()) {
            return 1;
        } else {
            return -1;
        }
    }

    public static boolean isSameMonth(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameMonth(cal1, cal2);
    }

    public static boolean isSameMonth(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH));
    }

    public static Date getFirstDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getFirstTimeOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static int getMonthOffset(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        int year1 = cal1.get(Calendar.YEAR);
        int month1 = cal1.get(Calendar.MONTH);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int year2 = cal2.get(Calendar.YEAR);
        int month2 = cal2.get(Calendar.MONTH);
        if (year1 > year2) {
            return (year1 - year2 - 1) * 12 + (month1 + 1) + 11 - month2;
        } else if (year1 == year2) {
            return Math.abs(month1 - month2);
        } else {
            return (year2 - year1 - 1) * 12 + (month2 + 1) + 11 - month1;
        }
    }

    public static void main(String[] args) {
        Date date1 = parseDate("2019-01-01 00:00:00");
        Date date2 = parseDate("2019-06-01 00:00:00");
        Date date3 = parseDate("2018-07-01 00:00:00");
        Date date4 = parseDate("2017-07-01 00:00:00");
        Date date5 = parseDate("2020-07-01 00:00:00");
        Date date6 = parseDate("2021-07-01 00:00:00");
        System.out.println(getDayOffset(date1, date2));
        System.out.println(getDayOffset(date1, date3));
        System.out.println(getDayOffset(date1, date4));
        System.out.println(getDayOffset(date1, date5));
        System.out.println(getDayOffset(date1, date6));
    }

    public static int getDayOffset(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        int year1 = cal1.get(Calendar.YEAR);
        int day1 = cal1.get(Calendar.DAY_OF_YEAR);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int year2 = cal2.get(Calendar.YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);
        if (year1 > year2) {
            return (year1 - year2 - 1) * 365 + day1 + 365 - day2;
        } else if (year1 == year2) {
            return Math.abs(day1 - day2);
        } else {
            return (year2 - year1 - 1) * 365 + day2 + 365 - day1;
        }
    }

    /**
     * 后N天的时间
     *
     * @param day
     * @return
     */
    public static Date getAfterTime(int day) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, day);
        Date date = c.getTime();
        return date;
    }

}
