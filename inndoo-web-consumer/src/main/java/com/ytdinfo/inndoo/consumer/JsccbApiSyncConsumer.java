package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.HttpRequestUtil;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.ActAccountService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author jj
 */
@Service
@Scope("prototype")
public class JsccbApiSyncConsumer extends BaseConsumer {

    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private AccountService accountService;

    private String getPhoneByAccountId(String actAccountId) {
        ActAccount actAccount = actAccountService.findByActAccountId(actAccountId);
        if (actAccount == null) {
            return null;
        }
        String coreAccountId = actAccount.getCoreAccountId();
        if (StringUtils.isEmpty(coreAccountId)) {
            return null;
        }
        Account account = accountService.get(coreAccountId);
        if (account == null) {
            return null;
        }
        return account.getPhone();
    }

    @Override
    public void onMessage(MQMessage mqMessage) {
        String config = XxlConfClient.get("activity.jsccb.lb.config");
        if (StrUtil.isNotEmpty(config)) {
            config = config.replaceAll("rn", "");
            JSONObject jsonObject = JSONUtil.parseObj(config);
            String url = jsonObject.getStr("apiUrl") + "/api/jsccb/operateData.do";

            String date1 = DateUtil.format(new Date(), "yyyy-MM-dd");
            String date2 = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
            JSONObject content = JSONUtil.parseObj(mqMessage.getContent());
            String cardItemId = content.getStr("cardItemId");
            String cardName = content.getStr("cardName");
            String accountId = content.getStr("accountId");
            String status = content.getStr("status");
            String campaignCode = content.getStr("campaignCode");
            Long totalCount = content.getLong("totalCount");
            Long dayCount = content.getLong("dayCount");
            String createTime = content.getStr("createTime");
            String operateTime = content.getStr("operateTime");
            String userTime = content.getStr("userTime");
            String phone = getPhoneByAccountId(accountId);
            if (StrUtil.isNotEmpty(phone)) {
                Map<String, Object> map = new HashMap<>();
                map.put("createTime", date1);
                map.put("campaignCode", campaignCode);
                map.put("supplierCode", "YTD");
                map.put("objectType", "9");
                map.put("count", totalCount.toString());
                map.put("dayCount", dayCount.toString());
                List<Map<String, String>> list = new ArrayList<>(1);
                Map<String, String> recordMap = new HashMap<>();
                recordMap.put("campaignCode", campaignCode);
                recordMap.put("supplierCode", "YTD");
                recordMap.put("telPhone", phone);
                recordMap.put("equityRecordId", cardItemId);
                recordMap.put("equityName", cardName);
                recordMap.put("status", status);
                recordMap.put("createTime", createTime);
                recordMap.put("operateTime", operateTime);
                recordMap.put("useTime", userTime);
                list.add(recordMap);
                map.put("records", list);
                HttpRequestUtil.post(url + "?phone=" + phone, JSONUtil.toJsonStr(map));
            }
        }
    }
}
