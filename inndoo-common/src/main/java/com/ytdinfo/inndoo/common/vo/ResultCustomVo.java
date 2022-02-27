package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

@Data
public class ResultCustomVo {
    private boolean success;
    private String content;
    private Object result;
}
