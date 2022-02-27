package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.modules.core.entity.ActivityDataSource;

/**
 * 活动平台数据源接口
 * @author Timmy
 */
public interface IActivityDataSourceService extends IService<ActivityDataSource> {

    boolean updateAsDefault(String id);
}