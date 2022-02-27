package com.ytdinfo.inndoo.common.vo;

import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedList;

/**
 * 修改的机构信息
 *
 * @author YourName
 * @date 2020-10-14 11:42 AM
 **/
@Data
public class ModifyDepartmentVo implements Serializable {

    @ApiModelProperty(value = "部门id")
    private String id;

    @ApiModelProperty(value = "部门旧名称")
    private String oldTitle;

    @ApiModelProperty(value = "部门旧编码")
    private String oldDeptCode;

    @ApiModelProperty(value = "部门旧上级")
    private LinkedList<Department> parentList;


    @ApiModelProperty(value = "部门旧名称")
    private String Title;

    @ApiModelProperty(value = "部门旧编码")
    private String DeptCode;

    @ApiModelProperty(value = "type")
    private String type;

    @ApiModelProperty(value = "父id")
    private String parentId;

    @ApiModelProperty(value = "是否为父节点(含子节点) 默认false")
    private Boolean isParent = false;

    @ApiModelProperty(value = "排序值")
    @Column(precision = 10, scale = 2)
    private BigDecimal sortOrder;

    @ApiModelProperty(value = "是否启用 0启用 -1禁用")
    private Integer status = CommonConstant.STATUS_NORMAL;

    private String tenantId;

    private String appid;


}