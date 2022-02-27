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
import javax.persistence.UniqueConstraint;

/**
 * @author Timmy
 */
@Data
@Entity
@Table(name = "t_white_list_extend_record")
@TableName("t_white_list_extend_record")
@ApiModel(value = "白名单扩展清单")
@SQLDelete(sql = "delete from t_white_list_extend_record  where id=?")
@Where(clause = "is_deleted=0")
public class WhiteListExtendRecord extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "白名单Id")
    @Column(length = 19, nullable = false)
    private String listId = StrUtil.EMPTY;

    @ApiModelProperty(value = "白名单RecordId")
    @Column(length = 19, nullable = false)
    private String recordId = StrUtil.EMPTY;

    @ApiModelProperty(value = "扩展信息对应的FormId")
    @Column(length = 19, nullable = false)
    private String formMetaId;

    @ApiModelProperty(value = "扩展信息编码")
    @Column(length = 20, nullable = false)
    private String metaCode;

    @ApiModelProperty(value = "扩展信息名称")
    @Column(length = 10, nullable = false)
    private String metaTitle;

    @ApiModelProperty(value = "扩展信息值")
    @Column(length = 500, nullable = false)
    private String record;

    @ApiModelProperty(value = "白名单用户标识，手机号为加密字符，其他类型为明文")
    @Column(length = 32, nullable = false)
    private String identifier;

}