package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OfflineRecordingFieldVo implements Serializable {
    private String listId;

    private String listType;

    private String metaId;

    private String metaType;

    private String metaName;

    private String fieldData;
}
