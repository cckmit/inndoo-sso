package com.ytdinfo.inndoo.modules.base.entity.social;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Exrick
 */
@Data
@Entity
@Table(name = "t_weibo")
@TableName("t_weibo")
@ApiModel(value = "微博用户")
@SQLDelete(sql = "update t_weibo set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class Weibo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "微博唯一id")
    private String openId;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "头像")
    private String avatar;

    @ApiModelProperty(value = "是否绑定账号 默认false")
    private Boolean isRelated = false;

    @ApiModelProperty(value = "绑定用户账号")
    private String relateUsername;
}