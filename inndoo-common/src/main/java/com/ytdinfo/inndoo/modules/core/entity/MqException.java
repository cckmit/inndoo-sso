package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author haiqing
 */
@Data
@Entity
@Table(name = "t_mq_exception")
@TableName("t_mq_exception")
@ApiModel(value = "mq执行异常")
public class MqException extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "队列名称")
    @Column(length = 100, nullable = false)
    private String queueName;

    @ApiModelProperty(value = "消息体")
    @Column(columnDefinition = "text", nullable = false)
    private String msgBody;

    @ApiModelProperty(value = "异常")
    @Column(columnDefinition = "text", nullable = false)
    private String exception;

}