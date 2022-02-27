package com.ytdinfo.inndoo.modules.base.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zhuzheng
 */
@Data
public class ProxySetting implements Serializable{

    @ApiModelProperty(value = "ip")
    private String ip;

    @ApiModelProperty(value = "port")
    private Integer port;

    @ApiModelProperty(value = "account")
    private String account;

    @ApiModelProperty(value = "password")
    private String password;

    @ApiModelProperty(value = "需要代理的域名")
    private String hosts;

    @ApiModelProperty(value = "password是否改变")
    private Boolean changed;
}
