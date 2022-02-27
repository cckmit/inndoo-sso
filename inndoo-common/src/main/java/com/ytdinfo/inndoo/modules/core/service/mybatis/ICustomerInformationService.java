package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformation;

import java.util.List;

public interface ICustomerInformationService extends BaseIService<CustomerInformation> {
    int deleteBatchByIdentifers(List<String> deleteIdentifers, int i);

}
