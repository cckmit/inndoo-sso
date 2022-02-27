package com.ytdinfo.inndoo.common.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AchieveListRecordDataVo {

    @ApiModelProperty(value = "id")
    private String id;

    @ApiModelProperty(value = "达标名单用户标识，手机号为加密字符，其他类型为明文")
    private String identifier;

    @ApiModelProperty(value = "剩余活动次数")
    private BigDecimal times;

}
