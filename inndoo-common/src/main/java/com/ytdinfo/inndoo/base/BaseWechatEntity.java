package com.ytdinfo.inndoo.base;

import com.ytdinfo.inndoo.common.context.UserContext;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author Timmy
 */
@Data
@MappedSuperclass
public abstract class BaseWechatEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "微信公众号appid")
    @Column(length = 19,nullable = false)
    private String appid = UserContext.getAppid();
}
