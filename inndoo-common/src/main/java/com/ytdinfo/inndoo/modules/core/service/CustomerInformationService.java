package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 客户信息表接口
 * @author yaochangning
 */
public interface CustomerInformationService extends BaseService<CustomerInformation,String> {

    /**
    * 多条件分页获取
    * @param customerInformation
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<CustomerInformation> findByCondition(CustomerInformation customerInformation, SearchVo searchVo, Pageable pageable);

    /**
     * 获取获取客户信息拓展字段
     * @return
     */
    List<DictData> findDictData();

    CustomerInformation findByIdentifier(String identifier);

    Result<String> getIdentifier(CustomerInformation customerInformation);

    List<CustomerInformation> findBatchByfindByIdentifiers(List<String> identifiers,int num);
}