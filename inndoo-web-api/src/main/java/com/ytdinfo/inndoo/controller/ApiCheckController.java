package com.ytdinfo.inndoo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.BulkheadContainter;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListRecord;
import com.ytdinfo.inndoo.modules.core.entity.ApiCheck;
import com.ytdinfo.inndoo.modules.core.entity.DynamicApi;
import com.ytdinfo.inndoo.modules.core.service.AchieveListRecordService;
import com.ytdinfo.inndoo.modules.core.service.ApiCheckService;
import com.ytdinfo.inndoo.modules.core.service.DynamicApiService;
import com.ytdinfo.inndoo.modules.core.service.ExternalApiInfoService;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Slf4j
@RestController
@Api(description = "接口校验管理接口")
@RequestMapping("/apiCheck")
@APIModifier(APIModifierType.PUBLIC)
public class ApiCheckController {

    @Autowired
    private ApiCheckService apiCheckService;

    @Autowired
    private DynamicApiService dynamicApiService;

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private AchieveListRecordService achieveListRecordService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiOperation(value = "接口校验管理列表")
    @SystemLog(description = "接口校验管理列表")
    public Result<List<ApiCheck>> list() {
        return new ResultUtil<List<ApiCheck>>().setData(apiCheckService.findByAppid(UserContext.getAppid()));
    }

    @RequestMapping(value = "/value", method = RequestMethod.GET)
    @ApiOperation(value = "获取接口校验值")
    @SystemLog(description = "获取接口校验值")
    public Result<Object> value(@RequestParam String apiCheckId, String actAccountId, String coreAccountId, String openId,@RequestParam(required = false) Byte accountType) {
        ApiCheck apiCheck = apiCheckService.get(apiCheckId);
        if (apiCheck != null) {
            ThreadPoolBulkhead poolBulkhead = BulkheadContainter.get("apicheck");
            if(poolBulkhead == null) {
                return dynamicApiService.getValue(apiCheck, actAccountId, coreAccountId, openId,accountType);
            }
            Supplier<CompletionStage<Result<Object>>> completionStageSupplier = ThreadPoolBulkhead.decorateSupplier(poolBulkhead, () -> dynamicApiService.getValue(apiCheck, actAccountId, coreAccountId, openId,accountType));
            Try<CompletionStage<Result<Object>>> completionStages = Try.ofSupplier(completionStageSupplier);
            try {
                return completionStages.get().toCompletableFuture().get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            //return dynamicApiService.getValue(apiCheck, actAccountId, coreAccountId, openId);
        }
        return new ResultUtil<Object>().setErrorMsg("接口校验配置不存在");
    }

    @RequestMapping(value = "/hotfix", method = RequestMethod.GET)
    @ApiOperation(value = "模拟重推达标名单")
    @SystemLog(description = "模拟重推达标名单")
    public Result<Object> hotfix(@RequestParam String openIds, @RequestParam String achieveListId) {
        String[] openIdList = openIds.split(",");
        for (String openId : openIdList) {
            AchieveListRecord achieveListRecord = achieveListRecordService.findByListIdAndIdentifier(achieveListId, openId);
            if (!BeanUtil.isEmpty(achieveListRecord)) {
                //发送mq达标用户导入后推送到act用户
                MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<AchieveListRecord>();
                mqMessageAchieveListRecord.setAppid(UserContext.getAppid());
                mqMessageAchieveListRecord.setTenantId(UserContext.getTenantId());
                mqMessageAchieveListRecord.setContent(achieveListRecord);
                rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);
            }
        }
        return new ResultUtil<Object>().setSuccessMsg("处理成功");
    }

}
