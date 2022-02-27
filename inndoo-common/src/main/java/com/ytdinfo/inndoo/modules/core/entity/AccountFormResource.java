package com.ytdinfo.inndoo.modules.core.entity;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
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
@Table(name = "t_account_form_resource")
@TableName("t_account_form_resource")
@ApiModel(value = "注册页面ui资源管理")
@SQLDelete(sql = "update t_account_form_resource set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class AccountFormResource extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "注册页面Id")
    @Column(length = 19, nullable = false)
    private String formId;

    @ApiModelProperty(value = "设定json字段")
    @Column(columnDefinition = "text", nullable = false)
    private String resourceData;

}