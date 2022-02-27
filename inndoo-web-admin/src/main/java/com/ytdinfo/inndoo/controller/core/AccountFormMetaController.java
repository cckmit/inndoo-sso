package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import com.ytdinfo.inndoo.modules.core.service.AccountFormMetaService;
import com.ytdinfo.inndoo.modules.core.service.AccountFormService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "动态表单控件配置信息管理接口")
@RequestMapping("/formmeta")
public class AccountFormMetaController extends BaseController<AccountFormMeta, String> {

    @Autowired
    private AccountFormMetaService formMetaDataService;

    @Autowired
    private AccountFormService accountFormService;

    @Override
    public AccountFormMetaService getService() {
        return formMetaDataService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<AccountFormMeta>> listByCondition(@ModelAttribute AccountFormMeta formMetaData,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<AccountFormMeta> page = formMetaDataService.findByCondition(formMetaData, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<AccountFormMeta>>().setData(page);
    }

    @RequestMapping(value = "/queryformmetalistByAccountFormId/{accountFormId}")
    @ApiOperation(value = "获取动态表单控件配置信息")
    @SystemLog(description = "获取动态表单控件配置信息")
    public Result<List<AccountFormMeta>> queryformmetalist(@PathVariable String accountFormId) {
        List<AccountFormMeta> formMetaList = formMetaDataService.findListByAccountFormId(accountFormId);
        AccountForm accountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(),true);
        List<AccountFormMeta> accountFormMetas = accountForm.getAccountFormMetas();
        for(AccountFormMeta accountFormMeta: formMetaList){

            List<AccountFormMeta> selectaccountFormMetas = accountFormMetas.stream().filter(item -> item.getMetaType().equals(accountFormMeta.getMetaType())).collect(Collectors.toList());
            if(CollectionUtil.isNotEmpty(selectaccountFormMetas)){
                accountFormMeta.setIsIdentifier(true);
            }else {
                accountFormMeta.setIsIdentifier(false);
            }

        }
        return new ResultUtil<List<AccountFormMeta>>().setData(formMetaList);
    }

    @RequestMapping(value = "/queryformmetaById/{id}")
    @ApiOperation(value = "获取动态表单控件配置信息")
    @SystemLog(description = "获取动态表单控件配置信息")
    public Result<AccountFormMeta> queryformmeta(@PathVariable String id) {
        AccountFormMeta formMeta =  formMetaDataService.get(id);
        return new ResultUtil<AccountFormMeta>().setData(formMeta);
    }
}
