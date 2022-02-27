package com.ytdinfo.inndoo.modules.core.entity;

import cn.hutool.core.util.StrUtil;
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
import java.io.Serializable;
import java.util.List;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_account")
@TableName("t_account")
@ApiModel(value = "会员账户")
@SQLDelete(sql = "update t_account set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class Account extends BaseWechatEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "客户唯一标识符，如手机号、身份证号码、银行卡号或客户号，由系统租户级别定义，加密存放")
    @Column(length = 64, nullable = false)
    private String identifier;
    @ApiModelProperty(value = "手机号")
    @Column(length = 32, nullable = false)
    private String phone;
    @ApiModelProperty(value = "md5手机号")
    @Column(length = 32, nullable = false)
    private String md5Phone;
    @ApiModelProperty(value = "身份证号码")
    @Column(length = 64, nullable = false)
    private String idcardNo;
    @ApiModelProperty(value = "银行卡号")
    @Column(length = 64, nullable = false)
    private String bankcardNo;
    @ApiModelProperty(value = "客户号")
    @Column(length = 64, nullable = false)
    private String customerNo;
    @ApiModelProperty(value = "出生日期,2008-12-31")
    @Column(length = 40, nullable = false)
    private String birthday;
    @ApiModelProperty(value = "姓名")
    @Column(length = 64, nullable = false)
    private String name;
    @ApiModelProperty(value = "性别")
    @Column(length = 45)
    private String sex;
    @ApiModelProperty(value = "身份证照片地址，正反面用逗号隔开")
    @Column(length = 400)
    private String idcardPhoto;
    @ApiModelProperty(value = "邮箱")
    @Column(length = 200)
    private String email;
    @ApiModelProperty(value = "地址")
    @Column(length = 200)
    private String address;
    @ApiModelProperty(value = "车牌号")
    @Column(length = 32, nullable = false)
    private String licensePlateNo;

    @ApiModelProperty(value = "是否是员工，0：普通用户，1：员工")
    @Column(length = 2, nullable = false)
    private Integer isStaff;

    @ApiModelProperty(value = "员工号")
    @Column(length = 40, nullable = false)
    private String staffNo;

    @ApiModelProperty(value = "部门编码")
    @Column(length = 100, nullable = false)
    private String deptNo;

    @ApiModelProperty(value = "是否勾选注册协议")
    @Column(nullable = false)
    private Boolean isAgreement;

    @ApiModelProperty(value = "客户唯一标识符，如手机号、身份证号码、银行卡号或客户号，由系统租户级别定义，md5加密存放")
    @Column(length = 300, nullable = false)
    private String md5identifier = StrUtil.EMPTY;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "绑定的活动平台的账户id,用来给前端查询接收数据使用")
    private String actAccountId;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "拓展属性值")
    private List<AccountFormField> accountFormFields;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "拓展属性查询值")
    private String accountFormFieldValue;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏手机号")
    private String tmphone;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏姓名")
    private String tmname;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏身份证号码")
    private String tmidcardNo;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏员工号")
    private String tmstaffNo;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏银行卡号")
    private String tmbankcardNo;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏地址")
    private String tmaddress;


    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏客户号")
    private String tmcustomerNo;


}