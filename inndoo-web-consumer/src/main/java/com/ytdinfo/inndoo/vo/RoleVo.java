package com.ytdinfo.inndoo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Exrick
 */
@Data
public class RoleVo {

    @ApiModelProperty(value = "id")
    private String id;

    @ApiModelProperty(value = "名称")
    private String title;

    @ApiModelProperty(value = "展示名称")
    private String value;

    @ApiModelProperty(value = "是否选中")
    private Boolean checked=false;

    @ApiModelProperty(value = "appId")
    private String appId;
}
