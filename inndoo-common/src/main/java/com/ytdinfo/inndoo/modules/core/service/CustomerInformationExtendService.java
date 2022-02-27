package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformationExtend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 客户信息拓展表接口
 * @author yaochangning
 */
public interface CustomerInformationExtendService extends BaseService<CustomerInformationExtend,String> {

    /**
    * 多条件分页获取
    * @param customerInformationExtend
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<CustomerInformationExtend> findByCondition(CustomerInformationExtend customerInformationExtend, SearchVo searchVo, Pageable pageable);

    List<CustomerInformationExtend> findByCustomerInformationId(String customerInformationId);

    Integer deleteByCustomerInformationId(String customerInformationId);
}