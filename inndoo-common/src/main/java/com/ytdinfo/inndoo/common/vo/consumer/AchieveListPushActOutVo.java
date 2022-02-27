package com.ytdinfo.inndoo.common.vo.consumer;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.List;

/**
 * 接收小核心达标名单时间接口
 */
@Data
public class AchieveListPushActOutVo implements Serializable {

    @ApiModelProperty(value = "达标名单id")
    private String listId;

    @ApiModelProperty(value = "名单类型，0：高级校验，1：openid，2：phone，3：小核心accountid，4：活动平台accountid")
    private Integer listType = 0;

    @ApiModelProperty(value = "传到活动平台接收数据 名单类型为1时，identifier值为openid 为0，2，3时为coreAccountId，times值为剩余活动次数，默认为0")
    List<AchieveListRecordVo> recordList;
}
