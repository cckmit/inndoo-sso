package com.ytdinfo.inndoo.modules.base.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;

@Data
public class DynamicApiDto {

    String tenantId;

    String appId;

    String actAccountId;

    String accountId;

    String openId;

    @ApiModelProperty(value = "会员类型，1:微信用户，2:小程序用户，3:支付宝小程序用户，4：手机银行用户 5 企业微信用户,10:浏览器")
    private Byte accountType;

    private DynamicApiDto() {

    }

    public DynamicApiDto(String tenantId, String appId, String actAccountId, String accountId, String openId,Byte accountType) {
        this.tenantId = tenantId;
        this.appId = appId;
        this.actAccountId = actAccountId;
        this.accountId = accountId;
        this.openId = openId;
        this.accountType = accountType;
    }

}
