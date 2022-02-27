package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

@Data
public class ApiCheckTestVo {
    private String id;

    private String actAccountId;

    private String accountId;

    private String openId;

    private Byte accountType;

    private String response;
}