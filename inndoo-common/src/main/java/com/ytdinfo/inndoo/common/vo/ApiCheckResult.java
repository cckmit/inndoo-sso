package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

@Data
public class ApiCheckResult {
    private String appkey;
    private String err_code;
    private String err_msg;
    private String data;
}
