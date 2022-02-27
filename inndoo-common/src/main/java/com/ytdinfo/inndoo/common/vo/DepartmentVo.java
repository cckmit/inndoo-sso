package com.ytdinfo.inndoo.common.vo;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Exrickx
 */
@Data
public class DepartmentVo {
	@ApiModelProperty(value = "机构id")
	String id;
	@ApiModelProperty(value = "机构名称")
	String title;
	@ApiModelProperty(value = "机构编码")
	String deptCode;
	@ApiModelProperty(value = "子集")
	List<DepartmentVo> children;
}
