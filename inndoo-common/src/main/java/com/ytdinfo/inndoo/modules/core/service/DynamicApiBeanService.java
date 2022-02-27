package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.common.vo.Result;

/**
 * 动态接口接口
 * @author Matao
 */
public interface DynamicApiBeanService {

    Result<Object> getBean(String dynamicApiId);
}