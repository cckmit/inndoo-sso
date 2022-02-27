package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.apiadapter.APIRequest;
import com.ytdinfo.inndoo.apiadapter.APIResponse;
import com.ytdinfo.inndoo.apiadapter.BaseRequest;
import com.ytdinfo.inndoo.apiadapter.ConvertRule;

/**
 * @author zhuzheng
 * @desc 包装com.ytdinfo.inndoo.apiadapter.APIAdapter以便SPRING_AOP
 */
public interface APIAdapterService<I extends BaseRequest, O extends ConvertRule> {

    APIResponse<O> request(APIRequest<I> request, Class<O> clazz);

}