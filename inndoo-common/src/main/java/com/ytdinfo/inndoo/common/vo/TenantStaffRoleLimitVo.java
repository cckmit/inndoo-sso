package com.ytdinfo.inndoo.common.vo;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Data
public class TenantStaffRoleLimitVo implements Serializable {

    private String id;

    @ApiModelProperty(value = "租户Id，为空表示所有租户通用")
    private String tenantId ;

    @ApiModelProperty(value = "是否开启人数限制 true 无限制，false 有限制")
    private Boolean enableLimit;

    @ApiModelProperty(value = "受限角色的名称")
    private String roleName;

    @ApiModelProperty(value = "限制角色拥有人数")
    private Integer limitSize;
}
