package com.ytdinfo.inndoo.common.utils;

/**
 * @author zhuzheng
 */
public class AjaxResultUtil {

    public static <T> AjaxResult<T> success(String msg) {
        return success(msg, null);
    }

    public static <T> AjaxResult<T> success(String msg, T data) {
        return ajaxResult(true, "0000", msg, data);
    }

    public static <T> AjaxResult<T> fail(String msg) {
        return fail(null, msg, null);
    }

    public static <T> AjaxResult<T> fail(String code, String msg) {
        return fail(code, msg, null);
    }

    public static <T> AjaxResult<T> fail(String code, String msg, T data) {
        return ajaxResult(false, code, msg, data);
    }

    private static <T> AjaxResult<T> ajaxResult(boolean success, String code, String msg, T data) {
        AjaxResult<T> ajaxResult = new AjaxResult<T>();
        ajaxResult.setSuccess(success);
        ajaxResult.setCode(code);
        ajaxResult.setMsg(msg);
        ajaxResult.setData(data);
        return ajaxResult;
    }

    public static <T> AjaxResult<T> result(ResponseCode responseCode) {
        return result(responseCode, null);
    }

    public static <T> AjaxResult<T> result(ResponseCode responseCode, T data) {
        return ajaxResult(responseCode, data);
    }

    private static <T> AjaxResult<T> ajaxResult(ResponseCode responseCode, T data) {
        AjaxResult<T> ajaxResult = new AjaxResult<T>();
        ajaxResult.setSuccess(responseCode.getCode().equals(ResponseCode.SUCCESS.getCode()));
        ajaxResult.setCode(responseCode.getCode());
        ajaxResult.setMsg(responseCode.getDescribe());
        ajaxResult.setData(data);
        return ajaxResult;
    }
}
