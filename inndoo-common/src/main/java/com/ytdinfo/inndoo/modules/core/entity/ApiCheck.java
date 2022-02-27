package com.ytdinfo.inndoo.modules.core.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_api_check")
@TableName("t_api_check")
@ApiModel(value = "接口校验")
@SQLDelete(sql = "update t_api_check set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ApiCheck extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "接口校验名称")
    @Column(length = 30, nullable = false,unique = true)
    private String name = StrUtil.EMPTY;

    @ApiModelProperty(value = "返回值类型")
    @Column(length = 16, nullable = false)
    private String returnType = StrUtil.EMPTY;

    @ApiModelProperty(value = "动态接口id")
    @Column(length = 19, nullable = false)
    private String dynamicApiId = StrUtil.EMPTY;

    @ApiModelProperty(value = "是否缓存")
    @Column(length = 1)
    private Boolean isCache = false;

    @ApiModelProperty(value = "缓存时长")
    @Column
    private Long cacheTime = 0L;

    @ApiModelProperty(value = "时间单位")
    @Column(length = 16)
    private String timeUnit = StrUtil.EMPTY;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "动态接口名称")
    private String dynamicApiName = StrUtil.EMPTY;

    @ApiModelProperty(value = "备注")
    @Column(length = 2000, nullable = false)
    private String remark = StrUtil.EMPTY;

}
