package com.ytdinfo.inndoo.modules.core.entity;

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
import java.io.Serializable;

/**
 * @author zhuzheng
 */
@Data
@Entity
@Table(name = "t_dynamic_api_detail")
@TableName("t_dynamic_api_detail")
@ApiModel(value = "动态接口")
@SQLDelete(sql = "update t_dynamic_api_detail set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class DynamicApiDetail extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "动态接口Id")
    @Column(length = 32, nullable = false)
    private String dynamicApiId;

    @ApiModelProperty(value = "动态接口类名称")
    @Column(length = 64, nullable = false)
    private String beanName;

    @ApiModelProperty(value = "代码")
    @Column(nullable = false,columnDefinition = "text")
    private String code;

    @ApiModelProperty(value = "版本")
    @Column(length = 32,nullable = false)
    private String version;
}