package com.ytdinfo.inndoo.common.utils;

/**
 * @param <T>
 * @author zhuzhneg
 */

public class AjaxResult<T> {

    private boolean success;

    /**
     * 状态码 .
     */
    private String code;

    private String msg;

    /**
     * 数据 .
     */
    private T data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        String toString = "[" + this.code + "]" + this.msg;
        return this.data == null ? toString : toString + ", data: " + this.data.toString();
    }
}
