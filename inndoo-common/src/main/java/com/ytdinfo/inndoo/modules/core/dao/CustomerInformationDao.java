package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformation;

import java.util.List;

/**
 * 客户信息表数据处理层
 * @author yaochangning
 */
public interface CustomerInformationDao extends BaseDao<CustomerInformation,String> {
    CustomerInformation findByIdentifierAndAppid(String identifier ,String appid);

    List<CustomerInformation> findByAppidAndIdentifierIn(String appid,List<String> identifiers);
}