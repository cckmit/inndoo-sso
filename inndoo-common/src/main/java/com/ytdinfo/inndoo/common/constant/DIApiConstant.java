package com.ytdinfo.inndoo.common.constant;

/**
 * Created by nolan on 2020/8/11.
 * 数据平台接口地址
 */
public interface DIApiConstant {

    String DI_TOKEN_USER_KEY = "admin@admin.cn";
    String DI_SECRET_KEY = "Yu%q^t4Rad";
    Integer DI_TOKEN_EXPIRES_IN = 1000;
    /**
     * token在redis中的key
     */
    String DI_COMPONENT_TOKEN_KEY = "di:tag:token:key:";

    /**
     * 获取token
     */
    String DI_COMPONENT_TOKEN = "openapi/commonapi/v2/accessToken";

    /**
     * 获取标签列表
     */
    String DI_TAG_LIST = "/di/portrait/tag/list/info";

    /**
     * 获取标签的枚举值
     */
    String DI_TAG_VALUE_ENUM = "/di/portrait/tag/value/enum";

    /**
     * 获取缓存中标签的值
     */
    String DI_TAG_USER_VALUE = "/di/portrait/tag/value/get";

    /**
     * 设置标签的失效时间
     */
    String DI_TAG_EXPIRE_SET = "/di/portrait/tag/expire/set";

    /**
     * 更新标签缓存
     */
    String DI_TAG_CACHE_REFRESH = "/di/portrait/tag/cache/refresh";

    /**
     * 获取缓存更新情况
     */
    String DI_TAG_CACHE_PROGRESS = "/di/portrait/tag/cache/progress";

}
