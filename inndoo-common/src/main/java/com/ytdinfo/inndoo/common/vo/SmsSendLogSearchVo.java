package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

@Data
public class SmsSendLogSearchVo {
    private String phone;
    private String tenantId;
    private String appid;
    PageVo pageVo;
}
