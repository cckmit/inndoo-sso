package com.ytdinfo.inndoo.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Timmy
 */
@Data
public class WxopenComponentInfo implements Serializable{

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "唯一标识")
    private String id;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @ApiModelProperty(value = "删除标志 默认0")
    private Boolean isDeleted;
    @ApiModelProperty(value = "第三方平台APPID")
    private String componentAppid;

    @ApiModelProperty(value = "第三方平台名称")
    private String componentName;

    @ApiModelProperty(value = "第三方平台Secret")
    private String componentSecret;

    @ApiModelProperty(value = "消息校验Token")
    private String token;

    @ApiModelProperty(value = "消息加解密Key")
    private String aeskey;

    @ApiModelProperty(value = "授权发起页域名")
    private String domain;

    @ApiModelProperty(value = "开放平台ticket，由腾讯每10分钟推送一次")
    private String ticket;
}