package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_account_form_field")
@TableName("t_account_form_field")
@ApiModel(value = "会员注册扩展表单内容")
@SQLDelete(sql = "update t_account_form_field set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class AccountFormField extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "会员表单Id")
    @Column(length = 19, nullable = false)
    private String formId;

    @ApiModelProperty(value = "会员账户Id")
    @Column(length = 19, nullable = false)
    private String accountId;

    @ApiModelProperty(value = "表单属性数据Id")
    @Column(length = 19, nullable = false)
    private String metaId;

    @ApiModelProperty(value = "表单控件类型，如input/select/textarea等")
    @Column(length = 20, nullable = false)
    private String metaType;

    @ApiModelProperty(value = "表单属性标题")
    @Column(length = 50, nullable = false)
    private String metaTitle;

    @ApiModelProperty(value = "表单属性简短值")
    @Column(length = 600, nullable = false)
    private String fieldShortData;

    @ApiModelProperty(value = "表单属性值")
    @Column(columnDefinition = "text", nullable = false)
    private String fieldData;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "拓展属性值")
    private String  formName;

}