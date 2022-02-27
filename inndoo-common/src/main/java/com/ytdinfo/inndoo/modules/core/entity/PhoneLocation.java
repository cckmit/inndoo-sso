package com.ytdinfo.inndoo.modules.core.entity;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
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
@Table(name = "t_phone_location")
@TableName("t_phone_location")
@ApiModel(value = "手机号码归属地")
@SQLDelete(sql = "update t_phone_location set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class PhoneLocation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "手机号码")
    @Column(length = 32, nullable = false)
    private String phone;

    @ApiModelProperty(value = "省份")
    @Column(length = 20, nullable = false)
    private String province;

    @ApiModelProperty(value = "城市")
    @Column(length = 25, nullable = false)
    private String city;

    @ApiModelProperty(value = "运营商")
    @Column(length = 25, nullable = false)
    private String company;

}