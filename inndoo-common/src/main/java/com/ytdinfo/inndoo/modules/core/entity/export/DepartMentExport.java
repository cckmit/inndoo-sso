package com.ytdinfo.inndoo.modules.core.entity.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 *
 * 机构管理--导出机构实体类
 *
 * @author xuewenlong
 * @Date 1/28/22
 */
@Data
public class DepartMentExport {
    //     	List<?> row1 = CollUtil.newArrayList("一级机构名称", "一级机构编码", "二级机构名称", "二级机构编码", "三级机构名称", "三级机构编码", "四级机构名称", "四级机构编码", "五级机构名称", "五级机构编码","六级机构名称", "六级机构编码");

    @ExcelProperty(value = "一级机构名称")
    private String firstOrgName;

    @ExcelProperty(value = "一级机构编码")
    private String firstOrgNo;

    @ExcelProperty(value = "二级机构名称")
    private String secondOrgName;

    @ExcelProperty(value = "二级机构编码")
    private String secondOrgNo;

    @ExcelProperty(value = "三级机构名称")
    private String threeOrgName;

    @ExcelProperty(value = "三级机构编码")
    private String threeOrgNo;

    @ExcelProperty(value = "四级机构名称")
    private String fourOrgName;

    @ExcelProperty(value = "四级机构编码")
    private String fourOrgNo;

    @ExcelProperty(value = "五级机构名称")
    private String fiveOrgName;

    @ExcelProperty(value = "五级机构编码")
    private String fiveOrgNo;

    @ExcelProperty(value = "六级机构名称")
    private String sixOrgName;

    @ExcelProperty(value = "六级机构编码")
    private String sixOrgNo;


}
