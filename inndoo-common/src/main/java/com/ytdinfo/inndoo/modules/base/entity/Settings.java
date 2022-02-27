package com.ytdinfo.inndoo.modules.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "t_settings")
@TableName("t_settings")
@ApiModel(value = "系统配置")
@SQLDelete(sql = "update t_settings set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class Settings extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty
    @Column(length = 50, nullable = false)
    private String keyName;

    @ApiModelProperty
    @Column(length = 2000, nullable = false)
    private String value;
}
