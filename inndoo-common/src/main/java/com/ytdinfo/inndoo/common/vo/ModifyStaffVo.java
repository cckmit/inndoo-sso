package com.ytdinfo.inndoo.common.vo;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * 修改的员工信息
 *
 * @author YourName
 * @date 2020-10-14 11:41 AM
 **/
@Data
public class ModifyStaffVo implements Serializable {
    @ApiModelProperty(value = "员工Id")
    private String id;

    @ApiModelProperty(value = "姓名")
    private String oldName = StrUtil.EMPTY;

    @ApiModelProperty(value = "工号")
    private String oldStaffNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "绑定账户Id")
    private String oldAccountId = StrUtil.EMPTY;

    @ApiModelProperty(value = "旧部门编号")
    private String oldDeptNo = StrUtil.EMPTY;

    @ApiModelProperty(value = "员工所属部门 从高到低")
    private LinkedList<Department> oldDepartmentList;

    @ApiModelProperty(value = "排序值")
    private Integer sortOrder = 999 ;

    private String tenantId;

    private String appid;

    private String type;


}