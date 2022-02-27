package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

import java.util.Map;

@Data
public class RequestDefineVo {
    private String url;
    private Map<String,String> params;
}
