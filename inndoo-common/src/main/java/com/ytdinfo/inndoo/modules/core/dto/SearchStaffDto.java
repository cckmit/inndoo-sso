package com.ytdinfo.inndoo.modules.core.dto;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

@Data
public class SearchStaffDto {

    private String branchId = StrUtil.EMPTY;

    private String subBranchId = StrUtil.EMPTY;

    private String staffName = StrUtil.EMPTY;

    private String staffId = StrUtil.EMPTY;

//    private Integer pageNumber;

//    private Integer pageSize;

}
