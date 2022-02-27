package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAccountFormFieldService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账户导入处理
 */
@Service
@Scope("prototype")
public class AccountInputConsumer extends BaseConsumer {
    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountFormService accountFormService;

    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    private String tenantId;

    @Autowired
    private ActAccountService actAccountService;

    @Override
    public void onMessage(MQMessage mqMessage) {
        Account account = JSONUtil.toBean((JSONObject) mqMessage.getContent(), Account.class);
        String lockKey = "AccountInputConsumer:"+ account.getPhone();
        lockTemplate.execute(lockKey, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                actAccountService.accountInput(account,null);
                return "SUCCESS";
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                rabbitUtil.sendToExchange(QueueEnum.QUEUE_ACCOUNT_INPUT_MSG.getExchange(), "", mqMessage);
                return "SUCCESS";
            }
        });
    }
}
