package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 *
 * @author timmy
 * @date 2019/10/24
 */
@Data
public class NameListValidateResultVo {
    private boolean match;
    private String formId;
    private String registerUrl;
    private Integer times;
    private BigDecimal value;
}