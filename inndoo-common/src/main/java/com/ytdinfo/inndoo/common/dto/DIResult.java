package com.ytdinfo.inndoo.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据团队返回结果
 */
@Data
public class DIResult implements Serializable {

    private static final long serialVersionUID = 1L;
    private Boolean success;
    private Boolean isLogin;
    private Boolean encrypt;
    private Object data;
    private String errorMsg;
    private Integer total;

    public Boolean success(){
        return success;
    }
}
