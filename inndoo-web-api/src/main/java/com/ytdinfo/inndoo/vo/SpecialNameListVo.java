package com.ytdinfo.inndoo.vo;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

/**
 * @author timmy
 * @date 2019/10/17
 */
@Data
public class SpecialNameListVo implements Serializable {
    private String id;
    private String name;
    private Integer listType;
    private String formId;
    private String validateFields;
    private String remark;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireDate;
    private Byte linkType;
    private String linkId;
    private Byte isTimes;
    private Byte superimposed;
    private Byte isDifferentReward;
    private Byte isEncryption;
    private Byte encryptionMethod;
    private String encryptionPassword;
}