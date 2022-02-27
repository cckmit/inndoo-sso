package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountOutVo implements Serializable {

    private String Id;

    private Integer isStaff;

    private Boolean isbind;

    private String createTime;

    private String phone;
}
