package com.ytdinfo.inndoo.modules.base.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Exrickx
 */
@Data
@AllArgsConstructor
public class RedisVo {

    private String key;

    private String value;
}
