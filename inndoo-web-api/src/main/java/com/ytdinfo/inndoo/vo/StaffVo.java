package com.ytdinfo.inndoo.vo;

import com.ytdinfo.inndoo.modules.base.entity.Department;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;

@Data
public class StaffVo {

    private String id;

    private String name;

    private String staffNo;

    private String phone;

    private String deptNo;

    private Integer status;

    private String deptNumber;

    private Department department;

    private String qrcode;

    private String headImg;

}