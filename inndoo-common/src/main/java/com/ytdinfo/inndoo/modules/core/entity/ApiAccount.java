package com.ytdinfo.inndoo.modules.core.entity;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import javax.persistence.Column;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_api_account")
@TableName("t_api_account")
@ApiModel(value = "API用户帐号管理")
@SQLDelete(sql = "update t_api_account set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ApiAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "微信开放平台appid")
    @Column(length = 19, nullable = false)
    private String componentAppid = "";

    @ApiModelProperty(value = "微信公众号appid")
    @Column(length = 19, nullable = false)
    private String appid = "";

    @ApiModelProperty(value = "帐号名称")
    @Column(length = 20, nullable = false)
    private String accountName;

    @ApiModelProperty(value = "appkey")
    @Column(length = 32, nullable = false)
    private String appkey;

    @ApiModelProperty(value = "appSecret")
    @Column(length = 32, nullable = false)
    private String appSecret;

    @ApiModelProperty(value = "IP地址白名单")
    @Column(columnDefinition = "text",nullable = false)
    private String ipWhiteList;

    @ApiModelProperty(value = "状态，0：有效，-1：禁用")
    @Column(nullable = false)
    private Byte status = 0;

}