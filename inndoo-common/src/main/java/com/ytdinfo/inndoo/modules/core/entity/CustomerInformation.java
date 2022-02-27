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
 * @author yaochangning
 */
@Data
@Entity
@Table(name = "t_customer_information")
@TableName("t_customer_information")
@ApiModel(value = "客户信息表")
@SQLDelete(sql = "update t_customer_information set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class CustomerInformation  extends BaseWechatEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "客户唯一标识符，如手机号、身份证号码、银行卡号或客户号，由系统租户级别定义，加密存放")
    @Column(length = 64, nullable = false)
    private String identifier;

    @ApiModelProperty(value = "客户手机号")
    @Column(length = 32, nullable = false)
    private String phone = StrUtil.EMPTY;

    @ApiModelProperty(value = "客户号")
    @Column(length = 32, nullable = false)
    private String customerNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "姓名")
    @Column(length = 32, nullable = false)
    private String name = StrUtil.EMPTY;

    @ApiModelProperty(value = "身份证号码")
    @Column(length = 64, nullable = false)
    private String idcardNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "银行卡号")
    @Column(length = 64, nullable = false)
    private String bankcardNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "出生日期,2008-12-31")
    @Column(length = 40, nullable = false)
    private String birthday = StrUtil.EMPTY;

    @ApiModelProperty(value = "邮箱")
    @Column(length = 200, nullable = false)
    private String email = StrUtil.EMPTY;

    @ApiModelProperty(value = "地址")
    @Column(length = 200, nullable = false)
    private String address = StrUtil.EMPTY;

    @ApiModelProperty(value = "分行编码")
    @Column(length = 20, nullable = false)
    private String bankBranchNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "分行名称")
    @Column(length = 100, nullable = false)
    private String bankBranchName = StrUtil.EMPTY;

    @ApiModelProperty(value = "机构编号")
    @Column(length = 20, nullable = false)
    private String institutionalCode = StrUtil.EMPTY;

    @ApiModelProperty(value = "机构名称")
    @Column(length = 100, nullable = false)
    private String institutionalName = StrUtil.EMPTY;

    @ApiModelProperty(value = "客群编码")
    @Column(length = 30, nullable = false)
    private String customerGroupCoding = StrUtil.EMPTY;

    @ApiModelProperty(value = "企业名称")
    @Column(length = 100, nullable = false)
    private String companyName = StrUtil.EMPTY;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "客户信息拓展列表")
    @Column(nullable = false)
    private List<CustomerInformationExtend> customerInformationExtends;


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
    @ApiModelProperty(value = "脱敏银行卡号")
    private String tmbankcardNo;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏分行名称")
    private String tmbankBranchName;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏机构名称")
    private String tminstitutionalName;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏邮箱")
    private String tmemail;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏地址")
    private String tmaddress;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏客户号")
    private String tmcustomerNo;


}
