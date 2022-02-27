package com.ytdinfo.inndoo.controller.core;

import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.EncryptionMethodType;
import com.ytdinfo.inndoo.common.enums.SourceType;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.AppUrl;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SourceTypeVo;
import com.ytdinfo.inndoo.modules.base.entity.WxAuthorizer;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.*;
import com.ytdinfo.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.Collator;
import java.util.*;

/**
 * Created by timmy on 2020/2/12.
 */
@Slf4j
@RestController
@Api(description = "公用方法")
@RequestMapping("/common")
public class CommonController {

    @Autowired
    private ExternalApiInfoService externalApiInfoService;

    @Autowired
    private DynamicCodeService dynamicCodeService;

    @Autowired
    private DynamicApiService dynamicApiService;

    @Autowired
    private RoleStaffService roleStaffService;

    @Autowired
    private IAccountService iAccountService;
    @Autowired
    private IAccountFormFieldService iAccountFormFieldService;
    @Autowired
    private IStaffService iStaffService;
    @Autowired
    private IAchieveListRecordService iAchieveListRecordService;
    @Autowired
    private IAchieveListExtendRecordService iAchieveListExtendRecordService;
    @Autowired
    private IWhiteListRecordService iWhiteListRecordService;
    @Autowired
    private IWhiteListExtendRecordService iWhiteListExtendRecordService;
    @Autowired
    private ILimitListRecordService iLimitListRecordService;
    @Autowired
    private ILimitListExtendRecordService iLimitListExtendRecordService;
    @Autowired
    private IBindLogService iBindLogService;
    @Autowired
    private ISmsCaptchaLogService iSmsCaptchaLogService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private WhiteListService whiteListService;
    @Autowired
    private LimitListService limitListService;
    @Autowired
    private AchieveListService achieveListService;
    @Autowired
    private MatrixApiUtil matrixApiUtil;




    @RequestMapping(value = "/fronturl", method = RequestMethod.GET)
    @ApiOperation(value = "获取活动平台前端根路径")
    @SystemLog(description = "获取活动平台前端根路径")
    public Result<AppUrl> activityList() {
        AppUrl appUrl = new AppUrl();
        String actFrontUrl = XxlConfClient.get("activity.front.rooturl");
        String coreFrontUrl = XxlConfClient.get("core.front.rooturl");

        appUrl.setCoreFrontUrl(coreFrontUrl);
        appUrl.setActFrontUrl(actFrontUrl);

        return new ResultUtil<AppUrl>().setData(appUrl);
    }

    @RequestMapping(value = "/encryptionMethods", method = RequestMethod.GET)
    @ApiOperation(value = "获取加密方式")
    @SystemLog(description = "获取加密方式")
    public Result<List<Map<String, Object>>> encryptionMethods() {
        return new ResultUtil<List<Map<String, Object>>>().setData(EncryptionMethodType.getEncryptionMethods());
    }

    @RequestMapping(value = "/sourceType/list", method = RequestMethod.GET)
    @ApiOperation(value = "获取sourceType类型清单")
    @SystemLog(description = "获取sourceType类型清单")
    public Result<List<SourceTypeVo>> type() {
        List<SourceTypeVo> list = new ArrayList<>();
        for (SourceType value : SourceType.values()) {
            list.add(new SourceTypeVo(value.getValue(), value.getDisplayName()));
        }
        Collator collator = Collator.getInstance(Locale.CHINESE);
        list.sort((o1, o2) -> collator.compare(o1.getName(), o2.getName()));
        return new ResultUtil<List<SourceTypeVo>>().setData(list);
    }

    @RequestMapping(value = "/listExternalApi", method = RequestMethod.GET)
    @ApiOperation(value = "获取外部接口定义列表")
    public Result<List<ExternalApiInfo>> listExternalApi() {
        return new ResultUtil<List<ExternalApiInfo>>().setData(externalApiInfoService.findAll());
    }

    @RequestMapping(value = "/dynamicApiList", method = RequestMethod.GET)
    @ApiOperation(value = "获取动态接口列表")
    public Result<List<DynamicApi>> dynamicApiList() {
        return new ResultUtil<List<DynamicApi>>().setData(dynamicApiService.findByAppid(UserContext.getAppid()));
    }

