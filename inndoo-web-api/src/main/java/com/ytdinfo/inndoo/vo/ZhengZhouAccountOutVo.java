package com.ytdinfo.inndoo.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ZhengZhouAccountOutVo implements Serializable {
    //是否注册成功
    public Boolean register;

    public String returnUrl;

    public String code;
}
