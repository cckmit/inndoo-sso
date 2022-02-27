package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformationExtend;

import java.util.List;

/**
 * 客户信息拓展表数据处理层
 * @author yaochangning
 */
public interface CustomerInformationExtendDao extends BaseDao<CustomerInformationExtend,String> {

    List<CustomerInformationExtend> findByCustomerInformationIdAndAppid(String customerInformationId,String appid);

    Integer deleteByCustomerInformationId(String customerInformationId);
}