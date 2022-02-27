package com.ytdinfo.inndoo.common.rabbit;

import lombok.Getter;

/**
 * 消息队列枚举配置
 * Created by macro on 2018/9/14.
 */
@Getter
public enum QueueEnum {
    /*
     * 开放平台账户变更消息通知
     */
    QUEUE_WXOPEN_EVENT_WXOPENCOMPONENTINFO("matrix.wxopencomponentinfo", "core.wxopencomponentinfo", ""),

       /*
     * 数据源管理变更消息通知
     */
    QUEUE_WXOPEN_EVENT_ACTIVITYDATASOURCE("matrix.activitydatasource", "core.activitydatasource", ""),

    /*
     * 租户变更消息通知
     */
    QUEUE_WXOPEN_EVENT_TENANT("matrix.tenant", "core.tenant", ""),
    /*
     * 公众号授权消息通知
     */
    QUEUE_WXOPEN_EVENT_WXAUTHORIZER("matrix.wxAuthorizer", "core.wxAuthorizer", ""),

    /**
     * mq执行异常消息
     */
    QUEUE_MQEXCEPTION_EVENT_MSG("matrix.mqexception.event.msg", "", ""),
    
    /**
     * 异常消息
     */
    QUEUE_EXCEPTIONLOG_EVENT_MSG("matrix.exceptionlog.event.msg", "", ""),

    /**
     * 用户操作消息
     */
    QUEUE_USER_EVENT_MSG("sso.user.event.msg", "core.user.msg", ""),

    /**
     * 发送短信消息
     */
    QUEUE_SEND_SMS_EVENT_MSG("core.send.sms.event.msg", "core.send.sms.msg", ""),

    /**
     *  达标用户导入后推送到act用户
     */
    QUEUE_ACHIEVELISTRECORD_PUSHACT_MSG("core.achievelistrecord.pushact.event.msg", "core.achievelistrecord.pushact.event.msg", ""),

    /**
     *  达标用户导入后推送到act用户,单个推送
     */
    QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG("core.achievelistrecord.pushact.single.event.msg", "core.achievelistrecord.pushact.single.event.msg", ""),

    QUEUE_JSCCB_API_SYNC("core.jsccb.api.sync.event.msg", "core.jsccb.api.sync.event.msg", ""),

    QUEUE_JSCCB_API_SYNC_ACTIVATE_MSG("core.jsccb.api.sync.activate.event.msg", "core.jsccb.api.sync.activate.event.msg", ""),

    QUEUE_JSCCB_API_SYNC_STATISTIC_MSG("core.jsccb.api.sync.statistic.event.msg", "core.jsccb.api.sync.statistic.event.msg", ""),

    QUEUE_JSCCB_API_SYNC_OLDDATA_MSG("core.jsccb.api.sync.oldData.event.msg", "core.jsccb.api.sync.oldData.event.msg", ""),

    /**
     *  达标用户导入后推送到act用户
     */
    QUEUE_ACCOUNT_INPUT_MSG("core.account.input.pushact.event.msg", "core.account.input.event.msg", ""),

    QUEUE_API_DATA_MSG("core.api.data.event.msg", "core.api.data.event.msg", ""),

    /**
     * 手机号归属地
     */
    QUEUE_PHONE_LOCATION_EVENT_MSG("core.phone.location", "core.phone.location", ""),

    /**
     *  模拟员工注册
     */
    QUEUE_SIMULATION_STAFF_REGISTRATION("core.simulation.staff.registration.event.msg", "core.simulation.staff.registration.event.msg", ""),

    /**
     *  记录解密账户信息
     */
    QUEUE_DECRYPTACCOUNT_MSG("core.decryptAccount.event.msg", "core.decryptAccount.event.msg", ""),

    /**
     * 数据平台的标签用户信息
     */
    QUEUE_PUSH_TAG_MSG("core.push.tag.event.msg", "core.push.tag.msg", ""),

    /**
     * 清除api调用日志
     */
    QUEUE_CLEAR_API_LOG_MSG("core.clear.api.log","core.clear.api.log","");

    /**
     * 交换名称
     */
    private String exchange;
    /**
     * 队列名称
     */
    private String name;
    /**
     * 路由键
     */
    private String routeKey;

    QueueEnum(String exchange, String name, String routeKey) {
        this.exchange = exchange;
        this.name = name;
        this.routeKey = routeKey;
    }
}
