package com.ytdinfo.inndoo.common.vo.consumer;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 达标名单用户标识及次数
 */
@Data
public class AchieveListRecordVo implements Serializable {

    @ApiModelProperty(value = "达标名单用户标识，手机号为加密字符，其他类型为明文")
    private String identifier;

    @ApiModelProperty(value = "剩余活动次数")
    private BigDecimal times;

    @ApiModelProperty(value = "是否未找到用户")
    private  boolean found =true;

}
