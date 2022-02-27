package com.ytdinfo.inndoo.common.vo.consumer;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TagUserVo{

    private String tenantId;

    private String appId;

    private String reqId;

    private String data;

}
