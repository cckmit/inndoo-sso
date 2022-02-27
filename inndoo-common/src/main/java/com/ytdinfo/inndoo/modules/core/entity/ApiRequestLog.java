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
 * @author zhuzheng
 */
@Data
@Entity
@Table(name = "t_api_request_log")
@TableName("t_api_request_log")
@ApiModel(value = "api请求日志")
@SQLDelete(sql = "update t_api_request_log set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class ApiRequestLog extends BaseWechatEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "请求地址")
    @Column(length = 500, nullable = false)
    private String url;

    @ApiModelProperty(value = "请求方法")
    @Column(length = 8)
    private String method;

    @ApiModelProperty(value = "请求头")
    @Column(columnDefinition = "longtext")
    private String requestHeader = StrUtil.EMPTY;

    @ApiModelProperty(value = "请求体")
    @Column(columnDefinition = "longtext")
    private String requestBody = StrUtil.EMPTY;

    @ApiModelProperty(value = "响应体")
    @Column(columnDefinition = "longtext")
    private String responseBody = StrUtil.EMPTY;

    @ApiModelProperty(value = "异常")
    @Column(columnDefinition = "longtext")
    private String exception = StrUtil.EMPTY;

    @ApiModelProperty(value = "请求耗时")
    @Column(length = 11)
    private Long requestTime;

}