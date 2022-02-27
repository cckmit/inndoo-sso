package com.ytdinfo.inndoo.modules.core.entity.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 *
 * 账户信息--导出实体类
 *
 * @author xuewenlong
 * @Date 1/28/22
 */
@Data
public class AccountExport {
    // ("客户号", "账户","姓名", "手机号",  "出生日期", "银行卡号", "身份证号码", "邮箱", "地址", "客户唯一标识符");
    @ExcelProperty(value = "客户号")
    private String customerNo;

    @ExcelProperty(value = "账户")
    private String id;

    @ExcelProperty(value = "姓名")
    private String name;

    @ExcelProperty(value = "手机号")
    private String phone;

    @ExcelProperty(value = "出生日期")
    private String birthday;

    @ExcelProperty(value = "银行卡号")
    private String bankcardNo;

    @ExcelProperty(value = "身份证号码")
    private String idcardNo;

    @ExcelProperty(value = "邮箱")
    private String email;

    @ExcelProperty(value = "地址")
    private String address;

    @ExcelProperty(value = "客户唯一标识符")
    private String identifier;


}
