package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.ActivityDataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 活动平台数据源接口
 * @author Timmy
 */
public interface ActivityDataSourceService extends BaseService<ActivityDataSource,String> {

    /**
    * 多条件分页获取
    * @param activityDataSource
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<ActivityDataSource> findByCondition(ActivityDataSource activityDataSource, SearchVo searchVo, Pageable pageable);
}