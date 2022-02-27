package com.ytdinfo.inndoo.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * Created by timmy on 2019/8/8.
 */
@Data
@MappedSuperclass
public abstract class BaseActivityEntity extends BaseWechatEntity {

    @ApiModelProperty(value = "名称")
    @Column(length = 50,nullable = false)
    private String name;

    @ApiModelProperty(value = "标题")
    @Column(length = 50, nullable = false)
    private String title;

    @ApiModelProperty(value = "开始时间，开始时间当天00:00:00生效，格式yyyy-MM-dd")
    @Column(length = 10, nullable = false)
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @ApiModelProperty(value = "结束时间，结束时间当天23:59:59前有效，格式yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(length = 10, nullable = false)
    private Date endDate;

    @ApiModelProperty(value = "发布状态，0：待发布，-1：下架，1：发布")
    @Column(nullable = false)
    private Integer status;

    @ApiModelProperty(value = "开始时段，格式HH:mm:ss")
    @Column(length = 10, nullable = false)
    private String startTime = "00:00:00";

    @ApiModelProperty(value = "结束时段，格式HH:mm:ss")
    @Column(length = 10, nullable = false)
    private String endTime = "23:59:59";

    @ApiModelProperty(value = "备注说明")
    @Column(length = 2000, nullable = false)
    private String remark;

}