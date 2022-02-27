package com.ytdinfo.inndoo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 *
 * @author timmy
 * @date 2019/9/23
 */
@Data
public class AccountVo implements Serializable {
    private String AccountId;
    private Boolean exist;
}