package com.ytdinfo.inndoo.common.vo;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class BindLogVo {
    @ApiModelProperty(value = "活动平台accountId")
    private String actAccountId = StrUtil.EMPTY;

    @ApiModelProperty(value = "0解绑，1绑定")
    private Boolean isBind;

    @ApiModelProperty(value = "加密手机号")
    private String phone;

    @CreatedDate
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    private String bindDesc = StrUtil.EMPTY;
}
