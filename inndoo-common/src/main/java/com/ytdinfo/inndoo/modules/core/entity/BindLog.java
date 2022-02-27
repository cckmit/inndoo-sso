package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author zhuzheng
 */
@Data
@Entity
@Table(name = "t_bind_log")
@TableName("t_bind_log")
@ApiModel(value = "账户绑定日志")
@SQLDelete(sql = "update t_bind_log set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class BindLog extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动平台accountId")
    @Column(length = 19, nullable = false)
    private String actAccountId;

    @ApiModelProperty(value = "小核心AccountId")
    @Column(length = 19, nullable = false)
    private String coreAccountId;

    @ApiModelProperty(value = "0解绑，1绑定")
    @Column(nullable = false)
    private Boolean isBind;

    @ApiModelProperty(value = "注册表单Id")
    @Column(length = 19)
    private String formId;

    @ApiModelProperty(value = "加密手机号")
    @Column(length = 32)
    private String phone;

}