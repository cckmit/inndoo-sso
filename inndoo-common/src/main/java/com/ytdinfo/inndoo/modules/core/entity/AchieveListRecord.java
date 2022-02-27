package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_achieve_list_record_2")
@TableName("t_achieve_list_record_2")
@ApiModel(value = "达标名单清单")
@SQLDelete(sql = "delete from t_achieve_list_record_2  where id=?")
@Where(clause = "is_deleted=0")
public class AchieveListRecord extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "达标名单Id")
    @Column(length = 19, nullable = false)
    private String listId;

    @ApiModelProperty(value = "达标名单用户标识，手机号为加密字符，其他类型为明文")
    @Column(length = 64, nullable = false)
    private String identifier;

//    public String getIdentifier() {
//        if (listType.equals(NameListType.ADVANCED)) {
//            Map paramMap = new HashMap<>();
//            for (AchieveListExtendRecord record : this.extendInfo) {
//                paramMap.put(record.getFormMetaId(), record.getRecord());
//            }
//            return SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
//        }
//        return this.identifier;
//    }

    @ApiModelProperty(value = "状态 默认0正常 -1禁用")
    @Column(nullable = false)
    private Integer status = CommonConstant.STATUS_NORMAL;

    @Transient
    @TableField(exist = false)
    @ApiModelProperty(value = "扩展字段，来自达标名单扩展表")
    private List<AchieveListExtendRecord> extendInfo;

    @Transient
    @TableField(exist = false)
    @ApiModelProperty(value = "达标名单类型，0：高级校验，1：openid，2：phone，3：accountid")
    @Column(nullable = false)
    private Integer listType = 0;

    @ApiModelProperty(value = "剩余活动次数")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal times =BigDecimal.ZERO;

    @ApiModelProperty(value = "达标名单导入后是否已有推送到act活动平台操作")
    @Column(nullable = false)
    private Boolean pushAct = false;
}