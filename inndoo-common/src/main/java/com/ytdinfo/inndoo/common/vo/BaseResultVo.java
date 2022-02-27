package com.ytdinfo.inndoo.common.vo;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

@Data
public class BaseResultVo {
    private boolean success;
    private String errMsg = StrUtil.EMPTY;
    private String errCode = StrUtil.EMPTY;
    private String id = StrUtil.EMPTY;
}
