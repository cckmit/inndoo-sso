package com.ytdinfo.inndoo.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.constant.ApiCostTypeConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseThreadPoolConsumer;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.SmsUtil;
import com.ytdinfo.inndoo.common.vo.SmsVo;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.model.response.SendSMSResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author QHT
 * @date 2020/2/13
 */
@Service
@Scope("prototype")
public class SMSSendConsumer extends BaseThreadPoolConsumer {

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private AccountService accountService;

    @Autowired
    private DictDataService dictDataService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @Override
    public void onMessage(MQMessage mqMessage) {
        SmsVo smsVo = JSONUtil.toBean((JSONObject) mqMessage.getContent(), SmsVo.class);
        String mobile = "";
        if (smsVo.getIsPhone()) {
            //mobile = AESUtil.decrypt(smsVo.getCoreAccountId());
            mobile = AESUtil.comDecrypt(smsVo.getCoreAccountId());

        } else {
            Account account = accountService.get(smsVo.getCoreAccountId());
            mobile = account.getPhone();
        }
        String content = smsVo.getContent();
        String smsSignature = dictDataService.findSmsSignatureByAppid(UserContext.getAppid());
        try {
            SendSMSResponse sendSMSResponse = smsUtil.sendSms(mobile, smsSignature + content, "-1");
            if (sendSMSResponse.isSuccess()) {
                //调活动平台api接口，记录短信接口费用
                activityApiUtil.noteApiCost(ApiCostTypeConstant.MOBILE_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
