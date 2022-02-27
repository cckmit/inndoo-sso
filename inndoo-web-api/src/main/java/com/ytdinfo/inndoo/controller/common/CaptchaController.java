package com.ytdinfo.inndoo.controller.common;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.ApiCostTypeConstant;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.base.vo.SmsSetting;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import com.ytdinfo.inndoo.modules.core.entity.SmsCaptchaLog;
import com.ytdinfo.inndoo.modules.core.service.SmsCaptchaLogService;
import com.ytdinfo.inndoo.modules.core.serviceimpl.SmsCaptchaLogServiceImpl;
import com.ytdinfo.model.response.SendSMSResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "验证码接口")
@RequestMapping("/base/common/captcha")

@APIModifier(APIModifierType.PUBLIC)
public class CaptchaController {

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SettingUtil settingUtil;

    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @XxlConf("core.inndoo.urlprefix")
    private String urlprefix;

    @XxlConf("activity.classicactivity.aes.password")
    private String aesKeyV1;

    @Autowired
    private SmsSetting smsSetting;

    @Autowired
    private DictDataService dictDataService;

    @Autowired
    private SmsCaptchaLogService smsCaptchaLogService;

    @RequestMapping(value = "/sendAccountSmsBy/{mobile}", method = RequestMethod.GET)
    @ApiOperation(value = "发送短信验证码")
    public Result<Object> sendAccountSms(@PathVariable String mobile,
                                         @RequestParam String ticket, HttpServletRequest request) {
        //后台调用验证码接口校验
        boolean check = MatrixApiUtil.check(ticket, urlprefix);
        if (!check) {
            return new ResultUtil<Object>().setErrorMsg("非法的请求！");
        }
        // 生成6位数验证码
        String code = new CreateVerifyCode().getRandomNum();
        // 缓存验证码
        redisTemplate.opsForValue().set(CommonConstant.PRE_SMS + mobile, code, 5L, TimeUnit.MINUTES);
        // 添加短信验证码记录
        SmsCaptchaLog log = new SmsCaptchaLog();
        log.setAppid(UserContext.getAppid());
        log.setTenantId(UserContext.getTenantId());
        log.setPhone(AESUtil.encrypt(mobile));
        log.setSendStatus(0);
        Date date = new Date();
        log.setCreateTime(date);
        log.setUpdateTime(date);
        log.setCode(code);
        smsCaptchaLogService.save(log);
        // 发送验证码
        try {

            String captchaTemplate = smsSetting.getCaptchaTemplate();
            String smsSignature = dictDataService.findSmsSignatureByAppid(UserContext.getAppid());
            if (StrUtil.isEmpty(smsSignature)) {
                smsSignature = "【盈天地】";
            }
            String content = smsSignature + StrUtil.replace(captchaTemplate, "${code}", code);
            SendSMSResponse response = smsUtil.sendCode(mobile, content, "-1");
            if (response.isSuccess()) {
                PhoneLocation phoneLocation = new PhoneLocation();
                phoneLocation.setPhone(mobile);
                MQMessage<PhoneLocation> mqMessagePhoneLocation = new MQMessage<>();
                mqMessagePhoneLocation.setAppid(UserContext.getAppid());
                mqMessagePhoneLocation.setTenantId(UserContext.getTenantId());
                mqMessagePhoneLocation.setContent(phoneLocation);
                rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_PHONE_LOCATION_EVENT_MSG), mqMessagePhoneLocation);
                // 更新验证码发送状态
                log.setSendStatus(1);
                smsCaptchaLogService.update(log);
                //调活动平台api接口，记录短信接口费用
                activityApiUtil.noteApiCost(ApiCostTypeConstant.MOBILE_MESSAGE);
                return new ResultUtil<Object>().setSuccessMsg("发送短信验证码成功");
            } else {
                // 更新验证码发送状态
                log.setSendStatus(-1);
                log.setReason(response.getErr_msg());
                smsCaptchaLogService.update(log);
                return new ResultUtil<Object>().setErrorMsg("请求发送验证码失败，" + response.getErr_msg());
            }
        } catch (Exception e) {
            // 更新验证码发送状态
            log.setSendStatus(-1);
            log.setReason(e.getMessage());
            smsCaptchaLogService.update(log);
            CaptchaController.log.error("请求发送短信验证码失败，" + e);
            return new ResultUtil<Object>().setErrorMsg("请求发送验证码失败，" + e.getMessage());
        }
    }

