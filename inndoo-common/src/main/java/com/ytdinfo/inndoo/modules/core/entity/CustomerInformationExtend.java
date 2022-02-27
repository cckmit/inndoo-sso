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
import java.io.Serializable;

/**
 * @author yaochangning
 */
@Data
@Entity
@Table(name = "t_customer_information_extend")
@TableName("t_customer_information_extend")
@ApiModel(value = "客户信息拓展表")
@SQLDelete(sql = "update t_customer_information_extend set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class CustomerInformationExtend extends BaseWechatEntity implements Serializable {

    @ApiModelProperty(value = "客户信息id")
    @Column(length = 19, nullable = false)
    private String customerInformationId  = StrUtil.EMPTY;

    @ApiModelProperty(value = "拓张信息名称")
    @Column(length = 40, nullable = false)
    private String title = StrUtil.EMPTY;

    @ApiModelProperty(value = "拓张信息值")
    @Column(length = 200, nullable = false)
    private String value = StrUtil.EMPTY;
}
