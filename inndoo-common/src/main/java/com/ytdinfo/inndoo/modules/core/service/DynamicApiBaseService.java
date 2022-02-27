package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.dto.DynamicApiDto;

/**
 *
 * @author zhuzheng
 */
@FunctionalInterface
public interface DynamicApiBaseService<T>{

    Result<T> getValue(DynamicApiDto dto);

}