package com.ytdinfo.inndoo.common.constant;

/**
 * @author Exrickx
 */
public interface SecurityConstant {

    /**
     * token分割
     */
    String TOKEN_SPLIT = "Bearer ";

    /**
     * JWT签名加密key
     */
    String JWT_SIGN_KEY = "inndoo";

    /**
     * token参数头
     */
    String HEADER = "accessToken";

    /**
     * 权限参数头
     */
    String AUTHORITIES = "authorities";

    /**
     * 用户选择JWT保存时间参数头
     */
    String SAVE_LOGIN = "saveLogin";

    /**
     * github保存state前缀key
     */
    String GITHUB_STATE = "INNDOO_GITHUB:";

    /**
     * qq保存state前缀key
     */
    String QQ_STATE = "INNDOO_QQ:";

    /**
     * qq保存state前缀key
     */
    String WEIBO_STATE = "INNDOO_WEIBO:";

    /**
     * 交互token前缀key
     */
    String TOKEN_PRE = "INNDOO_TOKEN_PRE:";

    /**
     * 用户token前缀key 单点登录使用
     */
    String USER_TOKEN = "INNDOO_USER_TOKEN:";

    /**
     * tenantId参数
     */
    String TENANT_ID = "tenantId";

    /**
     * wxappid参数
     */
    String WXAPPID = "wxappid";
    /**
     * 缓存有效期 秒
     * @author qinbaolei 2022/2/24 10:46
     **/
    Integer EXPIRE_TIME=604800;
}
