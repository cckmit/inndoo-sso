package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

/**
 * 角色查询
 *
 * @author YourName
 * @date 2021-03-15 下午12:00
 **/
@Data
public class StaffVo {

    private String appid;

    private String name ;

    private String staffNo ;

    private String phone ;

    private String deptNo ;

    private Integer status ;

    private String accountId ;

    private String title ;

    private String deptNumber ;

    private String tmname;

    private String tmstaffNo;

    private String qrcode;

    private String headImg;

    private String roleIds;

    private String roleNames;

    private Integer sortOrder;

    private String roleId;

    private String isbind;

    PageVo pageVo;

}