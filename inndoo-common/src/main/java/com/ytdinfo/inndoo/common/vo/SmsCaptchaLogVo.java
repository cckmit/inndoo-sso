package com.ytdinfo.inndoo.common.vo;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class SmsCaptchaLogVo {


    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "验证码")
    private String code = StrUtil.EMPTY;

    private Integer sendStatus = 0;

    @ApiModelProperty(value = "发送状态")
    private String sendStatusDesc = StrUtil.EMPTY;

    private String reason = StrUtil.EMPTY;

}