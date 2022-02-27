package com.ytdinfo.inndoo.modules.core.entity;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import javax.persistence.Column;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_exception_log")
@TableName("t_exception_log")
@ApiModel(value = "异常日志")
@SQLDelete(sql = "update t_exception_log set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ExceptionLog extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "地址")
    @Column(columnDefinition = "text", nullable = false)
    private String url;
    
    @ApiModelProperty(value = "消息体")
    @Column(columnDefinition = "text", nullable = false)
    private String msgBody;

    @ApiModelProperty(value = "异常")
    @Column(columnDefinition = "longtext", nullable = false)
    private String exception;

    @ApiModelProperty(value = "是否是后端，默认true")
    @Column(length = 1,nullable = false,columnDefinition="tinyint default 1")
    private Boolean isBackend = Boolean.TRUE;

}