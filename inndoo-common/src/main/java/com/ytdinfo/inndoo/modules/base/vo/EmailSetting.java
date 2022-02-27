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
public class EmailSetting implements Serializable{

    @ApiModelProperty(value = "邮箱服务器")
    @XxlConf("matrix.email.host")
    private String host;

    @ApiModelProperty(value = "发送者邮箱账号")
    @XxlConf("matrix.email.username")
    private String username;

    @ApiModelProperty(value = "邮箱授权码")
    @XxlConf("matrix.email.password")
    private String password;

    @ApiModelProperty(value = "SMTP端口")
    @XxlConf("matrix.email.port")
    private String port;
}
