package com.ytdinfo.inndoo.modules.core.serviceimpl;

import com.ytdinfo.inndoo.apiadapter.*;
import com.ytdinfo.inndoo.modules.core.service.APIAdapterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class APIAdapterServiceImpl<I extends JsonRequest, O extends ConvertRule> implements APIAdapterService<I, O> {

    @Override
    public APIResponse<O> request(APIRequest<I> request, Class<O> clazz) {
        return APIAdapter.request(request,clazz);
    }

}
