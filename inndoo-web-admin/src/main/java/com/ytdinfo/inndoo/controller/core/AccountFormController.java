package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.DateUtils;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormField;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import com.ytdinfo.inndoo.modules.core.service.AccountFormMetaService;
import com.ytdinfo.inndoo.modules.core.service.AccountFormService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "会员注册页面主信息管理接口")
@RequestMapping("/accountform")
public class AccountFormController extends BaseController<AccountForm, String> {

    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private AccountFormMetaService accountFormMetaService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public AccountFormService getService() {
        return accountFormService;
    }


    @Override
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据")
    @SystemLog(description = "获取全部数据")
    public Result<List<AccountForm>> listAll(){
        String appid =  UserContext.getAppid();
        List<AccountForm> list = getService().findByAppid(appid);
        return new ResultUtil<List<AccountForm>>().setData(list);
    }

    @RequestMapping(value = "/getAccountFormMetasByAccountFormId", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取表单输入信息")
    @SystemLog(description = "获取表单输入信息")
    public Result<List<AccountFormMeta>> getAccountFormMetasByAccountFormId(@RequestParam String accountFormId){
        List<AccountFormMeta> list = accountFormMetaService.findListByAccountFormId(accountFormId);
        list.removeIf(r -> r.getMetaType().equals("agreement"));
        list.removeIf(r -> r.getMetaType().equals("button"));
        list.removeIf(r -> r.getTitle().equals("验证码"));
        return new ResultUtil<List<AccountFormMeta>>().setData(list);
    }

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<AccountForm>> listByCondition(@ModelAttribute AccountForm accountForm,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){
        String appid =  UserContext.getAppid();
        accountForm.setAppid(appid);
        Page<AccountForm> page = accountFormService.findByCondition(accountForm, searchVo, PageUtil.initPage(pageVo));
        for(AccountForm aF: page.getContent()){
            //判断状态
            accountFormService.setActStatus(aF);
            aF.setViewEndDate(DateUtil.format(aF.getEndDate(), "yyyy-MM-dd"));
            aF.setViewStartDate(DateUtil.format(aF.getStartDate(), "yyyy-MM-dd"));
        }
        return new ResultUtil<Page<AccountForm>>().setData(page);
    }


    @RequestMapping(value = "/setDefault", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "设置默认注册表单")
    @SystemLog(description = "设置默认注册表单")
    public Result<AccountForm> setDefault(@ModelAttribute AccountForm accountForm){
        AccountForm accountForm1 = accountFormService.setDefault(accountForm);
        return new ResultUtil<AccountForm>().setData(accountForm1);
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加")
    @SystemLog(description = "添加")
    public Result<AccountForm> add(@RequestBody AccountForm accountForm,HttpServletRequest request){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        if(StrUtil.isNotBlank(accountForm.getViewStartDate())) {
           // accountForm.setStartDate(DateUtil.parse(accountForm.getViewStartDate(), "yyyy-MM-dd"));
            accountForm.setStartDate(DateUtils.parseDate(accountForm.getViewStartDate(),df));
        }
        if(StrUtil.isNotBlank(accountForm.getViewEndDate())) {
         //   accountForm.setEndDate(DateUtil.parse(accountForm.getViewEndDate(), "yyyy-MM-dd"));
            accountForm.setEndDate(DateUtils.parseDate(accountForm.getViewEndDate(),df));
        }
        //全部设置成开启
        accountForm.setEnableCaptcha(true);
        List<AccountFormMeta> accountFormMetas = accountForm.getAccountFormMetas();
        if(null == accountFormMetas || accountFormMetas.size() < 1){
            return new ResultUtil<AccountForm>().setErrorMsg("请加入输入框组件");
        }
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        //去重
        List<AccountFormMeta> uniqueAccountFormMeta = accountFormMetas.parallelStream().distinct()
                .filter(distinctByKey(b -> b.getTitle()))
                .collect(toList());
        if(accountFormMetas.size() != uniqueAccountFormMeta.size()) {
            return new ResultUtil<AccountForm>().setErrorMsg("标题不能重复");
        }
        if(accountForm.getFormType() == 0){
//            String staffNo = "staffNo";
//            List<AccountFormMeta> aFMetas = accountFormMetas.stream().filter(item -> item.getMetaType().equals(staffNo)).collect(Collectors.toList());
//            if(null == aFMetas || aFMetas.size() < 1){
//                return new ResultUtil<AccountForm>().setErrorMsg("员工号注册页必须包含员工号组件");
//            }
        }
        AccountForm accountForm1 = accountFormService.save(accountForm);
        return new ResultUtil<AccountForm>().setData(accountForm1);
    }

    @RequestMapping(value = "/saveIdentifierForm", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "添加身份识别表单")
    public Result<AccountForm> saveIdentifierForm(@RequestBody AccountForm accountForm,HttpServletRequest request){

        List<AccountFormMeta> accountFormMetas = accountForm.getAccountFormMetas();
        if(null == accountFormMetas || accountFormMetas.size() < 1){
            return new ResultUtil<AccountForm>().setErrorMsg("请加入输入框组件");
        }
        //去重
        List<AccountFormMeta> uniqueAccountFormMeta = accountFormMetas.parallelStream().distinct()
                .filter(distinctByKey(b -> b.getTitle()))
                .collect(toList());
        if(accountFormMetas.size() != uniqueAccountFormMeta.size()) {
            return new ResultUtil<AccountForm>().setErrorMsg("标题不能重复");
        }
        accountForm.setIsIdentifierForm(true);
        AccountForm accountForm1 = accountFormService.saveIdentifierForm(accountForm);
        return new ResultUtil<AccountForm>().setData(accountForm1);
    }

    @RequestMapping(value = "/getsaveIdentifierFormMeta", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取身份识别表单控件")
    @SystemLog(description = "获取身份识别表单控件")
    public Result<List<AccountFormMeta>> getsaveIdentifierFormMeta(){
        Boolean isIdentifierForm = true;
        //根据appid设置成身份识别表单的主键
        AccountForm accountForm = getService().findByAppidAndIsIdentifierForm(UserContext.getAppid(),isIdentifierForm);
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        if(null == accountForm || accountForm.getAccountFormMetas().size() <1){
            return new ResultUtil<List<AccountFormMeta>>().setErrorMsg("请先创建初始的身份识别表单");
        }
        List<AccountFormMeta> accountFormMetas = accountForm.getAccountFormMetas();
        for(AccountFormMeta accountFormMeta:accountFormMetas){
            accountFormMeta.setAccountFormId(null);
            accountFormMeta.setId(null);
            accountFormMeta.setSortOrder(null);
        }
        return new ResultUtil<List<AccountFormMeta>>().setData(accountFormMetas);
    }

    @RequestMapping(value = "/getIdentifierForm", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取身份识别表单")
    @SystemLog(description = "获取身份识别表单")
    public Result<AccountForm> getIdentifierForm(){
        Boolean isIdentifierForm = true;
        //根据appid设置成身份识别表单的主键
        AccountForm accountForm = getService().findByAppidAndIsIdentifierForm(UserContext.getAppid(),isIdentifierForm);
        if(null == accountForm || accountForm.getAccountFormMetas().size() <1){
            return new ResultUtil<AccountForm>().setErrorMsg("请先创建初始的身份识别表单");
        }
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        return new ResultUtil<AccountForm>().setData(accountForm);
    }

    @RequestMapping(value = "/enable/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "后台发布")
    @SystemLog(description = "后台发布")
    public Result<Object> enable(@ApiParam("应用唯一id标识") @PathVariable String id){
        AccountForm accountForm = getService().get(id);
        if(accountForm==null){
            return new ResultUtil<Object>().setErrorMsg("通过id获取失败");
        }
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        accountForm.setStatus(CommonConstant.STATUS_APPROVED);
        getService().save(accountForm);
        //手动批量删除缓存
        List<String> redisKeys = new ArrayList<>();
        redisKeys.add("AccountForm::" + id);
        redisTemplate.delete(redisKeys);
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/disable/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "后台下架")
    @SystemLog(description = "后台下架")
    public Result<Object> disable(@ApiParam("应用唯一id标识") @PathVariable String id){
        AccountForm accountForm = getService().get(id);
        if(accountForm==null){
            return new ResultUtil<Object>().setErrorMsg("通过id获取失败");
        }
        if(null == accountForm.getType()){
            Byte type = 0;
            accountForm.setType(type);
        }
        accountForm.setStatus(CommonConstant.STATUS_DISABLE);
        getService().save(accountForm);
        //手动批量删除缓存
        List<String> redisKeys = new ArrayList<>();
        redisKeys.add("AccountForm::" + id);
        redisTemplate.delete(redisKeys);
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/batch_disable/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量下架")
    @SystemLog(description = "通过id批量下架")
    public Result<Object> batchDisableById(@PathVariable String[] ids){
        //手动批量删除缓存
        List<String> redisKeys = new ArrayList<>();
        for(String id:ids){
            AccountForm accountForm = getService().get(id);
            if(accountForm==null){
                return new ResultUtil<Object>().setErrorMsg("通过id获取失败");
            }
            if(null == accountForm.getType()){
                Byte type = 0;
                accountForm.setType(type);
            }
            accountForm.setStatus(CommonConstant.STATUS_DISABLE);
            getService().save(accountForm);
            redisKeys.add("AccountForm::" + id);
        }
        redisTemplate.delete(redisKeys);
        return new ResultUtil<Object>().setSuccessMsg("批量下架数据成功");
    }

    @RequestMapping(value = "/batch_enable/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量发布")
    @SystemLog(description = "通过id批量发布")
    public Result<Object> batchEnableById(@PathVariable String[] ids){
        //手动批量删除缓存
        List<String> redisKeys = new ArrayList<>();
        for(String id:ids){
            AccountForm accountForm = getService().get(id);
            if(accountForm==null){
                return new ResultUtil<Object>().setErrorMsg("通过id获取失败");
            }
            if(null == accountForm.getType()){
                Byte type = 0;
                accountForm.setType(type);
            }
            accountForm.setStatus(CommonConstant.STATUS_APPROVED);
            getService().save(accountForm);
        //    redisKeys.add("AccountForm::" + id);
        }
      //  redisTemplate.delete(redisKeys);
        return new ResultUtil<Object>().setSuccessMsg("批量发布数据成功");
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
