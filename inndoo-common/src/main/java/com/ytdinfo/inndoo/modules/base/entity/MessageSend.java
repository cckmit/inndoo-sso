package com.ytdinfo.inndoo.modules.base.entity;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author Exrick
 */
@Data
@Entity
@Table(name = "t_message_send")
@TableName("t_message_send")
@ApiModel(value = "消息发送详情")
@SQLDelete(sql = "update t_message_send set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class MessageSend extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "关联消息id")
    private String messageId;

    @ApiModelProperty(value = "关联用户id")
    private String userId;

    @ApiModelProperty(value = "状态 0默认未读 1已读 2回收站")
    private Integer status = CommonConstant.MESSAGE_STATUS_UNREAD;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "发送用户名")
    private String username;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "消息标题")
    private String title;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "消息内容")
    private String content;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "消息类型")
    private String type;
}