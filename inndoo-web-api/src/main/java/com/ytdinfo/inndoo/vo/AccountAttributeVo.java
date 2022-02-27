package com.ytdinfo.inndoo.vo;

import lombok.Data;


/**
 * @FileName: AccountAttribute
 * @Author: zhulin
 * @Date: 2020/9/16 9:36 AM
 * @Description: 用户属性
 */
@Data
public class AccountAttributeVo {

    /**
     * 属性值
      */
    private String value;

    /**
     * 表单类型，如input/select/textarea/phone/idcard/sex/name等
     */
    private String metaType;

    /**
     * 表单字段说明信息
     */
    private String metaDesc;

}
