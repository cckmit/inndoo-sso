package com.ytdinfo.inndoo.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.DIApiConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.dto.DIResult;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.BulkheadContainter;
import com.ytdinfo.inndoo.common.utils.DIApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.util.MD5Util;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Slf4j
@RestController
@Api(description = "标签接口")
@RequestMapping("/tag")
@APIModifier(APIModifierType.PRIVATE)
public class TagsController {

    @Autowired
    private DIApiUtil diApiUtil;

    @Autowired
    private AccountService accountService;

    @XxlConf("core.sm3.appid")
    private String appIds;

    /**
     * 使用openId作为标签获取参数
     */
    @XxlConf("core.di.appid.openid")
    private String openIdAppIds;

    @RequestMapping(value = "/token/cache/clean", method = RequestMethod.GET)
    @ApiOperation(value = "清除token缓存")
    @ResponseBody
    public Result<String> cleanTokenCache(HttpServletRequest request) {
        diApiUtil.cleanTokenCache();
        return new ResultUtil<String>().setSuccessMsg("ok");
    }

    /**
     * @param pid   父级分组id，顶级传-1
     * @return
     */
    @RequestMapping(value = "/list/info", method = RequestMethod.GET)
    @ApiOperation(value = "获取标签列表")
    @ResponseBody
    public Result<List<Map<String, Object>>> getTagList(@RequestParam(defaultValue = "-1") Integer pid, HttpServletRequest request) {
        DIResult result = diApiUtil.getTagList(UserContext.getTenantId(),pid);
        if(Objects.isNull(result)){
            return new ResultUtil<List<Map<String, Object>>>().setErrorMsg("请求失败");
        }
        if(result.success()){
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.getData();
            return new ResultUtil<List<Map<String, Object>>>().setData(dataList);
        }
        return new ResultUtil<List<Map<String, Object>>>().setErrorMsg(result.getErrorMsg());
    }

    /**
     * @param tagId   标签id
     * @return
     */
    @RequestMapping(value = "/value/enum", method = RequestMethod.GET)
    @ApiOperation(value = "获取标签的枚举值")
    @ResponseBody
    public Result<Object> getTagValueEnum(@RequestParam Integer tagId, HttpServletRequest request) {
        DIResult result = diApiUtil.getTagValueEnum(tagId);
        if(Objects.isNull(result)){
            return new ResultUtil<Object>().setErrorMsg("请求失败");
        }
        if(result.success()){
            return new ResultUtil<Object>().setData(result.getData());
        }
        return new ResultUtil<Object>().setErrorMsg(result.getErrorMsg());
    }

    /**
     * @param tagId   标签id
     * @param accountId
     * @return
     */
    @RequestMapping(value = "/value/getOld", method = RequestMethod.GET)
    @ApiOperation(value = "获取缓存中标签的值")
    @ResponseBody
    public Result<Object> getTagUserValueOld(@RequestParam Integer tagId,@RequestParam String accountId, HttpServletRequest request) {
        Account account = accountService.get(accountId);
        if(account==null){
            return new ResultUtil<Object>().setErrorMsg("用户不存在");
        }
        if(StrUtil.isEmpty(account.getPhone())){
            return new ResultUtil<Object>().setErrorMsg("用户标识不存在");
        }
        String md5Phone = account.getMd5Phone();
        if(StrUtil.isEmpty(md5Phone)){
            md5Phone = MD5Util.md5(account.getPhone());
            account.setMd5Phone(md5Phone);
            accountService.update(account);
        }
        DIResult result = diApiUtil.getTagUserValue(tagId,md5Phone);
        if(Objects.isNull(result)){
            return new ResultUtil<Object>().setErrorMsg("请求失败");
        }
        if(result.success()){
            return new ResultUtil<Object>().setData(result.getData());
        }
        return new ResultUtil<Object>().setErrorMsg(result.getErrorMsg());
    }

