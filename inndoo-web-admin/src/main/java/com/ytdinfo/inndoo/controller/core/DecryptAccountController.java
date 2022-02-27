package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.DecryptAccount;
import com.ytdinfo.inndoo.modules.core.service.DecryptAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author cnyao
 */
@Slf4j
@RestController
@Api(description = "账户解密信息管理接口")
@RequestMapping("/decryptaccount")
public class DecryptAccountController extends BaseController<DecryptAccount, String> {

    @Autowired
    private DecryptAccountService decryptAccountService;

    @Override
    public DecryptAccountService getService() {
        return decryptAccountService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<DecryptAccount>> listByCondition(@ModelAttribute DecryptAccount decryptAccount,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<DecryptAccount> page = decryptAccountService.findByCondition(decryptAccount, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<DecryptAccount>>().setData(page);
    }

}
