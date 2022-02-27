package com.ytdinfo.inndoo.modules.core.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_achieve_list")
@TableName("t_achieve_list")
@ApiModel(value = "达标名单")
@SQLDelete(sql = "update t_achieve_list set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class AchieveList extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "达标名单名称")
    @Column(length = 20, nullable = false,unique = true)
    private String name = StrUtil.EMPTY;

    @ApiModelProperty(value = "名单类型，0：高级校验，1：openid，2：phone，3：小核心accountid，4:活动平台accountid")
    @Column(nullable = false)
    private Integer listType = 0;

    @ApiModelProperty(value = "高级校验时选中的表单Id")
    @Column(length = 19, nullable = false)
    private String formId;

    @ApiModelProperty(value = "组合校验字段，以逗号分隔")
    @Column(length = 300, nullable = false)
    private String validateFields = StrUtil.EMPTY;

    @ApiModelProperty(value = "备注")
    @Column(length = 2000, nullable = false)
    private String remark = StrUtil.EMPTY;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "过期时间")
    private Date expireDate;

    @ApiModelProperty(value = "关联类型LinkTypeConstant  1:白名单;2:限制;3:达标;4、接口")
    @Column(length = 1, nullable = false)
    private Byte linkType = 0;

    @ApiModelProperty(value = "关联ID")
    @Column(length = 19, nullable = false)
    private String linkId = StrUtil.EMPTY;

    @ApiModelProperty(value = "名单是否包含参与活动次数，0不包含、1包含")
    @Column(length = 1, nullable = false)
    private Byte isTimes = 0;

    @ApiModelProperty(value = "名单导入时活动次数是否叠加 0表示不叠加次数 1表示叠加次数")
    @Column(length = 1, nullable = false)
    private Byte superimposed = 0;

    @ApiModelProperty(value = "是不是发不同的分值，0不包含、1包含")
    @Column(length = 1, nullable = false)
    private Byte isDifferentReward = 0;

    @ApiModelProperty(value = "是否加密：0不加密，1加密")
    @Column(length = 1, nullable = false)
    private Byte isEncryption = 0;

    @ApiModelProperty(value = "加密方式：EncryptionMethodType")
    @Column(length = 1, nullable = false)
    private Byte encryptionMethod = 0;

    @ApiModelProperty(value = "加密密钥")
    @Column(length = 32, nullable = false)
    private String encryptionPassword = StrUtil.EMPTY;

    @ApiModelProperty(value = "数据是否加盐：0不加盐，1加盐")
    @Column(length = 1, nullable = false)
    private Byte isDataAddSalt = 0;

    @ApiModelProperty(value = "数据盐值")
    @Column(length = 32, nullable = false)
    private String dataSalt = StrUtil.EMPTY;

    @ApiModelProperty(value = "加盐方式：0 数据前后加盐")
    @Column(length = 1, nullable = false)
    private Byte dataSaltMethod = 0;

    @ApiModelProperty(value = "加盐位置：0前 1后")
    @Column(length = 1, nullable = false)
    private Byte dataSaltPosition = 0;

}
