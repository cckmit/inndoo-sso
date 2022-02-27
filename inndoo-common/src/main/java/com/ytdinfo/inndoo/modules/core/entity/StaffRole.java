package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * 员工角色关联关系
 *
 * @author YourName
 * @date 2021-03-08 下午1:34
 **/
@Data
@Entity
@Table(name = "t_staff_role")
@TableName("t_staff_role")
@ApiModel(value = "员工角色")
//@SQLDelete(sql = "update t_staff_role set is_deleted=1 where id=?")
//@Where(clause = "is_deleted=0")
public class StaffRole extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户唯一id")
    private String staffId;

    @ApiModelProperty(value = "角色唯一id")
    private String roleId;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "角色名")
    private String roleName;
}