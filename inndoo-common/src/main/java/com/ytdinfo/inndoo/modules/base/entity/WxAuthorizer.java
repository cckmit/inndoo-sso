package com.ytdinfo.inndoo.modules.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ytdinfo.inndoo.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Timmy
 */
@Data
public class WxAuthorizer  implements Serializable {

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

    @ApiModelProperty(value = "微信公众号appid")
    private String appid;

    @ApiModelProperty(value = "微信公众号头像")
    private String headImg;

    @ApiModelProperty(value = "微信公众号名称")
    private String nickName;

    @ApiModelProperty(value = "公众号类型")
    private Integer serviceTypeInfo;

    @ApiModelProperty(value = "认证类型")
    private Integer verifyTypeInfo;

    @ApiModelProperty(value = "公众号的原始ID")
    private String userName;

    @ApiModelProperty(value = "认证主体名称")
    private String principalName;

    @ApiModelProperty(value = "功能开通状态")
    private String businessInfo;

    @ApiModelProperty(value = "权限集清单")
    private String functionInfo;

    @ApiModelProperty(value = "微信公众号账号")
    private String alias;

    @ApiModelProperty(value = "微信公众号二维码")
    private String qrcodeUrl;

    @ApiModelProperty(value = "微信公众号授权状态")
    private Integer status;

    @ApiModelProperty(value = "微信开放平台Appid")
    private String componentAppid;

    @ApiModelProperty(value = "租户Id")
    private String tenantId;

}