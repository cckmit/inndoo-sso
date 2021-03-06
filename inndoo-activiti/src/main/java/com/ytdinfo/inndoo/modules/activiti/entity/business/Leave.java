package com.ytdinfo.inndoo.modules.activiti.entity.business;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author Exrick
 */
@Data
@Entity
@Table(name = "t_leave")
@TableName("t_leave")
@ApiModel(value = "请假申请")
@SQLDelete(sql = "update t_leave set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class Leave extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "请假类型")
    private String type;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "说明")
    private String description;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "开始日期")
    private Date startDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "截止日期")
    private Date endDate;

    @ApiModelProperty(value = "请假时长（小时）")
    private Integer duration;
}