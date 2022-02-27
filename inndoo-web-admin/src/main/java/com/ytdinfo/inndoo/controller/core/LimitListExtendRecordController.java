package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.LimitListExtendRecord;
import com.ytdinfo.inndoo.modules.core.service.LimitListExtendRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "受限名单扩展信息清单管理接口")
@RequestMapping("/limitlistextendrecord")
public class LimitListExtendRecordController extends BaseController<LimitListExtendRecord, String> {

    @Autowired
    private LimitListExtendRecordService limitListExtendRecordService;

    @Override
    public LimitListExtendRecordService getService() {
        return limitListExtendRecordService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<LimitListExtendRecord>> listByCondition(@ModelAttribute LimitListExtendRecord limitListExtendRecord,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){
        String appid = UserContext.getAppid();
        limitListExtendRecord.setAppid(appid);
        Page<LimitListExtendRecord> page = limitListExtendRecordService.findByCondition(limitListExtendRecord, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<LimitListExtendRecord>>().setData(page);
    }

}
