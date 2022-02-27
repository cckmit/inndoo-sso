package com.ytdinfo.inndoo.common.vo;

import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class LiaoNingResult implements Serializable {
    /**
     * 结果对象
     */
    private List<Map<String,Object>> data;

    private Boolean success;

    private Boolean login;

    private Boolean encrypt;

    private String traceId;

    private String errorMsg;

    private String total;
}