    @RequestMapping(value = "/dynamicCodeList", method = RequestMethod.GET)
    @ApiOperation(value = "获取动态代码列表")
    public Result<List<DynamicCode>> dynamicCodeList() {
        return new ResultUtil<List<DynamicCode>>().setData(dynamicCodeService.findByAppid(UserContext.getAppid()));
    }

    @RequestMapping(value = "/roleStaff/list", method = RequestMethod.GET)
    @ApiOperation(value = "获取员工角色")
    public Result<List<RoleStaff>> roleStaffList() {
        return new ResultUtil<List<RoleStaff>>().setData(roleStaffService.findAll());
    }


    //密码更换同步数据
    @RequestMapping(value = "/aessynchro", method = RequestMethod.GET)
    @ApiOperation(value = "密码更换同步数据")
    public Result<String> aessynchro() {
        String isexecute="N";
        if (redisTemplate.hasKey("aes:privatepassword"))
        {
            isexecute=String.valueOf(redisTemplate.opsForValue().get("aes:privatepassword"));
        }
        if("N".equals(isexecute)) {
            String oldpassword = AESUtil.PASSWORD;
            String newpassword = AESUtil.PRIVATEPASSWORD;
            if (StringUtils.isNotEmpty(newpassword) && StringUtils.isNotEmpty(oldpassword)) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("oldpassword", oldpassword);
                map.put("newpassword", newpassword);

                //2 、t_account_form_field 处理
                iAccountFormFieldService.aesDataSwitchPassword(map);

                //1 、Account 处理
                iAccountService.aesDataSwitchPassword(map);

                //4 、t_achieve_list_record_2 处理
                //5 、t_achieve_list_extend_record 处理
                //6 、t_white_list_record 处理
                //7 、t_white_list_extend_record 处理
                //8 、t_limit_list_record 处理
                //9 、t_limit_list_extend_record 处理

                Byte isEncryption = new Byte("0");
                List<WhiteList> whiteList =whiteListService.findByListTypeAndIsEncryption(2,isEncryption);
                for (WhiteList item:whiteList) {
                    Map<String, Object> mapWhiteList = new HashMap<String, Object>();
                    mapWhiteList.put("oldpassword", oldpassword);
                    mapWhiteList.put("newpassword", newpassword);
                    mapWhiteList.put("listId",item.getId());
                    iWhiteListRecordService.aesDataSwitchPassword(mapWhiteList);
                }

                List<LimitList> limitList =limitListService.findByListTypeAndIsEncryption(2,isEncryption);
                for (LimitList item:limitList) {
                    Map<String, Object> mapLimitList = new HashMap<String, Object>();
                    mapLimitList.put("oldpassword", oldpassword);
                    mapLimitList.put("newpassword", newpassword);
                    mapLimitList.put("listId", item.getId());
                    iLimitListRecordService.aesDataSwitchPassword(mapLimitList);
                }
                List<AchieveList> achieveList =achieveListService.findByListTypeAndIsEncryption(2,isEncryption);
                for (AchieveList item:achieveList) {
                    Map<String, Object> mapAchieveList = new HashMap<String, Object>();
                    mapAchieveList.put("oldpassword", oldpassword);
                    mapAchieveList.put("newpassword", newpassword);
                    mapAchieveList.put("listId", item.getId());
                    iAchieveListRecordService.aesDataSwitchPassword(mapAchieveList);
                }

                //3 、t_staff 处理
                //10 、t_phone_location 处理
                //11 、t_bind_log 处理
                //12 、t_sms_captcha_log 处理
                iStaffService.aesDataSwitchPassword(map);

                redisTemplate.opsForValue().set("aes:privatepassword", "Y");
            }
            return new ResultUtil<String>().setData("ok");
        }

        return new ResultUtil<String>().setData("yy");
    }

    @RequestMapping(value = "/wxauthorizer", method = RequestMethod.GET)
    @ApiOperation(value = "密码更换同步数据")
    public Result<WxAuthorizer> getWxAuthorizer() {
        return new ResultUtil<WxAuthorizer>().setData(matrixApiUtil.getWxAuthorizer(UserContext.getAppid()));
    }

}