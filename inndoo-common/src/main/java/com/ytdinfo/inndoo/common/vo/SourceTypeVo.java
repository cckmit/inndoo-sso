package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

/**
 * @author timmy
 * @date 2019/11/20
 */
@Data
public class SourceTypeVo {

    public SourceTypeVo(){}

    public SourceTypeVo(Integer value, String name) {
        this.value = value;
        this.name = name;
    }

    private Integer value;
    private String name;
}