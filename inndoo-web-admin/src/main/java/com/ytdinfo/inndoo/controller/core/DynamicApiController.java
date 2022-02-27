package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.utils.DynamicCodeUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zhuzheng
 */
@Slf4j
@RestController
@Api(description = "动态接口管理接口")
@RequestMapping("/dynamicApi")
public class DynamicApiController extends BaseController<DynamicApi, String> {

    @Autowired
    private DynamicApiService dynamicApiService;

    @Autowired
    private DynamicApiDetailService dynamicApiDetailService;

    @Autowired
    private DynamicCodeService dynamicCodeService;

    @Autowired
    private DynamicCodeDetailService dynamicCodeDetailService;

    @Autowired
    private ApiCheckService apiCheckService;

    @Override
    public DynamicApiService getService() {
        return dynamicApiService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<DynamicApi>> listByCondition(@ModelAttribute DynamicApi dynamicApi,
                                                    @ModelAttribute SearchVo searchVo,
                                                    @ModelAttribute PageVo pageVo) {
        dynamicApi.setAppid(UserContext.getAppid());
        Page<DynamicApi> page = dynamicApiService.findByCondition(dynamicApi, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<DynamicApi>>().setData(page);
    }

    @Override
    @RequestMapping(value = "/query/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<DynamicApi> query(@PathVariable String id) {
        DynamicApi entity = getService().get(id);
        DynamicApiDetail detail = dynamicApiDetailService.findByDynamicApiIdAndVersion(entity.getId(), entity.getVersion());
        if (detail != null) {
            entity.setCode(detail.getCode());
        }
        if (entity.getDynamicCodeIds() != null) {
            entity.setDynamicCodeIdList(StrUtil.split(entity.getDynamicCodeIds(), ','));
        }
        return new ResultUtil<DynamicApi>().setData(entity);
    }

    @Override
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @Transactional(rollbackFor = RuntimeException.class)
    public Result<DynamicApi> create(@RequestBody DynamicApi entity) {
        String beanName = entity.getBeanName();
        if (SpringContextUtil.containsBean(beanName)) {
            return new ResultUtil<DynamicApi>().setErrorMsg(beanName + "已存在");
        }
        entity.setVersion("1.0");
        if (entity.getDynamicCodeIdList() != null && entity.getDynamicCodeIdList().size() > 0) {
            entity.setDynamicCodeIds(StrUtil.join(",", entity.getDynamicCodeIdList()));
        } else {
            entity.setDynamicCodeIds("");
        }
        DynamicApi e = getService().save(entity);
        DynamicApiDetail detail = new DynamicApiDetail();
        detail.setDynamicApiId(e.getId());
        detail.setBeanName(e.getBeanName());
        detail.setCode(entity.getCode());
        detail.setVersion(e.getVersion());
        dynamicApiDetailService.save(detail);
        if (entity.getDynamicCodeIdList() != null) {
            for (String dynamicCodeId : entity.getDynamicCodeIdList()) {
                DynamicCode dynamicCode = dynamicCodeService.get(dynamicCodeId);
                if (dynamicCode != null && !SpringContextUtil.containsBean(dynamicCode.getBeanName())) {
                    DynamicCodeDetail dynamicCodeDetail = dynamicCodeDetailService.findByDynamicCodeIdAndVersion(dynamicCodeId, dynamicCode.getVersion());
                    if (dynamicCodeDetail != null) {
                        DynamicCodeUtil.getBean(dynamicCode.getBeanName(), dynamicCodeDetail.getCode(), dynamicCodeDetail.getVersion());
                    }
                }
            }
        }
        Object bean = DynamicCodeUtil.getBean(e.getBeanName(), entity.getCode(),entity.getVersion());
        if (!(bean instanceof DynamicApiBaseService)) {
            throw new InndooException("bean必须实现DynamicApiBaseService接口");
        }
        return new ResultUtil<DynamicApi>().setData(e);
    }

    @Override
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "更新数据")
    @Transactional(rollbackFor = RuntimeException.class)
    public Result<DynamicApi> update(@RequestBody DynamicApi entity) {
        String version = entity.getVersion();
        String[] versions = version.split("\\.");
        Integer mainVersion = Integer.parseInt(versions[0]);
        if (entity.getNewMainVersion()) {
            entity.setVersion((mainVersion + 1) + ".0");
        } else {
            Integer subVersion = Integer.parseInt(versions[1]);
            entity.setVersion(mainVersion + "." + (subVersion + 1));
        }
        if (entity.getDynamicCodeIdList() != null && entity.getDynamicCodeIdList().size() > 0) {
            entity.setDynamicCodeIds(StrUtil.join(",", entity.getDynamicCodeIdList()));
        } else {
            entity.setDynamicCodeIds("");
        }
        DynamicApi e = getService().update(entity);
        DynamicApiDetail detail = new DynamicApiDetail();
        detail.setDynamicApiId(e.getId());
        detail.setBeanName(e.getBeanName());
        detail.setCode(entity.getCode());
        detail.setVersion(e.getVersion());
        dynamicApiDetailService.save(detail);
        if (entity.getDynamicCodeIdList() != null) {
            for (String dynamicCodeId : entity.getDynamicCodeIdList()) {
                DynamicCode dynamicCode = dynamicCodeService.get(dynamicCodeId);
                if (dynamicCode != null && !SpringContextUtil.containsBean(dynamicCode.getBeanName())) {
                    DynamicCodeDetail dynamicCodeDetail = dynamicCodeDetailService.findByDynamicCodeIdAndVersion(dynamicCodeId, dynamicCode.getVersion());
                    if (dynamicCodeDetail != null) {
                        DynamicCodeUtil.getBean(dynamicCode.getBeanName(), dynamicCodeDetail.getCode(), dynamicCodeDetail.getVersion());
                    }
                }
            }
        }
        Object bean = DynamicCodeUtil.getBean(e.getBeanName(), entity.getCode(), entity.getVersion());
        if (!(bean instanceof DynamicApiBaseService)) {
            throw new InndooException("bean必须实现DynamicApiBaseService接口");
        }
        List<ApiCheck> apiCheckList = apiCheckService.findByDynamicApiIdAndIsDeleted(entity.getId(), false);
        if (apiCheckList != null && apiCheckList.size() > 0) {
            for (ApiCheck apiCheck : apiCheckList) {
                apiCheck.setReturnType(entity.getReturnType());
                apiCheckService.update(apiCheck);
            }
        }
        return new ResultUtil<DynamicApi>().setData(e);
    }

}


