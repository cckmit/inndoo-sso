package com.ytdinfo.inndoo.modules.core.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

/**
 * @author Timmy
 *
 */
@Data
@Entity
@Table(name = "t_staff")
@TableName("t_staff")
@ApiModel(value = "员工信息")
@SQLDelete(sql = "delete from  t_staff where id=?")
@Where(clause = "is_deleted = 0")
public class Staff extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "姓名，可为空可重名")
    @Column(length = 100, nullable = false)
    private String name = StrUtil.EMPTY;

    @ApiModelProperty(value = "工号，不可为空，不可重复")
    @Column(length = 20, nullable = false, unique = true)
    private String staffNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "手机号")
    @Column(length = 32, nullable = false)
    private String phone = StrUtil.EMPTY;

    @ApiModelProperty(value = "部门Id")
    @Column(length = 20, nullable = false)
    private String deptNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "状态 默认0正常 -1拉黑")
    @Column(nullable = false)
    private Integer status = CommonConstant.USER_STATUS_NORMAL;

    @ApiModelProperty(value = "绑定账户Id")
    @Column(length = 19, nullable = false)
    private String accountId = StrUtil.EMPTY;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "部门名称")
    @Column(length = 20,nullable = false)
    private String title = StrUtil.EMPTY;
    
    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "部门编码")
    @Column(length = 20,nullable = false)
    private String deptNumber = StrUtil.EMPTY;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏姓名")
    private String tmname;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "脱敏员工号")
    private String tmstaffNo;

    @ApiModelProperty(value = "二维码，url地址")
    @Column(length = 200)
    private String qrcode;

    @ApiModelProperty(value = "用户头像，url地址")
    @Column(length = 200)
    private String headImg;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "员工角色")
    private List<StaffRole> roles;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "员工角色IDs")
    private String roleIds;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "员工角色s")
    private String roleNames;

    @ApiModelProperty(value = "排序值")
    private Integer sortOrder = 999 ;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "是否绑定员工")
    private String isBind;

    @ApiModelProperty(value = "职位")
    private String position;

    @ApiModelProperty(value = "备注")
    @Column(length = 500)
    private String remark;

    @ApiModelProperty(value = "标签")
    @Column(length = 500)
    private String tags;

    @ApiModelProperty(value = "默认推荐 0否 1是")
    private Integer recommendFlag;
}