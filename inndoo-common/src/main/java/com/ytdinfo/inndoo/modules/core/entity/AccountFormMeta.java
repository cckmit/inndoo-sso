package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseEntity;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_account_form_meta")
@TableName("t_account_form_meta")
@ApiModel(value = "动态表单控件配置信息")
@SQLDelete(sql = "update t_account_form_meta set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class AccountFormMeta extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "标题")
    @Column(length = 50, nullable = false)
    private String title;

    @ApiModelProperty(value = "是否必填")
    @Column(length = 1, nullable = false)
    private Boolean isRequired;

    @ApiModelProperty(value = "必填提示信息")
    @Column(length = 200, nullable = false)
    private String requiredNotice;

    @ApiModelProperty(value = "填写的提示信息")
    @Column(length = 200, nullable = false)
    private String placeHolder;

    @ApiModelProperty(value = "填写文字最小长度")
    @Column(nullable = false)
    private Integer minLength;

    @ApiModelProperty(value = "填写文字最大长度")
    @Column(nullable = false)
    private Integer maxLength;

    @ApiModelProperty(value = "表单字段说明信息")
    @Column(length = 500, nullable = false)
    private String metaDesc;

    @ApiModelProperty(value = "自定义注册表单Id")
    @Column(length = 19, nullable = false)
    private String accountFormId;

    @ApiModelProperty(value = "表单类型，如input/select/textarea/phone/idcard/sex/等")
    @Column(length = 20, nullable = false)
    private String metaType;

    @ApiModelProperty(value = "属性设定信息，json字符保存")
    @Column(columnDefinition = "text", nullable = false)
    private String metaData;

    @ApiModelProperty(value = "是否系统内置标准控件")
    @Column(nullable = false)
    private Boolean isStandard;

    @ApiModelProperty(value = "排序值")
    @Column(nullable = false)
    private Integer sortOrder;

    @ApiModelProperty(value = "是否是用户身份识别主键")
    @Column(nullable = false)
    private Boolean isIdentifier = Boolean.FALSE;
}