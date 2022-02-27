package com.ytdinfo.inndoo.vo;

import lombok.Data;

/**
 *
 * @author timmy
 * @date 2019/10/17
 */
@Data
public class NameListVo {
    private String id;
    private String name;
    private Integer listType;
    private String formId;
    private Byte linkType;
    private Byte isTimes;
    private String validateFields;
}