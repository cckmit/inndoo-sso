package com.ytdinfo.inndoo.modules.core.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ytdinfo.inndoo.base.BaseActivityEntity;
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
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_account_form")
@TableName("t_account_form")
@ApiModel(value = "会员注册页面主信息")
@SQLDelete(sql = "update t_account_form set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class AccountForm extends BaseActivityEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "页面类型 0、页面  1、弹窗")
    @Column(nullable = true)
    private Byte type;

    @ApiModelProperty(value = "注册协议")
    @Column( columnDefinition = "text", nullable = false)
    private String agreement;

    @ApiModelProperty(value = "是否开启注册协议")
    @Column(nullable = false)
    private Boolean enableAgreement = false;

    @ApiModelProperty(value = "是否启用图形验证码")
    @Column(nullable = false)
    private Boolean enableCaptcha = Boolean.TRUE;

    @ApiModelProperty(value = "注册成功后跳转URL")
    @Column(length = 300,nullable = false)
    private String redirectUrl;

    @ApiModelProperty(value = "限制打开的平台，多个平台以逗号隔开，勾选代表仅限该平台打开")
    @Column(length = 100,nullable = false)
    private String platformLimit = "";

    @ApiModelProperty(value = "使用人群，0：员工，1：客户")
    @Column(nullable = false)
    private Integer formType;

    @ApiModelProperty(value = "是否是身份识别表单")
    @Column(nullable = false)
    private Boolean isIdentifierForm = Boolean.FALSE;

    @ApiModelProperty(value = "是否是默认注册页")
    @Column(nullable = false)
    private Boolean isDefault = Boolean.FALSE;

    @ApiModelProperty(value = "员工身份校验属性，用,隔开")
    @Column(nullable = false)
    private String checkStaff = StrUtil.EMPTY;

    @ApiModelProperty(value = "提交成功后的提示语")
    @Column(nullable = true,length = 200)
    private String successTips = StrUtil.EMPTY;

    @ApiModelProperty(value = "是否显示取消按钮")
    @Column(nullable = false)
    private Boolean cancelBtn = Boolean.FALSE ;

    @ApiModelProperty(value = "取消按钮内容")
    @Column(nullable = false)
    private String cancelBtnContent = StrUtil.EMPTY;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "动态表单控件配置信息")
    private List<AccountFormMeta> accountFormMetas;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "注册页面ui资源管理")
    private List<AccountFormResource> accountFormResources;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "传要删除的AccountFormMetas的id。前端使用")
    private List<String> deleteAccountFormMetaIds;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "活动状态，0：未发布，1：进行中 -1：活动未开始 2：活动已过期  3：已下架")
    private Integer actStatus;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "开始时间用于前端接受和显示时间，开始时间当天00:00:00生效，格式yyyy-MM-dd")
    @Column(length = 10, nullable = false)
    private String viewStartDate;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "开始时间用于前端接受和显示时间，结束时间，结束时间当天23:59:59前有效，格式yyyy-MM-dd")
    @Column(length = 10, nullable = false)
    private String viewEndDate;

}