//    @APIModifier(APIModifierType.PRIVATE)
//    @RequestMapping(value = "/getSmsCaptchaLog/{mobile}", method = RequestMethod.GET)
//    @ApiOperation(value = "获取短信验证码发送记录")
//    public Result<Page<SmsCaptchaLog>> getSmsCaptchaLog(@PathVariable String mobile, @ModelAttribute PageVo pageVo, @ModelAttribute SearchVo searchVo,
//                                           HttpServletRequest request) {
//        SmsCaptchaLog smsCaptchaLog = new SmsCaptchaLog();
//        smsCaptchaLog.setPhone(AESUtil.encrypt(mobile));
//        smsCaptchaLog.setAppid(UserContext.getAppid());
//        smsCaptchaLog.setTenantId(UserContext.getTenantId());
//        Page<SmsCaptchaLog> condition = smsCaptchaLogService.findByCondition(smsCaptchaLog,searchVo,PageUtil.initPage(pageVo));
//        return new ResultUtil<Page<SmsCaptchaLog>>().setData(condition);
//    }

    /**
     * 湖南建行，发送指定短信验证码
     * @param mobile
     * @param code
     * @param request
     * @return
     */
    @APIModifier(APIModifierType.PRIVATE)
    @RequestMapping(value = "/sendHnccbAccountSmsBy/{mobile}", method = RequestMethod.GET)
    @ApiOperation(value = "发送短信验证码")
    public Result<Object> sendHnccbAccountSms(@PathVariable String mobile,
                                              @RequestParam String code, HttpServletRequest request) {
        // 添加短信验证码记录
        code = AESUtil.decrypt(code, aesKeyV1);
        SmsCaptchaLog log = new SmsCaptchaLog();
        log.setAppid(UserContext.getAppid());
        log.setTenantId(UserContext.getTenantId());
        log.setPhone(AESUtil.encrypt(mobile));
        log.setSendStatus(0);
        Date date = new Date();
        log.setCreateTime(date);
        log.setUpdateTime(date);
        log.setCode(code);
        smsCaptchaLogService.save(log);
        // 发送验证码
        try {

            String captchaTemplate = smsSetting.getCaptchaTemplate();
            String smsSignature = dictDataService.findSmsSignatureByAppid(UserContext.getAppid());
            if (StrUtil.isEmpty(smsSignature)) {
                smsSignature = "【盈天地】";
            }
            String content = smsSignature + StrUtil.replace(captchaTemplate, "${code}", code);
            SendSMSResponse response = smsUtil.sendCode(mobile, content, "-1");
            if (response.isSuccess()) {
                PhoneLocation phoneLocation = new PhoneLocation();
                phoneLocation.setPhone(mobile);
                MQMessage<PhoneLocation> mqMessagePhoneLocation = new MQMessage<>();
                mqMessagePhoneLocation.setAppid(UserContext.getAppid());
                mqMessagePhoneLocation.setTenantId(UserContext.getTenantId());
                mqMessagePhoneLocation.setContent(phoneLocation);
                rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_PHONE_LOCATION_EVENT_MSG), mqMessagePhoneLocation);
                // 更新验证码发送状态
                log.setSendStatus(1);
                smsCaptchaLogService.update(log);
                //调活动平台api接口，记录短信接口费用
                activityApiUtil.noteApiCost(ApiCostTypeConstant.MOBILE_MESSAGE);
                return new ResultUtil<Object>().setSuccessMsg("发送短信验证码成功");
            } else {
                // 更新验证码发送状态
                log.setSendStatus(-1);
                log.setReason(response.getErr_msg());
                smsCaptchaLogService.update(log);
                return new ResultUtil<Object>().setErrorMsg("请求发送验证码失败，" + response.getErr_msg());
            }
        } catch (Exception e) {
            // 更新验证码发送状态
            log.setSendStatus(-1);
            log.setReason(e.getMessage());
            smsCaptchaLogService.update(log);
            CaptchaController.log.error("请求发送短信验证码失败，" + e);
            return new ResultUtil<Object>().setErrorMsg("请求发送验证码失败，" + e.getMessage());
        }
    }
}
