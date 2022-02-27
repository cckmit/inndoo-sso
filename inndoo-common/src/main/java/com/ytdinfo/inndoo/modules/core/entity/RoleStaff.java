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

/**
 * 员工角色
 *
 * @author YourName
 * @date 2021-03-08 下午1:25
 **/
@Data
@Entity
@Table(name = "t_role_staff")
@TableName("t_role_staff")
@ApiModel(value = "员工角色")
public class RoleStaff extends BaseWechatEntity {

    @ApiModelProperty(value = "角色名")
    @Column(length = 20, nullable = false)
    private String name;

    @ApiModelProperty(value = "编码 以STAFF_开头")
    @Column(length = 30, nullable = false)
    private String code;

    @ApiModelProperty(value = "是否为注册默认角色")
    private Boolean defaultRole;

    @ApiModelProperty(value = "备注")
    @Column(length = 200,nullable = false)
    private String description = StrUtil.EMPTY;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "人数上线")
    private String limitSize;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "已有人数")
    private String alreadySize;
}