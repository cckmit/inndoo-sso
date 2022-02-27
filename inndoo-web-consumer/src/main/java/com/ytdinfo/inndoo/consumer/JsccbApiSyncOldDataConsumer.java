package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.HttpRequestUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.entity.AchieveListRecord;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.AchieveListRecordService;
import com.ytdinfo.inndoo.modules.core.service.AchieveListService;
import com.ytdinfo.inndoo.modules.core.service.ActAccountService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
@Slf4j
public class JsccbApiSyncOldDataConsumer extends BaseConsumer {

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
            String apiUrl = jsonObject.getStr("apiUrl");
            Date now = new Date();
            String date1 = DateUtil.format(now, "yyyy-MM-dd");
            JSONArray array = JSONUtil.parseArray(mqMessage.getContent());
            Map<String, Object> syncApiMap = new HashMap<>();
            if (null != array && array.size() > 0) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    String accountId = item.getStr("accountId");
                    item.put("telPhone", getPhoneByAccountId(accountId));
                    item.put("equityRecordId", item.getStr("cardItemId"));
                    item.put("equityName", item.getStr("cardName"));
                    item.remove("accountId");
                    item.remove("cardItemId");
                    item.remove("cardName");
                }
                Lists.partition(array, 1000).forEach(list ->{
                    syncApiMap.put("count", array.size());
                    syncApiMap.put("dayCount", array.size());
                    syncApiMap.put("createTime", date1);
                    syncApiMap.put("supplierCode", "YTD");
                    syncApiMap.put("objectType", "9");
                    syncApiMap.put("campaignCode", array.getJSONObject(0).getStr("campaignCode"));
                    syncApiMap.put("records", list);
                    HttpRequestUtil.post(apiUrl + "/api/jsccb/operateData.do?record=operateDataBatch" + date1, JSONUtil.toJsonStr(syncApiMap));
                });
            }
        }
    }
}