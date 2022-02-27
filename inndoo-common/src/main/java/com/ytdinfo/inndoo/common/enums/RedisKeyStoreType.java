package com.ytdinfo.inndoo.common.enums;

import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import lombok.Getter;

/**
 * @author timmy
 * @date 2019/11/20
 */
@Getter
public enum RedisKeyStoreType {
    /**
     * 落地页
     */
    userPermission("userPermission:"),
    user("user:"),
    ssoUser("ytd:sso:user:"),
    userRole("userRole:"),
    AccountFormResource("AccountFormResource::"),
    AccountFormMeta_NameList("AccountFormMeta::NameList::"),
    USER_TOKEN(SecurityConstant.USER_TOKEN),
    TOKEN_PRE(SecurityConstant.TOKEN_PRE),
    userMenuList("permission::userMenuList"),
    Area("Area::"),
    roleUserList("roleUserList:");


    /**
     * 缓存key前缀
     */
    private String prefixKey;

    RedisKeyStoreType(String prefixKey) {
        this.prefixKey = prefixKey;
    }


}