package com.ytdinfo.inndoo.modules.base.vo;

import com.ytdinfo.conf.core.annotation.XxlConf;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * @author Exrickx
 */
@Data
@Configuration
public class OtherSetting implements Serializable{

    @ApiModelProperty(value = "域名")
    @XxlConf("core.server.url")
    private String domain;

    @ApiModelProperty(value = "IP黑名单")
    @XxlConf("core.server.blacklist")
    private String blacklist;
}