    /**
     * @param tagId   标签id
     * @param accountId
     * @return
     */
    @RequestMapping(value = "/value/empty", method = RequestMethod.GET)
    @ApiOperation(value = "获取缓存中标签的值")
    @ResponseBody
    public Result<Object> empty(@RequestParam Integer tagId,@RequestParam String accountId, HttpServletRequest request) {
        ThreadPoolBulkhead poolBulkhead = BulkheadContainter.get("tag");
        Supplier<CompletionStage<Result>> completionStageSupplier = ThreadPoolBulkhead.decorateSupplier(poolBulkhead, () -> {
            try {
                Thread.sleep(1);
            }catch (InterruptedException e) {

            }
            return new ResultUtil<String>().setData("OK");
        });
        Try<CompletionStage<Result>> completionStages = Try.ofSupplier(completionStageSupplier);
        try {
            return completionStages.get().toCompletableFuture().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @param tagId   标签id
     * @param accountId
     * @return
     */
    @RequestMapping(value = "/value/get", method = RequestMethod.GET)
    @ApiOperation(value = "获取缓存中标签的值")
    @ResponseBody
    public Result<Object> getTagUserValue(@RequestParam Integer tagId,@RequestParam String accountId,@RequestParam(required = false) String openId, HttpServletRequest request) {
        DIResult result = null;
        //天津中行手机银行用户 使用openId获取标签
        if (StrUtil.isNotEmpty(openIdAppIds) && StrUtil.isNotEmpty(UserContext.getAppid())
                && openIdAppIds.contains(UserContext.getAppid()) && StrUtil.isNotEmpty(openId)) {
            result = diApiUtil.getTagUserValueBulkHead(tagId,openId);
        } else {
            Account account = accountService.get(accountId);
            if (account == null) {
                return new ResultUtil<Object>().setErrorMsg("用户不存在");
            }
            if (StrUtil.isEmpty(account.getPhone())) {
                return new ResultUtil<Object>().setErrorMsg("用户标识不存在");
            }
            String md5Phone = account.getMd5Phone();
            if (StrUtil.isEmpty(md5Phone)) {
                md5Phone = MD5Util.md5(account.getPhone());
                account.setMd5Phone(md5Phone);
                accountService.update(account);
            }
            if (StrUtil.isNotEmpty(appIds) && appIds.contains(account.getAppid())) {
                md5Phone = SmUtil.sm3("YTD" + account.getPhone()).toUpperCase(Locale.ROOT);
            }
            result = diApiUtil.getTagUserValueBulkHead(tagId, md5Phone);
        }

        if(Objects.isNull(result)){
            return new ResultUtil<Object>().setErrorMsg("请求失败");
        }
        if(result.success()){
            return new ResultUtil<Object>().setData(result.getData());
        }
        return new ResultUtil<Object>().setErrorMsg(result.getErrorMsg());
    }

    /**
     * @param tagId   标签id
     * @param expire   失效时间，格式：yyyy-MM-dd HH:mm:ss
     * @return
     */
    @RequestMapping(value = "/expire/set", method = RequestMethod.POST)
    @ApiOperation(value = "设置标签的失效时间")
    @ResponseBody
    public Result<Object> setTagExpireTime(@RequestParam Integer tagId,@RequestParam String expire, HttpServletRequest request) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date expireDate = dateFormat.parse(expire);
        } catch (ParseException e) {
            return new ResultUtil<Object>().setErrorMsg(400,"日期格式错误，参考格式:yyyy-MM-dd HH:mm:ss");
        }
        DIResult result = diApiUtil.setTagExpireTime(tagId,expire);
        if(Objects.isNull(result)){
            return new ResultUtil<Object>().setErrorMsg("请求失败");
        }
        if(result.success()){
            return new ResultUtil<Object>().setData(result.getData());
        }
        return new ResultUtil<Object>().setErrorMsg(result.getErrorMsg());
    }

    /**
     * @param tagId   标签id
     * @return
     */
    @RequestMapping(value = "/cache/refresh", method = RequestMethod.GET)
    @ApiOperation(value = "更新标签缓存")
    @ResponseBody
    public Result<Object> refreshTagCache(@RequestParam Integer tagId,HttpServletRequest request) {

        DIResult result = diApiUtil.refreshTagCache(tagId);
        if(Objects.isNull(result)){
            return new ResultUtil<Object>().setErrorMsg("请求失败");
        }
        if(result.success()){
            return new ResultUtil<Object>().setData(result.getData());
        }
        return new ResultUtil<Object>().setErrorMsg(result.getErrorMsg());
    }

    /**
     * @param tagId   标签id
     * @return
     */
    @RequestMapping(value = "/cache/progress", method = RequestMethod.GET)
    @ApiOperation(value = "获取缓存更新情况")
    @ResponseBody
    public Result<Object> getTagCacheProgress(@RequestParam Integer tagId,HttpServletRequest request) {
        DIResult result = diApiUtil.getTagCacheProgress(tagId);
        if(Objects.isNull(result)){
            return new ResultUtil<Object>().setErrorMsg("请求失败");
        }
        if(result.success()){
            return new ResultUtil<Object>().setData(result.getData());
        }
        return new ResultUtil<Object>().setErrorMsg(result.getErrorMsg());
    }


}
