package com.ytdinfo.inndoo.modules.core.entity;

import cn.hutool.core.util.StrUtil;
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

@Data
@Entity
@Table(name = "t_api_data")
@TableName("t_api_data")
@ApiModel(value = "接口数据记录")
@SQLDelete(sql = "update t_api_data set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ApiData extends BaseWechatEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据类型名称")
    @Column(length = 30, nullable = false)
    private String typeName = StrUtil.EMPTY;

    @ApiModelProperty(value = "匹配唯一标识")
    @Column(length = 32, nullable = false)
    private String identifier = StrUtil.EMPTY;

    @ApiModelProperty(value = "唯一标识类型")
    @Column(length = 20, nullable = false)
    private String identifier_type = StrUtil.EMPTY;

    @ApiModelProperty(value = "数据值")
    @Column(length = 500, nullable = false)
    private String dataValue = StrUtil.EMPTY;
}
