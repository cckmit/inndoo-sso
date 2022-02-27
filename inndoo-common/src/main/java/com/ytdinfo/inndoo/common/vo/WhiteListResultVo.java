package com.ytdinfo.inndoo.common.vo;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class WhiteListResultVo {

    @ApiModelProperty(value = "名单类型，0：高级校验，1：openid，2：phone，3：accountid")
    private Integer listType;

    @ApiModelProperty(value = "目标用户标识清单，除了listType为1时，存放的是OpenId列表；其它几种名单类型，存放的都是小核心的AccountId列表")
    private List<String> recordList;

    @ApiModelProperty(value = "下一个主键Id，为空，则说明白名单中的人已经全部取出，不为空，则下一次从此Id开始，继续向下取1W条")
    private String nextId;

    @ApiModelProperty(value = "错误信息")
    private String errMsg = StrUtil.EMPTY;
}

