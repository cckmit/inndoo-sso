package com.ytdinfo.inndoo.modules.base.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工查询
 *
 * @author YourName
 * @date 2021-03-19 下午2:27
 **/
@Data
public class StaffSearchVo implements Serializable {

    private String name;

    private String staffNo;

    private String phone;

    private String deptNo;

    private String accountId;

    private String appid;
    //1、绑定  2、未绑定
    private String isBind;
}