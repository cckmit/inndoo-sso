package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author zhuzheng
 */
@Data
@Entity
@Table(name = "t_dynamic_code")
@TableName("t_dynamic_code")
@ApiModel(value = "动态代码")
@SQLDelete(sql = "update t_dynamic_code set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class DynamicCode extends BaseWechatEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "名称")
    @Column(length = 64, nullable = false)
    private String name;

    @ApiModelProperty(value = "动态接口类名称")
    @Column(length = 64, nullable = false)
    private String beanName;

    @Transient
    @TableField(exist=false)
    private String code;

    @Transient
    @TableField(exist=false)
    private Boolean newMainVersion;

    @ApiModelProperty(value = "版本")
    @Column(length = 32,nullable = false)
    private String version;

}