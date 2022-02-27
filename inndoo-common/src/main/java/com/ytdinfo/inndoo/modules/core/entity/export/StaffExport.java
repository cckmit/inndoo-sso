package com.ytdinfo.inndoo.modules.core.entity.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 员工信息管理--导出实体类
 *
 * @author xuewenlong
 * @Date 1/28/22
 */
@Data
public class StaffExport {
    // List<?> row1 = CollUtil.newArrayList("姓名", "手机号", "机构编码", "员工号", "状态","绑定状态","员工角色");
    @ExcelProperty(value = "姓名")
    private String name = "";

    @ExcelProperty(value = "手机号")
    private String phone = "";

    @ExcelProperty(value = "机构编码")
    private String deptNo;

    @ExcelProperty(value = "员工号")
    private String staffNo;

    @ExcelProperty(value = "状态")
    private String status;

    @ExcelProperty(value = "绑定状态")
    private String bindStatus;

    @ExcelProperty(value = "员工角色")
    private String roleNames = "";

}
