package com.ytdinfo.inndoo.modules.activiti.entity;

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
@Table(name = "t_act_node")
@TableName("t_act_node")
@ApiModel(value = "节点")
@SQLDelete(sql = "update t_act_node set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ActNode extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "节点id")
    private String nodeId;

    @ApiModelProperty(value = "节点关联类型 0角色 1用户 2部门")
    private Integer type;

    @ApiModelProperty(value = "关联其他表id")
    private String relateId;

}