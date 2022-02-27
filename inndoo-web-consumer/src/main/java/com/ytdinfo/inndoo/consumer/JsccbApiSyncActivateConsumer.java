package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.inndoo.common.constant.DIApiConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.HttpRequestUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IAchieveListRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
@Slf4j
public class JsccbApiSyncActivateConsumer extends BaseConsumer {

    private static final String hashKey = "jsccb-equity-status-statistics";

    @Autowired
    private AchieveListService achieveListService;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private IAchieveListRecordService iAchieveListRecordService;
    @Autowired
    private RabbitUtil rabbitUtil;
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
            String appid = jsonObject.getStr("appid");
            String tenantId = jsonObject.getStr("tenantId");
            Integer batchSize = jsonObject.getInt("batchSize");
            JSONObject achieveListConfig = jsonObject.getJSONObject("achieveListConfig");
            Date now = new Date();
            String date1 = DateUtil.format(now, "yyyy-MM-dd");
            String date2 = DateUtil.format(now, "yyyy-MM-dd HH:mm:ss");
            JSONObject obj = JSONUtil.parseObj(mqMessage.getContent());
            JSONArray array = obj.getJSONArray("array");
            JSONObject statisticCount = obj.getJSONObject("statisticCount");
            Map<String, Map<String, String>> requestMap;
            Map<String, String> targetUserCheck;
            Map<String, String> achieveMap;
            List<Map<String, String>> achieveMapList = new ArrayList<>();
            if (null != array && array.size() > 0) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    String accountId = item.getStr("accountId");
                    String cardItemId = item.getStr("cardItemId");
                    String cardName = item.getStr("cardName");
                    String campaignCode = item.getStr("campaignCode");
                    String createTime = item.getStr("createTime");
                    String targetUserCampaignCode = achieveListConfig.getJSONObject(campaignCode).getStr("targetUserCampaignCode");
                    String phone = getPhoneByAccountId(accountId);
                    if (StrUtil.isNotEmpty(phone)) {
                        targetUserCheck = new HashMap<>();
                        targetUserCheck.put("telPhone", phone);
                        targetUserCheck.put("campaignCode", targetUserCampaignCode);
                        requestMap = new HashMap<>();
                        requestMap.put("targetUserCheck", targetUserCheck);
                        String result = HttpRequestUtil.post("https://act.ytdinfo.cn/wxactivity/api/jsccb/isTargetUser.do", JSONUtil.toJsonStr(requestMap));
                        if (StrUtil.isNotEmpty(result)) {
                            JSONObject resultObject = JSONUtil.parseObj(result);
                            Integer code = resultObject.getInt("code");
                            if (null != code && code.equals(0)) {
                                JSONObject dataDecode = resultObject.getJSONObject("dataDecode");
                                Boolean isTargetUser = dataDecode.getBool("isTargetUser");
                                if ("315633948053053440".equals(tenantId)) {
                                    if (isTargetUser || "13814829384".equals(phone)) {
                                        achieveMap = new HashMap<>();
                                        achieveMap.put("phone", phone);
                                        achieveMap.put("campaignCode", campaignCode);
                                        achieveMap.put("cardItemId", cardItemId);
                                        achieveMap.put("cardName", cardName);
                                        achieveMap.put("createTime", createTime);
                                        achieveMapList.add(achieveMap);
                                    }
                                } else {
                                    achieveMap = new HashMap<>();
                                    achieveMap.put("phone", phone);
                                    achieveMap.put("campaignCode", campaignCode);
                                    achieveMap.put("cardItemId", cardItemId);
                                    achieveMap.put("cardName", cardName);
                                    achieveMap.put("createTime", createTime);
                                    achieveMapList.add(achieveMap);
                                }
                            }
                        }
                    }
                }
                if (CollectionUtil.isNotEmpty(achieveMapList)) {
                    Map<String, List<Map<String, String>>> campaignCodeGroupMap = achieveMapList.stream().collect(Collectors.groupingBy(e -> e.get("campaignCode")));
                    campaignCodeGroupMap.forEach((campaignCode, list) -> {
                        String achieveListId = achieveListConfig.getJSONObject(campaignCode).getStr("achieveListId");
                        if (StrUtil.isNotEmpty(achieveListId)) {
                            if (CollectionUtil.isNotEmpty(list)) {
                                long size = list.size();
                                List<AchieveListRecord> achieveListRecordList = new ArrayList<>();
                                Map<String, Object> syncApiMap = new HashMap<>();
                                List<Map<String, String>> records = new ArrayList<>();
                                Map<String, String> recordItem;
                                syncApiMap.put("createTime", date1);
                                syncApiMap.put("campaignCode", campaignCode);
                                syncApiMap.put("supplierCode", "YTD");
                                syncApiMap.put("objectType", "9");
                                JSONObject statisticCountObj = statisticCount.getJSONObject(campaignCode);
                                Long totalCount = statisticCountObj.getLong("totalCount", 0L);
                                Long dayCount = statisticCountObj.getLong("dayCount", 0L);
                                syncApiMap.put("count", totalCount + size);
                                syncApiMap.put("dayCount", dayCount + size);

                                String totalKey = campaignCode;
                                String dayKey = campaignCode + ":" + DateUtil.format(new Date(), "yyyyMMdd");

                                for (Map<String, String> map : list) {
                                    putRecord(appid, achieveListId, AESUtil.encrypt(map.get("phone")), now, achieveListRecordList);
                                    recordItem = new HashMap<>();
                                    recordItem.put("campaignCode", campaignCode);
                                    recordItem.put("supplierCode", "YTD");
                                    recordItem.put("telPhone", map.get("phone"));
                                    recordItem.put("equityRecordId", map.get("cardItemId"));
                                    recordItem.put("equityName", map.get("cardName"));
                                    recordItem.put("status", "2");
                                    recordItem.put("createTime", map.get("createTime"));
                                    recordItem.put("operateTime", date2);
                                    records.add(recordItem);
                                }
                                iAchieveListRecordService.saveBatchWithIgnore(achieveListRecordList, batchSize);
                                achieveListRecordService.loadCache(achieveListId);
                                //发送mq达标用户导入后推送到act用户
                                MQMessage<AchieveList> mqMessageAchieveList = new MQMessage<AchieveList>();
                                mqMessageAchieveList.setAppid(appid);
                                mqMessageAchieveList.setTenantId(tenantId);
                                mqMessageAchieveList.setContent(achieveListService.get(achieveListId));
                                rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_PUSHACT_MSG), mqMessageAchieveList);

                                MQMessage<Map<String, Object>> mqMessageStatistic = new MQMessage<>();
                                mqMessageStatistic.setAppid(appid);
                                mqMessageStatistic.setTenantId(tenantId);
                                Map<String, Object> map = new HashMap<>();
                                map.put("totalKey", totalKey);
                                map.put("dayKey", dayKey);
                                map.put("count", size);
                                mqMessageStatistic.setContent(map);
                                rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_JSCCB_API_SYNC_STATISTIC_MSG), mqMessageStatistic);

                                syncApiMap.put("records", records);
                                HttpRequestUtil.post(apiUrl + "/api/jsccb/operateData.do?record=operateDataBatch" + date1, JSONUtil.toJsonStr(syncApiMap));
                            }
                        }
                    });
                }
            }
        }
    }

    private void putRecord(String appid, String achieveListId, String record, Date createTime, List<AchieveListRecord> achieveListRecordList) {
        AchieveListRecord w = new AchieveListRecord();
        w.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
        w.setAppid(appid);
        w.setListId(achieveListId);
        w.setCreateTime(createTime);
        w.setUpdateTime(createTime);
        w.setCreateBy("");
        w.setUpdateBy("");
        w.setTimes(BigDecimal.ZERO);
        w.setIdentifier(record);
        achieveListRecordList.add(w);
    }
}
