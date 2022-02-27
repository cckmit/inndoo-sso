package com.ytdinfo.inndoo.modules.core.entity;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
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
@Table(name = "t_act_account")
@TableName("t_act_account")
@ApiModel(value = "活动平台Account关联表")
//@SQLDelete(sql = "update t_act_account set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ActAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "小核心账户Id")
    @Column(length = 19, nullable = false)
    private String coreAccountId;

    @ApiModelProperty(value = "活动平台账户Id")
    @Column(length = 19, nullable = false)
    private String actAccountId;

}