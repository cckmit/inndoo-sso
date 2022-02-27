package com.ytdinfo.inndoo.common.constant;

/**
 * Created by timmy on 2019/8/12.
 */
public interface ApiConstant {
    /**
     * 根据id获取微信开放平台信息
     */
    String MATRIX_COMPONENT_QUERY = "/wxopen/component/query/{}";
    /**
     * 微信开放平台设置清单
     */
    String MATRIX_COMPONENT_LIST = "/wxopen/component/list";
    /**
     * 授权微信公众号清单
     */
    String MATRIX_WXAUTHORIZER_LIST = "/wxopen/wxauthorizer/list";

    /**
     * 根据租户获取授权微信公众号清单
     */
    String MATRIX_WXATHORIZER_LIST_BYTENANT = "/wxopen/wxauthorizer/list/{}";
    /**
     * 根据租户获取授权微信公众号清单
     */
    String MATRIX_WXATHORIZER_LIST_BYTENANTANDMOBILE = "/wxopen/wxauthorizer/list/mobile/{}";
    /**
     * 更新微信公众号token
     */
    String MATRIX_UPDATE_WXAUTHORIZERTOKEN = "/wxopen/wxauthorizertoken";

    /**
     * 获取微信公众号token
     */
    String MATRIX_GET_WXAUTHORIZERTOKEN = "/wxopen/wxauthorizertoken/{}";

    /**
     * 获取授权微信公众号信息
     */
    String MATRIX_WXATHORIZER_QUERY = "/wxopen/wxauthorizer/query/{}";

    /**
     * 获取数据源清单
     */
    String MATRIX_ACTIVITYDATASOURCE_LIST = "/activitydatasource/list";

    /**
     * 获取租户清单
     */
    String MATRIX_TENANT_LIST = "/tenant/list";

    /**
     * 根据id获取租户信息
     */
    String MATRIX_TENANT_QUERY = "/tenant/query/{}";

    /**
     * 根据userid获取授权租户清单
     */
    String MATRIX_TENANT_USERLIST = "/tenant/userlist/{}";

    /**
     * 根据id获取租户信息
     */
    String MATRIX_ACTIVITYDATASOURCE_QUERY = "/activitydatasource/query/{}";

    /**
     * 获取WorkerId
     */
    String MATRIX_WORKERID_QUERY = "/snowflake/query/{}";

    /**
     * 接收小核心达标名单时间接口
     */
    String ACTIVITY_ACHIEVELISTRECEIVE_RECEIVE = "/achieveListReceive/receive";

    /**
     * 活动平台绑定小核心账户
     */
    String ACTIVITY_ACCOUNT_BIND = "/account/bind";

    /**
     * 活动平台批量解绑小核心账户
     */
    String ACTIVITY_ACCOUNT_UNTIED = "/account/untied/{}";

    /**
     * 活动平台批量解绑小核心账户 根据活动平台accountId解绑
     */
    String ACTIVITY_ACCOUNT_UNTIEDACT = "/account/untiedAct/{}";

    String ACTIVITY_GETCOREACCOUNTID = "/account/getCoreAccountId/{}";

    /**
     * 活动平台记录短信/归属地校验接口费用接口
     */
    String ACTIVITY_APICOST_NOTE = "/apicost/note";

    /**
     * 1.0数据注册数据迁移到2.0注册
     */
    String ACTIVITY_DATAMIGRATION = "/account/dataMigration";

    /**
     * 通知活动平台 部门信息变更
     */
    String ACTIVITY_DEPARTMENT_MODIFY = "/department/modify";

    /**
     * 通知活动平台 员工信息变更
     */
    String ACTIVITY_STAFF_MODIFY = "/staff/modify";


    /**
     * 获取小核心账户
     */
    String ACTIVITY_FIND_CORE_ACCOUNT_ID = "/account/getCoreId";

    /**
     * 根据名称查询表单
     */
    String CORE_QUERYBYNAME = "/accountform/queryByName";

    /**
     * 获取身份识别表单
     */
    String CORE_GETIDENTIFIERFORM = "/accountform/getIdentifierForm";

    /**
     * 保存注册账户
     */
    String CORE_ACCOUNT_SAVEANDUPDATE = "/account/saveAndUpdate";

    /**
     * 根据活动账号id获取小核心账号
     */
    String CORE_ACCOUNT_GETCOREACCOUNTBYACTACCOUNTID = "/account/getCoreAccountByActAccountId";

    /**
     * 解绑账户untied
     */
    String CORE_ACCOUNT_UNTIED = "/account/untied";

    /**
     * act账户和小核心账户绑定
     */
    String CORE_ACCOUNT_BINDACCOUNT = "/account/bindAccount";
    /**
     * 获取活动平台账户
     */
    String ACTIVITY_ACCOUNT_GETBYACTACCOUNTID = "/account/getByActAccountId";

    /**
     * 通过名称获取租户员工角色可添加员工上线人数限制
     */
    String MATRIX_TENANTSTAFFROLELIMIT_GETTENANTSTAFFROLELIMITBYROLENAME = "/tenantstaffrolelimit/getTenantStaffRoleLimitByRoleName";

    /**
     * 判断校验名单是否被占用
     */
    String ACTIVITY_COMMON_CHECKEMPLOYLIST = "/common/checkEmployList";

    /**
     * 获取用户uuid(上海中行)
     */
    String ACTIVITY_SHZH_GETUUID = "/shzh/getUuid";
    /**
     * 根据mobile获取授权租户清单
     */
    String MATRIX_TENANT_USERLIST_MOBILE = "/tenant/userlistByMobile/{}";
    /**
     * 根据mobile获取授权租户清单
     */
    String MATRIX_TENANT_MOBILELIST = "/usertenant/listByMobile/{}";
}
