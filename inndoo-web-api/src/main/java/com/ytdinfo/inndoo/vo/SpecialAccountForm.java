package com.ytdinfo.inndoo.vo;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormResource;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 11
 *
 * @author YourName
 * @date 2021-05-26 11:42 上午
 **/
@Data
public class SpecialAccountForm  implements Serializable {

    private String id;

    private String appid;

    private String agreement;

    private Boolean enableAgreement;

    private Boolean enableCaptcha;

    private String redirectUrl;

    private String platformLimit;

    private Integer formType;

    private Boolean isIdentifierForm;

    private Boolean isDefault;

    private String checkStaff;

    private List<AccountFormMeta> accountFormMetas;

    private List<AccountFormResource> accountFormResources;

    private List<String> deleteAccountFormMetaIds;

    private Integer actStatus;

    private String viewStartDate;

    private String viewEndDate;

    private String name;

    private String title;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    private Integer status;

    private String startTime;

    private String endTime;

    private String remark;


    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String updateBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    private Boolean isDeleted;

    private Byte type;

    @ApiModelProperty(value = "提交成功后的提示语")
    @Column(nullable = true,length = 200)
    private String successTips = StrUtil.EMPTY;

    @ApiModelProperty(value = "是否显示取消按钮")
    @Column(nullable = false)
    private Boolean cancelBtn = Boolean.FALSE ;

    @ApiModelProperty(value = "取消按钮内容")
    @Column(nullable = false)
    private String cancelBtnContent = StrUtil.EMPTY;
}