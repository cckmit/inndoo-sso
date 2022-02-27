package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.AccountFormService;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.DecryptAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class DecryptAccountConsumer extends BaseConsumer {
    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountFormService accountFormService;

    @Autowired
    private DecryptAccountService decryptAccountService;

    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    private String tenantId;

    @Override
    public void onMessage(MQMessage mqMessage) {
        Account account = JSONUtil.toBean((JSONObject) mqMessage.getContent(), Account.class);
        String lockKey = "DecryptAccountConsumer:" + account.getId();
        lockTemplate.execute(lockKey, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                //获取身份识别表单
                Boolean isIdentifierForm = true;
                AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(account.getAppid(), isIdentifierForm);
                //获取初始的用户标识的注册页控件列表
                List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
                Map dentifierMap = new HashMap<>();
                if (null != IsIdentifierFormMetas && IsIdentifierFormMetas.size() > 0) {
                    for (AccountFormMeta accountFormMeta : IsIdentifierFormMetas) {
                        if (accountFormMeta.getIsStandard()) {
                            Object object = ReflectUtil.getFieldValue(account, accountFormMeta.getMetaType());
                            if (null == object) {
                                return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "是唯一标识控件，必须有值");
                            }
                            dentifierMap.put(accountFormMeta.getMetaType(), object.toString().trim());
                        }
                        if (!accountFormMeta.getIsStandard()) {
                            List<AccountFormField> accountFormFields = account.getAccountFormFields();
                            //获取存在必填非标准组件为accountFormMeta.getTitle()值
                            List<AccountFormField> addAccountFormFields = accountFormFields.stream().filter(item -> item.getMetaTitle().equals(accountFormMeta.getTitle())).collect(Collectors.toList());
                            if (null != addAccountFormFields && addAccountFormFields.size() > 0) {
                                dentifierMap.put(accountFormMeta.getMetaType(), addAccountFormFields.get(0).getFieldData().trim());
                            } else {
                                return new ResultUtil<String>().setErrorMsg(accountFormMeta.getTitle() + "是唯一标识控件，必须有值");
                            }
                        }
                    }
                    DecryptAccount decryptAccount = decryptAccountService.findByCoreAccountId(account.getId());
                    if (null == decryptAccount) {
                        String accountType = "";
                        String decryptValue = "";
                        Set<String> set = dentifierMap.keySet();
                        if (set.size() > 1) {
                            for (String key : set) {
                                accountType = accountType + key + ",";
                                decryptValue = decryptValue + dentifierMap.get(key).toString() + ",";
                            }
                        } else {
                            for (String key : set) {
                                accountType = key;
                                decryptValue = dentifierMap.get(key).toString();
                            }
                        }
                        decryptAccount = new DecryptAccount();
                        decryptAccount.setAccountType(accountType);
                        decryptAccount.setCoreAccountId(account.getId());
                        decryptAccount.setDecryptValue(decryptValue);
                        decryptAccount.setAppid(account.getAppid());
                        decryptAccountService.save(decryptAccount);
                    } else {
                        String accountType = "";
                        String decryptValue = "";
                        Set<String> set = dentifierMap.keySet();
                        if (set.size() > 1) {
                            for (String key : set) {
                                accountType = accountType + key + ",";
                                decryptValue = decryptValue + dentifierMap.get(key).toString() + ",";
                            }
                        } else {
                            for (String key : set) {
                                accountType = key;
                                decryptValue = dentifierMap.get(key).toString();
                            }
                        }
                        if (!decryptAccount.getDecryptValue().equals(decryptValue)) {
                            decryptAccount.setDecryptValue(decryptValue);
                            decryptAccountService.update(decryptAccount);
                        }
                    }

                } else {
                    return new ResultUtil<String>().setErrorMsg("必须有用户识别控件");
                }
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
