package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.CustomerInformationExtendMapper;
import com.ytdinfo.inndoo.modules.core.dao.mapper.CustomerInformationMapper;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformationExtend;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ICustomerInformationExtendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
@Slf4j
@Service
public class ICustomerInformationExtendServiceImpl extends BaseServiceImpl<CustomerInformationExtendMapper, CustomerInformationExtend>
        implements ICustomerInformationExtendService {

}
