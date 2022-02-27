package com.ytdinfo.inndoo.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Timmy
 */
@Data
public class Tenant implements Serializable {
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
    @ApiModelProperty(value = "租户名称")
    private String name;
    @ApiModelProperty(value = "企业全称")
    private String companyName;
    @ApiModelProperty(value = "企业通讯地址")
    private String companyAddress;
    @ApiModelProperty(value = "企业所在行业")
    private String companyIndustry;
    @ApiModelProperty(value = "企业规模")
    private String companySize;
    @ApiModelProperty(value = "联系电话")
    private String companyTel;
    @ApiModelProperty(value = "联系人")
    private String companyContact;
    @ApiModelProperty(value = "联系人邮箱")
    private String companyEmail;
    @ApiModelProperty(value = "备注信息")
    private String remark;
    @ApiModelProperty(value = "审核状态,状态码：0未审核，1审核通过，-1审核未通过")
    private Integer status;
    @ApiModelProperty(value = "是否默认租户")
    private Boolean isDefault;
    //租户类别，0互联网用户，用户级数据隔离，1企业级租户，租户级数据隔离
    @ApiModelProperty(value = "租户类别")
    private Integer tenantType;
    @ApiModelProperty(value = "租户绑定的微信开放平台帐号Id")
    private String wxopenComponentId;
    @ApiModelProperty(value = "租户绑定的微信开放平台帐号AppId")
    private String wxopenComponentAppId;
    @ApiModelProperty(value = "租户数据源Id")
    private String activityDataSourceId;
    @ApiModelProperty(value = "创建活动的限制次数")
    private Integer limitActivitySize ;
}