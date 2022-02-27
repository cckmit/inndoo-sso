package com.ytdinfo.inndoo.common.utils;

/**
 * @author zhuzheng
 * 请求返回码
 */
public enum ResponseCode {

    /**
     * 返回码，说明
     * 0000：仅此返回码代表成功，其余均代表失败
     * 0001-0999：为自定义级别返回码
     * 1000-1999：为系统级别返回码
     * 2000-2999：为应用级别返回码
     */
    SUCCESS(
            "0000",
            "成功"
    ),
    INVALID_SYSTEM_PARAMETER(
            "1000",
            "无效系统参数"
    ),
    ILLEGAL_REQUEST(
            "2001",
            "非法请求"
    ),
    INVALID_APP_ID(
            "2002",
            "无效app_id"
    ),
    INVALID_USER(
            "2003",
            "无效用户"
    ),
    INVALID_ACTIVITY(
            "2004",
            "无效活动"
    ),
    ILLEGAL_PARAMETER(
            "2005",
            "非法参数"
    ),
    DATA_NOT_FOUND(
            "2006",
            "未找到相应的数据"
    );

    /**
     * 返回码
     */
    private final String code;

    /**
     * 描述
     */
    private final String describe;

    public String getCode() {
        return code;
    }

    public String getDescribe() {
        return describe;
    }

    ResponseCode(String code, String describe) {
        this.code = code;
        this.describe = describe;
    }

    @Override
    public String toString() {
        return this.name() + "[" + this.code + "]";
    }
}
