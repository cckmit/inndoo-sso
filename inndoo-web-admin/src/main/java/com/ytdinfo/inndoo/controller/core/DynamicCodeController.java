package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.DynamicCodeUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApi;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApiDetail;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCode;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCodeDetail;
import com.ytdinfo.inndoo.modules.core.service.DynamicApiDetailService;
import com.ytdinfo.inndoo.modules.core.service.DynamicApiService;
import com.ytdinfo.inndoo.modules.core.service.DynamicCodeDetailService;
import com.ytdinfo.inndoo.modules.core.service.DynamicCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @author zhuzheng
 */
@Slf4j
@RestController
@Api(description = "动态代码管理接口")
@RequestMapping("/dynamicCode")
public class DynamicCodeController extends BaseController<DynamicCode, String> {

    @Autowired
    private DynamicCodeService dynamicCodeService;

    @Autowired
    private DynamicCodeDetailService dynamicCodeDetailService;

    @Autowired
    private DynamicApiService dynamicApiService;

    @Autowired
    private DynamicApiDetailService dynamicApiDetailService;

    @Resource
    private DynamicApiController dynamicApiController;

    @Override
    public DynamicCodeService getService() {
        return dynamicCodeService;
    }

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<DynamicCode>> listByCondition(@ModelAttribute DynamicCode dynamicCode,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){
        dynamicCode.setAppid(UserContext.getAppid());
        Page<DynamicCode> page = dynamicCodeService.findByCondition(dynamicCode, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<DynamicCode>>().setData(page);
    }

    @Override
    @RequestMapping(value = "/query/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    public Result<DynamicCode> query(@PathVariable String id) {
        DynamicCode entity = getService().get(id);
        DynamicCodeDetail detail = dynamicCodeDetailService.findByDynamicCodeIdAndVersion(entity.getId(), entity.getVersion());
        if (detail != null) {
            entity.setCode(detail.getCode());
        }
        return new ResultUtil<DynamicCode>().setData(entity);
    }

    @Override
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @Transactional(rollbackFor = RuntimeException.class)
    public Result<DynamicCode> create(@RequestBody DynamicCode entity) {
        String beanName = entity.getBeanName();
        if(SpringContextUtil.containsBean(beanName)){
            return new ResultUtil<DynamicCode>().setErrorMsg(beanName+"已存在");
        }
        entity.setVersion("1.0");
        DynamicCode e = getService().save(entity);
        DynamicCodeDetail detail = new DynamicCodeDetail();
        detail.setDynamicCodeId(e.getId());
        detail.setBeanName(e.getBeanName());
        detail.setCode(entity.getCode());
        detail.setVersion(e.getVersion());
        dynamicCodeDetailService.save(detail);
        DynamicCodeUtil.getBean(e.getBeanName(),entity.getCode(), entity.getVersion());
        return new ResultUtil<DynamicCode>().setData(e);
    }

    @Override
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "更新数据")
    @Transactional(rollbackFor = RuntimeException.class)
    public Result<DynamicCode> update(@RequestBody DynamicCode entity) {
        String version = entity.getVersion();
        String[] versions = version.split("\\.");
        Integer mainVersion = Integer.parseInt(versions[0]);
        if (entity.getNewMainVersion()) {
            entity.setVersion((mainVersion + 1) + ".0");
        } else {
            Integer subVersion = Integer.parseInt(versions[1]);
            entity.setVersion(mainVersion + "." + (subVersion + 1));
        }
        DynamicCode e = getService().update(entity);
        DynamicCodeDetail detail = new DynamicCodeDetail();
        detail.setDynamicCodeId(e.getId());
        detail.setBeanName(e.getBeanName());
        detail.setCode(entity.getCode());
        detail.setVersion(e.getVersion());
        dynamicCodeDetailService.save(detail);
        DynamicCodeUtil.getBean(e.getBeanName(),entity.getCode(), entity.getVersion());

        List<DynamicApi> dynamicApiList = dynamicApiService.findByDynamicCodeIdsLike(e.getId());
        for (DynamicApi dynamicApi:dynamicApiList) {
            dynamicApi.setNewMainVersion(true);
            dynamicApi.setDynamicCodeIdList(Arrays.asList(dynamicApi.getDynamicCodeIds().split(",")));
            DynamicApiDetail apiDetail = dynamicApiDetailService.findByDynamicApiIdAndVersion(dynamicApi.getId(),dynamicApi.getVersion());
            dynamicApi.setCode(apiDetail.getCode());
            dynamicApiController.update(dynamicApi);
        }

        return new ResultUtil<DynamicCode>().setData(e);
    }

}
