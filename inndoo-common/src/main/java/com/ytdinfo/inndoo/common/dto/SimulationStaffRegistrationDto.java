package com.ytdinfo.inndoo.common.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author  zhuzheng
 * @desc    模拟员工注册数据传输对象
 */
@Data
public class SimulationStaffRegistrationDto {

    private String tenantId;

    private String appId;

    private String actAccountId;

    private String name;

    private String phone;

    private String staffNo;

    private String deptCode;

    private Date bindTime;

}
