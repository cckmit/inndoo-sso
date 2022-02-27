package com.ytdinfo.inndoo.common.vo;

import com.ytdinfo.inndoo.modules.core.entity.ExternalApiInfo;
import lombok.Data;

import java.util.Map;

@Data
public class ExternalAPIResultVo {
    private String msg;
    private boolean success;
    private ExternalApiInfo externalApiInfo;
    Map actResult;
}
