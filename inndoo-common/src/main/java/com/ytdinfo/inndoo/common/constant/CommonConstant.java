package com.ytdinfo.inndoo.common.constant;

/**
 * 常量
 * @author Exrickx
 */
public interface CommonConstant {

    /**
     * 用户默认头像
     */
    String USER_DEFAULT_AVATAR = "https://i.loli.net/2019/04/28/5cc5a71a6e3b6.png";

    /**
     * 用户正常状态
     */
    Integer USER_STATUS_NORMAL = 0;

    /**
     * 用户禁用状态
     */
    Integer USER_STATUS_LOCK = -1;

    /**
     * 普通用户
     */
    Integer USER_TYPE_NORMAL = 0;

    /**
     * 管理员
     */
    Integer USER_TYPE_ADMIN = 1;

    /**
     * 全部数据权限
     */
    Integer DATA_TYPE_ALL = 0;

    /**
     * 自定义数据权限
     */
    Integer DATA_TYPE_CUSTOM = 1;

    /**
     * 正常状态
     */
    Integer STATUS_NORMAL = 0;

    /**
     * 禁用状态
     */
    Integer STATUS_DISABLE = -1;

    /**
     * 已发布状态
     */
    Integer STATUS_APPROVED = 1;

    /**
     * 限流标识
     */
    String LIMIT_ALL = "INNDOO_LIMIT_ALL";

    /**
     * 顶部菜单类型权限
     */
    Integer PERMISSION_NAV = -1;

    /**
     * 页面类型权限
     */
    Integer PERMISSION_PAGE = 0;

    /**
     * 操作类型权限
     */
    Integer PERMISSION_OPERATION = 1;

    /**
     * 1级菜单父id
     */
    String PARENT_ID = "0";

    /**
     * 0级菜单
     */
    Integer LEVEL_ZERO = 0;

    /**
     * 1级菜单
     */
    Integer LEVEL_ONE = 1;

    /**
     * 2级菜单
     */
    Integer LEVEL_TWO = 2;

    /**
     * 3级菜单
     */
    Integer LEVEL_THREE = 3;

    /**
     * 消息发送范围
     */
    Integer MESSAGE_RANGE_ALL = 0;

    /**
     * 未读
     */
    Integer MESSAGE_STATUS_UNREAD = 0;

    /**
     * 已读
     */
    Integer MESSAGE_STATUS_READ = 1;

    /**
     * github登录
     */
    Integer SOCIAL_TYPE_GITHUB = 0;

    /**
     * qq登录
     */
    Integer SOCIAL_TYPE_QQ = 1;

    /**
     * 微博登录
     */
    Integer SOCIAL_TYPE_WEIBO = 2;

    /**
     * 短信验证码key前缀
     */
    String PRE_SMS = "INNDOO_PRE_SMS:";

    /**
     * 邮件验证码key前缀
     */
    String PRE_EMAIL = "INNDOO_PRE_EMAIL:";

    /**
     * 本地文件存储
     */
    Integer OSS_LOCAL = 0;

    /**
     * 七牛云OSS存储
     */
    Integer OSS_QINIU = 1;

    /**
     * 阿里云OSS存储
     */
    Integer OSS_ALI = 2;

    /**
     * 腾讯云COS存储
     */
    Integer OSS_TENCENT = 3;

    /**
     * 部门负责人类型 主负责人
     */
    Integer HEADER_TYPE_MAIN = 0;

    /**
     * 部门负责人类型 副负责人
     */
    Integer HEADER_TYPE_VICE = 1;

    /**
     * 一年的秒数
     */
    int SECOND_1YEAR = 31536000;
    /**
     * 一个月的秒数
     */
    int SECOND_1MONTH = 2592000;
    /**
     * 一周的秒数
     */
    int SECOND_1WEEK = 604800;
    /**
     * 一天的秒数
     */
    int SECOND_1DAY = 86400;
    /**
     * 8小时秒数
     */
    int SECOND_8HOUR = 28800;
    /**
     * 1小时秒数
     */
    int SECOND_1HOUR = 3600;
    /**
     * 十分钟秒数
     */
    int SECOND_10MUNITE = 600;
    /**
     * 一分钟秒数
     */
    int SECOND_1MUNITE = 60;

    /**
     * 会员类型：微信
     */
    String ACCOUNT_TYPE_WEIXIN = "weixin";

    /**
     * 会员类型：手机银行
     */
    String ACCOUNT_TYPE_BANKAPP = "bankapp";

    /**
     * 会员类型：微博
     */
    String ACCOUNT_TYPE_WEIBO = "weibo";

    /**
     * 返回结果Y
     */
    String RESULT_YES = "Y";

    /**
     * 返回结果N
     */
    String RESULT_NO = "N";

    /**
     * 标准日期格式化
     */
    String FORMAT_DATE = "yyyy-MM-dd";
    String UPLOAD_WHITELIST = ".gif;.png;.bmp;.jpg;.jpeg;.mp3;.wma;.flv;.mp4;.wmv;.ogg;.avi;.csv;.doc;.docx;.xls;.xlsx;.ppt;.pptx;.pdf;.sketch;.psd";
}
