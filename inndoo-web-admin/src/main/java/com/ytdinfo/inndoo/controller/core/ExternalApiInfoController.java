package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.enums.SourceType;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.ExternalAPIResultVo;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.ExternalApiInfo;
import com.ytdinfo.inndoo.modules.core.service.ExternalApiInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yaochangning
 */
@Slf4j
@RestController
@Api(description = "外部接口调用定义表管理接口")
@RequestMapping("/externalapiinfo")
public class ExternalApiInfoController extends BaseController<ExternalApiInfo, String> {

    @Autowired
    private ExternalApiInfoService externalApiInfoService;

    @Override
    public ExternalApiInfoService getService() {
        return externalApiInfoService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<ExternalApiInfo>> listByCondition(@ModelAttribute ExternalApiInfo externalApiInfo,
                                                         @ModelAttribute SearchVo searchVo,
                                                         @ModelAttribute PageVo pageVo) {

        Page<ExternalApiInfo> page = externalApiInfoService.findByCondition(externalApiInfo, searchVo, PageUtil.initPage(pageVo));
        for (ExternalApiInfo ex : page.getContent()) {
            if (null != ex.getSource()) {
                SourceType sourceType = SourceType.valueOf(ex.getSource());
                ex.setSourceName(sourceType.getDisplayName());
            }
        }
        return new ResultUtil<Page<ExternalApiInfo>>().setData(page);
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量删除")
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids) {

        for (String id : ids) {
            getService().delete(id);
        }
        return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
    }

    @RequestMapping(value = "/debug", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "测试参数")
    public Result<Object> debug(String id, String accountId, String ext) {
        if (StrUtil.isNotEmpty(ext) && ext.contains("{")) {
            ext = HexUtil.decodeHexStr(ext);
        }
        Result<Object> execute = externalApiInfoService.execute(id, accountId, "", true);
        return new ResultUtil<>().setSuccessMsg(JSONUtil.toJsonStr(execute));
    }
}
