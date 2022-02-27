package com.ytdinfo.inndoo.vo;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class RegisterVo {
    private String phone;

    @ApiModelProperty(value = "注册日期，格式yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date registerDate;

    private String virtualPhone = StrUtil.EMPTY;

    private String location = StrUtil.EMPTY;

    private String company = StrUtil.EMPTY;

    /***
     * 中国银行网联号
     */
    private String bocWlh = StrUtil.EMPTY;

    /***
     * 中国银行机构号
     */
    private String bocOrgCode = StrUtil.EMPTY;

    /***
     * 客户号
     */
    private String cusNo = StrUtil.EMPTY;

    /**
     * 员工注册还是客户注册
     */
    private Integer formType = -1;
}
