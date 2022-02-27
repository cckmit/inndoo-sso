package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ActAccountVo implements Serializable {

    private String id;

    private String accountId;

    private String coreAccountId;

    private String accountTypeName;

    private Byte accountType;

    private String openId;

}
