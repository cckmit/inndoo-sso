package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

@Data
public class BindLogSearchVo {
    PageVo pageVo;
    private String coreAccountId;
    String tenantId;
    String appid;
}
