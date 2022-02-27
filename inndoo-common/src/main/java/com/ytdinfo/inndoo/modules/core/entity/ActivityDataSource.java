package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ytdinfo.inndoo.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Timmy
 */
@Data
public class ActivityDataSource implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "唯一标识")
    private String id;
    @ApiModelProperty(value = "创建者")
    private String createBy;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "更新者")
    private String updateBy;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
    @ApiModelProperty(value = "删除标志 默认0")
    private Boolean isDeleted;
    @ApiModelProperty(value = "数据源名称")
    private String name;
    @ApiModelProperty(value = "数据源连接字符串，不含密码")
    private String databaseUrl;
    @ApiModelProperty(value = "core项目数据源连接字符串，不含密码")
    private String coreDatabaseUrl;
    @ApiModelProperty(value = "数据源备注信息")
    private String remark;
    @ApiModelProperty(value = "是否默认数据源")
    private Boolean isDefault;

}