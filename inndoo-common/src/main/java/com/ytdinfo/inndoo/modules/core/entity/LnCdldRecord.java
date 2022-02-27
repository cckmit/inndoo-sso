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
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author haiqing
 */
@Data
@Entity
@Table(name = "t_lncdld_record")
@TableName("t_lncdld_record")
@ApiModel(value = "存贷联动记录表")
@SQLDelete(sql = "update t_lncdld_record set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class LnCdldRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动平台账户id")
    @Column(length = 19, nullable = false)
    private String actAccountId;

    @ApiModelProperty(value = "比较值")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal val = BigDecimal.ZERO;

    @ApiModelProperty(value = "领取资格的初始金额")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal startAmt = BigDecimal.ZERO;

    @ApiModelProperty(value = "一重礼达标金额")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal t1amt = BigDecimal.ZERO;

    @ApiModelProperty(value = "一重礼达标金额")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal t2amt = BigDecimal.ZERO;

    @ApiModelProperty(value = "一重礼达标时间")
    @Column(nullable = false)
    private int t1date = 0;

    @ApiModelProperty(value = "二重礼达标时间")
    @Column(nullable = false)
    private int t2date = 0;

    @ApiModelProperty(value = "一重礼达标时间")
    @Column
    private Date t1dateTime;

    @ApiModelProperty(value = "二重礼自然达标时间")
    @Column
    private Date t2dateTime;

    @ApiModelProperty(value = "二重礼实际达标标志")
    @Column(nullable = false)
    private Boolean t2end = Boolean.FALSE;
}
