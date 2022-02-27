package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.utils.DynamicCodeUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApi;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApiDetail;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCode;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCodeDetail;
import com.ytdinfo.inndoo.modules.core.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 动态接口接口实现
 *
 * @author Matao
 */
@Slf4j
@Service
public class DynamicApiBeanServiceImpl implements DynamicApiBeanService {

    @Autowired
    private DynamicApiService dynamicApiService;

    @Autowired
    private DynamicApiDetailService dynamicApiDetailService;

    @Autowired
    private DynamicCodeService dynamicCodeService;

    @Autowired
    private DynamicCodeDetailService dynamicCodeDetailService;

    @Override
    public Result<Object> getBean(String dynamicApiId) {
        DynamicApi dynamicApi = dynamicApiService.get(dynamicApiId);
        if (dynamicApi == null) {
            return new ResultUtil<Object>().setErrorMsg("接口不存在");
        }
        DynamicApiDetail dynamicApiDetail = dynamicApiDetailService.findByDynamicApiIdAndVersion(dynamicApi.getId(), dynamicApi.getVersion());
        if (dynamicApiDetail == null) {
            return new ResultUtil<Object>().setErrorMsg("接口配置异常");
        }
        if (dynamicApi.getDynamicCodeIds() != null) {
            dynamicApi.setDynamicCodeIdList(StrUtil.split(dynamicApi.getDynamicCodeIds(), ','));
        }
        if (dynamicApi.getDynamicCodeIdList() != null) {
            for (String dynamicCodeId : dynamicApi.getDynamicCodeIdList()) {
                DynamicCode dynamicCode = dynamicCodeService.get(dynamicCodeId);
                if (dynamicCode != null) {
                    boolean ready = SpringContextUtil.containsBean(dynamicCode.getBeanName());
                    if (ready) {
                        ready = DynamicCodeUtil.checkVersion(dynamicCode.getBeanName(), dynamicCode.getVersion());
                    }
                    if (!ready) {
                        DynamicCodeDetail dynamicCodeDetail = dynamicCodeDetailService.findByDynamicCodeIdAndVersion(dynamicCodeId, dynamicCode.getVersion());
                        if (dynamicCodeDetail != null) {
                            DynamicCodeUtil.getBean(dynamicCode.getBeanName(), dynamicCodeDetail.getCode(), dynamicCode.getVersion());
                        }
                    }
                }
            }
        }
        Object bean = DynamicCodeUtil.getBean(dynamicApi.getBeanName(), dynamicApiDetail.getCode(), dynamicApi.getVersion());
        if (bean instanceof DynamicApiBaseService) {
            return new ResultUtil<Object>().setData(bean);
        }
        return new ResultUtil<Object>().setErrorMsg("非标准接口");
    }

}