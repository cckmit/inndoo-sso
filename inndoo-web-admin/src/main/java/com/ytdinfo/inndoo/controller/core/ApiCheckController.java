package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.core.entity.ApiCheck;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApi;
import com.ytdinfo.inndoo.modules.core.service.ApiCheckService;
import com.ytdinfo.inndoo.modules.core.service.DynamicApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Api(description = "接口校验管理接口")
@RequestMapping("/apiCheck")
public class ApiCheckController extends BaseController<ApiCheck, String> {

    @Autowired
    private ApiCheckService apiCheckService;

    @Autowired
    private DynamicApiService dynamicApiService;
    @Autowired
    private ActivityApiUtil activityApiUtil;
    @Override
    public ApiCheckService getService() {
        return apiCheckService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<ApiCheck>> listByCondition(@ModelAttribute ApiCheck apiCheck,
                                                  @ModelAttribute SearchVo searchVo,
                                                  @ModelAttribute PageVo pageVo) {
        apiCheck.setAppid(UserContext.getAppid());
        Page<ApiCheck> page = apiCheckService.findByCondition(apiCheck, searchVo, PageUtil.initPage(pageVo));
        List<ApiCheck> list = page.getContent();
        for (ApiCheck item : list) {
            if (StrUtil.isNotEmpty(item.getDynamicApiId())) {
                DynamicApi dynamicApi = dynamicApiService.get(item.getDynamicApiId());
                if (dynamicApi != null) {
                    item.setDynamicApiName(dynamicApi.getName());
                }
            }
        }
        return new ResultUtil<Page<ApiCheck>>().setData(page);
    }

    @Override
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    public Result<ApiCheck> create(@ModelAttribute ApiCheck entity) {
        DynamicApi dynamicApi = dynamicApiService.get(entity.getDynamicApiId());
        if (dynamicApi != null) {
            entity.setReturnType(dynamicApi.getReturnType());
        }
        ApiCheck e = getService().save(entity);
        return new ResultUtil<ApiCheck>().setData(e);
    }

    @Override
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "更新数据")
    public Result<ApiCheck> update(@ModelAttribute ApiCheck entity) {
        DynamicApi dynamicApi = dynamicApiService.get(entity.getDynamicApiId());
        if (dynamicApi != null) {
            entity.setReturnType(dynamicApi.getReturnType());
        }
        ApiCheck e = getService().update(entity);
        return new ResultUtil<ApiCheck>().setData(e);
    }

    @RequestMapping(value = "/test", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    public Result<ApiCheckTestVo> test(@RequestBody ApiCheckTestVo entity) {
        ApiCheck apiCheck = apiCheckService.get(entity.getId());
        if (apiCheck != null) {
            if(StrUtil.isNotBlank(entity.getActAccountId())){
                ActAccountVo actAccountVo = activityApiUtil.getByActAccountId(UserContext.getTenantId(),UserContext.getAppid(),AESUtil.encrypt(entity.getActAccountId(),AESUtil.WXLOGIN_PASSWORD));
                if(null != actAccountVo && null != actAccountVo.getAccountType()){
                    entity.setAccountType(actAccountVo.getAccountType());
                    if(null != actAccountVo.getAccountType() & actAccountVo.getAccountType().intValue()!= 1){
                        if(StrUtil.isNotBlank(actAccountVo.getOpenId())){
                            entity.setOpenId(actAccountVo.getOpenId());
                        }
                    }
                }
            }
            Result<Object> result = dynamicApiService.getValue(apiCheck, entity.getActAccountId(), entity.getAccountId(), entity.getOpenId(),entity.getAccountType());
            entity.setResponse(JSONUtil.toJsonStr(result));
            return new ResultUtil<ApiCheckTestVo>().setData(entity);
        }
        return new ResultUtil<ApiCheckTestVo>().setErrorMsg("接口校验配置不存在");
    }

}
