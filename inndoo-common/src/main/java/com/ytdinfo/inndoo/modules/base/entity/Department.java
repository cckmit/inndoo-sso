package com.ytdinfo.inndoo.modules.base.entity;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Exrick
 */
@Data
@Entity
@Table(name = "t_department")
@TableName("t_department")
@ApiModel(value = "部门")
@SQLDelete(sql = "update t_department set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class Department extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "部门名称")
    @Column(length = 20,nullable = false)
    private String title = StrUtil.EMPTY;

    @ApiModelProperty(value = "部门编码")
    @Column(length = 20, unique = true,nullable = false)
    private String deptCode = StrUtil.EMPTY;

    @ApiModelProperty(value = "父id")
    @Column(length = 19,nullable = false)
    private String parentId;

    @ApiModelProperty(value = "是否为父节点(含子节点) 默认false")
    @Column(nullable = false)
    private Boolean isParent = false;

    @ApiModelProperty(value = "排序值")
    @Column(precision = 10, scale = 2)
    private BigDecimal sortOrder;

    @ApiModelProperty(value = "是否启用 0启用 -1禁用")
    private Integer status = CommonConstant.STATUS_NORMAL;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "父节点名称")
    private String parentTitle;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "主负责人")
    private List<String> mainHeader;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "副负责人")
    private List<String> viceHeader;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "拓展属性值")
    private List<Department> children;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "父级部门")
    private LinkedList<Department> parentList;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "层级 从第一层开始")
    private Integer level;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "拥有联系人")
    private Boolean hasContact = false;

}