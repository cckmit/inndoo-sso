package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.inndoo.apiadapter.APIRequest;
import com.ytdinfo.inndoo.apiadapter.APIResponse;
import com.ytdinfo.inndoo.apiadapter.param.zhengzhoubank.CheckIsBindRequest;
import com.ytdinfo.inndoo.apiadapter.param.zhengzhoubank.CheckIsBindResponse;
import com.ytdinfo.inndoo.apiadapter.param.zhengzhoubank.CusAssetsRequest;
import com.ytdinfo.inndoo.apiadapter.param.zhengzhoubank.CusAssetsResponse;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ClassicsActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.CoreApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.dto.DynamicApiDto;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.entity.ExceptionLog;
import com.ytdinfo.inndoo.modules.core.mqutil.ExceptionLogUtil;
import com.ytdinfo.inndoo.modules.core.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CheckConsumerMobileServiceItestmpl implements DynamicApiBaseService<Boolean> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ClassicsActivityApiUtil classicsActivityApiUtil;
    @Autowired
    private ExceptionLogService exceptionLogService;

    @Autowired
    private ExceptionLogUtil exceptionLogUtil;

    @Autowired
    private AccountService accountService;
    @Override
    public Result<Boolean> getValue(DynamicApiDto dto) {

      /*  ExceptionLog exceptionLog = new ExceptionLog();
        exceptionLog.setUrl("CheckWhiteListServiceImpl");
        exceptionLog.setException("CheckWhiteListServiceImpl");
        exceptionLog.setMsgBody("accountId："+dto.getAccountId());
        exceptionLog.setAppid(UserContext.getAppid());
        exceptionLogService.save(exceptionLog);
        exceptionLogUtil.sendMessage(exceptionLog);*/

        if(StrUtil.isBlank(dto.getAccountId())){
            return new ResultUtil<Boolean>().setData(false);
        }
        Account accout=accountService.get(dto.getAccountId());
        if(accout==null){
            return new ResultUtil<Boolean>().setData(false);
        }
        if(StrUtil.isBlank(accout.getPhone())){
            return new ResultUtil<Boolean>().setData(false);
        }
        String phone=accout.getPhone();
        Result<Boolean> result = classicsActivityApiUtil.checkConsumerMobile(phone);
        if(result.isSuccess()){
            return new ResultUtil<Boolean>().setData(true);
        }
        return new ResultUtil<Boolean>().setData(false);
    }
}
