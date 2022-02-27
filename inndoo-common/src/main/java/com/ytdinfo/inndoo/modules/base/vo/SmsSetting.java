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
public class SmsSetting implements Serializable{

    @ApiModelProperty(value = "工作流通知短信内容，含签名")
    @XxlConf("core.sms.template.activiti")
    private String activitiTemplate;

    @ApiModelProperty(value = "验证码内容，不含签名")
    @XxlConf("core.sms.template.captcha")
    private String captchaTemplate;
}
