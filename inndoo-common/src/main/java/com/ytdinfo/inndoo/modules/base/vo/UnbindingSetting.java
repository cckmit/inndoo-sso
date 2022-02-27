package com.ytdinfo.inndoo.modules.base.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zhuzheng
 */
@Data
public class UnbindingSetting implements Serializable {

    @ApiModelProperty(value = "同一act账户在时间间隔内可解绑次数")
    private Long accountUnbindTimes = 0L;

    @ApiModelProperty(value = "同一act账户可解绑时间间隔（天）")
    private Long accountUnbindDayInterval = 0L;

    @ApiModelProperty(value = "同一手机号在时间间隔内可解绑次数")
    private Long phoneUnbindTimes = 0L;

    @ApiModelProperty(value = "同一手机号可解绑时间间隔（天）")
    private Long phoneUnbindDayInterval = 0L;

}
