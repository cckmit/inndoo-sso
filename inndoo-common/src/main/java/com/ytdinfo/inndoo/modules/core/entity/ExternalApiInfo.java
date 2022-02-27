package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseEntity;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author yaochangning
 */
@Data
@Entity
@Table(name = "t_external_api_info")
@TableName("t_external_api_info")
@ApiModel(value = "外部接口调用定义表")
@SQLDelete(sql = "update t_external_api_info set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ExternalApiInfo extends BaseWechatEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "接口名称")
    @Column(length = 100, nullable = false)
    private String name;

    @ApiModelProperty(value = "接口来源 详情SourceType")
    @Column(length = 2, nullable = false)
    private Integer source;

    @ApiModelProperty(value = "url地址")
    @Column(length = 400, nullable = false)
    private String url;

    @ApiModelProperty(value = "接口入参数据")
    @Column(columnDefinition = "text", nullable = false)
    private String inputMsgBody;

    @ApiModelProperty(value = "接口出参数据")
    @Column(columnDefinition = "text", nullable = false)
    private String outMsgBody;

    @ApiModelProperty(value = "接口入参定义")
    @Column(columnDefinition = "text", nullable = false)
    private String inputDefinitionBody;

    @ApiModelProperty(value = "接口出参定义")
    @Column(columnDefinition = "text", nullable = false)
    private String outDefinitionBody;

    @ApiModelProperty(value = "接口沙箱脚本")
    @Column(columnDefinition = "text", nullable = false)
    private String apiSandboxScript;

    @ApiModelProperty(value = "达标沙箱脚本")
    @Column(columnDefinition = "text", nullable = false)
    private String achieveSandboxScript;

    @ApiModelProperty(value = "是否启用debugger")
    @Column(length = 2, nullable = false)
    private Boolean isDebugger;

    @ApiModelProperty(value = "mock数据")
    @Column(columnDefinition = "text", nullable = false)
    private String mock;

    @Transient
    @TableField(exist=false)
    @ApiModelProperty(value = "来源名称")
    private String sourceName;

}
