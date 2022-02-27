package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListExtendRecord;

import java.util.List;
import java.util.Map;

public interface IWhiteListExtendRecordService extends BaseIService<WhiteListExtendRecord> {
    int deleteBatchByIdentifersAndListId(List<String> deleteIdentifers, String listId,int i);

    List<Map<String, Object>> findTransformDate(Map<String, Object> findMap);
}
