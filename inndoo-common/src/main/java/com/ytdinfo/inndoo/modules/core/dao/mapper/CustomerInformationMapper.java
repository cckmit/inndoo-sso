package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformation;

import java.util.List;

public interface CustomerInformationMapper extends BaseInndooMapper<CustomerInformation> {
    int batchDeleteByIdentifier(List<String> list);
}
