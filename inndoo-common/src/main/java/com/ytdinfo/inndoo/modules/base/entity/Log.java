package com.ytdinfo.inndoo.modules.base.entity;

import com.ytdinfo.inndoo.base.BaseEntity;
import com.ytdinfo.inndoo.common.utils.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Map;

/**
 * @author Exrickx
 */
@Data
@Entity
@Table(name = "t_log")
@TableName("t_log")
@ApiModel(value = "日志")
@SQLDelete(sql = "update t_log set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class Log extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "方法操作名称")
    private String name;

    @ApiModelProperty(value = "日志类型 0登陆日志 1操作日志")
    private Integer logType;

    @ApiModelProperty(value = "请求路径")
    private String requestUrl;

    @ApiModelProperty(value = "请求类型")
    private String requestType;

    @ApiModelProperty(value = "请求参数")
    @Column(length = 1000)
    private String requestParam;

    @ApiModelProperty(value = "请求用户")
    private String username;

    @ApiModelProperty(value = "ip")
    private String ip;

    @ApiModelProperty(value = "ip信息")
    private String ipInfo;

    @ApiModelProperty(value = "花费时间")
    private Integer costTime;

    /**
     * 转换请求参数为Json
     * @param paramMap
     */
    public void setMapToParams(Map<String, String[]> paramMap) {

        this.requestParam = ObjectUtil.mapToString(paramMap);
    }

}
