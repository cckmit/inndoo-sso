package com.ytdinfo.inndoo.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OfflineRecordingInputVo implements Serializable {
    private String listId;

    private String listType;

    private Boolean multipleAllowed;

    private List<OfflineRecordingFieldVo> offlineRecordingFieldVos;
}
