package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Nolan
 */
@Data
@Entity
@Table(name = "t_sms_captcha_log")
@TableName("t_sms_captcha_log")
@ApiModel(value = "手机短信验证码记录")
@SQLDelete(sql = "update t_sms_captcha_log set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class SmsCaptchaLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "手机号码")
    @Column(length = 32, nullable = false)
    private String phone;

    @ApiModelProperty(value = "验证码")
    @Column(length = 10)
    private String code;

    @ApiModelProperty(value = "appid")
    @Column(length = 25, nullable = false)
    private String appid;

    @ApiModelProperty(value = "租户id")
    @Column(length = 25, nullable = false)
    private String tenantId;

    @ApiModelProperty(value = "验证码发送状态")
    @Column(length = 1, nullable = false)
    private Integer sendStatus;

    @ApiModelProperty(value = "验证码发送失败原因")
    @Column
    private String reason;

}