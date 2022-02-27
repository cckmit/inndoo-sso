package com.ytdinfo.inndoo.common.vo;


import com.ytdinfo.inndoo.modules.core.entity.MqException;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * mq执行异常
 * @author yaochangning
 */
@Data
public class MqExceptionVo extends MqException {
    @ApiModelProperty(value = "项目名称，act,core,sso,matrix")
    private String projectName;
    @ApiModelProperty(value = "租户id")
    private String tenantId;
}
