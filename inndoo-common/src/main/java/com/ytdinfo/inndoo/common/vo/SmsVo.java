package com.ytdinfo.inndoo.common.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 *
 *  * @author QHT
 *  * @date 2020/2/13
 */
@Data
public class SmsVo {

    @ApiModelProperty(value = "未加短信签名的发送内容")
    private String content;

    @ApiModelProperty(value = "发送目标的accountId")
    private String coreAccountId;

    @ApiModelProperty(value = "true：coreAccountId存的是AES加密的手机号，false：coreAccountId存放的就是小核心accountId")
    private Boolean isPhone;
}
