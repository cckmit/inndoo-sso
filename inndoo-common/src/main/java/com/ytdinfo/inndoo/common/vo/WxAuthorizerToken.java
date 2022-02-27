package com.ytdinfo.inndoo.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Timmy
 */
@Data
public class WxAuthorizerToken implements Serializable{

    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "唯一标识")
    private String id = String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId());
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
    private String authorizerAppid;
    @ApiModelProperty(value = "微信公众号授权AccessToken")
    private String authorizerAccessToken;
    @ApiModelProperty(value = "微信公众号授权RefreshToken")
    private String authorizerRefreshToken;

}