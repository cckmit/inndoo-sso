package com.ytdinfo.inndoo.controller.api.jsccb;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.NameListValidateResultVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.dao.DynamicApiDao;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/jsccb/gj")
@APIModifier(APIModifierType.PUBLIC)
public class JSCCBGJController {

    @XxlConf("core.activity.jsccb.gj.config")
    private String config;

    //偏移量
    public static final String VIPARA = "1234567876543210";   //AES 为16bytes. DES 为8bytes

    //私钥
    private static final String AES_KEY = "jkjs89p8mi123456";   //AES固定格式为128/192/256 bits.即：16/24/32bytes。DES固定格式为128bits，即8bytes。


    @Autowired
    private AchieveListService achieveListService;
    @Autowired
    private AchieveListRecordService achieveListRecordService;
    @Autowired
    private RabbitUtil rabbitUtil;
    @Autowired
    private RedisDistributedLockTemplate lockTemplate;
    @Autowired
    private DynamicApiDetailService dynamicApiDetailService;
    @Autowired
    private DynamicCodeService dynamicCodeService;
    @Autowired
    private DynamicApiDao dynamicApiDao;
    @Autowired
    private DynamicCodeDetailService dynamicCodeDetailService;
    @Autowired
    private RedisTemplate redisTemplate;

    public static class GJVO {
        private String data;
        private Long timestamp;
        private String sign;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public String getSign() {
            return sign;
        }

        public void setSign(String sign) {
            this.sign = sign;
        }

        public GJVO() {
        }

        ;
    }

    @PostMapping("cardActivate")
    public Result<Object> cardActivate(@RequestBody GJVO vo, HttpServletRequest request) {
        String data = vo.getData();
        Long timestamp = vo.getTimestamp();
        String sign = vo.getSign();
        if(!encryptMD5BySalt(data,String.valueOf(timestamp)).equals(sign)){
           return new ResultUtil<>().setErrorMsg("invalid sign");
        }
        String decryptData = decryptAES(data,VIPARA,AES_KEY);
        JSONObject obj = JSONUtil.parseObj(decryptData);
        String phone = obj.getStr("phone");
        String equityRecordId = obj.getStr("equityRecordId");
        String campaignCode = obj.getStr("campaignCode");
        if(StrUtil.isEmpty(phone)){
            return new ResultUtil<>().setErrorMsg("phone can not be empty");
        }
        if(StrUtil.isEmpty(equityRecordId)){
            return new ResultUtil<>().setErrorMsg("equityRecordId can not be empty");
        }
        if(StrUtil.isEmpty(campaignCode)){
            return new ResultUtil<>().setErrorMsg("campaignCode can not be empty");
        }

        JSONObject jsonObject = JSONUtil.parseObj(config);
        String appid = jsonObject.getStr("appid");
        String tenantId = jsonObject.getStr("tenantId");
        String achieveListId = jsonObject.getStr("achieveListId");
        String url = jsonObject.getStr("url")+"/api/jsccb/operateData.do";
        String cardName = jsonObject.getStr("cardName");

        //activate(appid,tenantId,achieveListId,campaignCode,phone,equityRecordId,url,cardName);

        return new ResultUtil<>().setData("操作成功");
    }


    private void activate(String appid,String tenantId,String achieveListId,String campaignCode,String phone,String equityRecordId,String apiUrl,String cardName) {
        String lockKey = achieveListId + ":" + phone;
        lockTemplate.execute(lockKey, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                boolean exists = achieveListRecordService.existsByListIdAndIdentifier(achieveListId, AESUtil.encrypt(phone));
                if (!exists) {
                    Date now = new Date();
                    AchieveListRecord w = new AchieveListRecord();
                    w.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                    w.setAppid(appid);
                    w.setListId(achieveListId);
                    w.setCreateTime(now);
                    w.setUpdateTime(now);
                    w.setCreateBy("");
                    w.setUpdateBy("");
                    w.setTimes(BigDecimal.ZERO);
                    w.setIdentifier(AESUtil.encrypt(phone));
                    achieveListRecordService.save(w);
                    achieveListRecordService.loadSingleCache(achieveListId, w);

                    //发送mq达标用户导入后推送到act用户
                    MQMessage<AchieveListRecord> mqMessageAchieveListRecord = new MQMessage<>();
                    mqMessageAchieveListRecord.setAppid(appid);
                    mqMessageAchieveListRecord.setTenantId(tenantId);
                    mqMessageAchieveListRecord.setContent(w);
                    rabbitUtil.sendToQueue(rabbitUtil.getQueueName(StrUtil.EMPTY, QueueEnum.QUEUE_ACHIEVELISTRECORD_SINGLE_PUSHACT_MSG), mqMessageAchieveListRecord);

                    String date1 = DateUtil.format(new Date(), "yyyy-MM-dd");
                    String date2 = DateUtil.format(new Date(), "yyyy-MM-dd hh:mm:ss");

                    Map<String, Object> map = new HashMap<>();
                    map.put("createTime", date1);
                    map.put("campaignCode",campaignCode);
                    map.put("supplierCode","YTD");
                    map.put("objectType","9");
                    List<Map<String, String>> list = new ArrayList<>(1);
                    Map<String, String> recordMap = new HashMap<>();
                    recordMap.put("campaignCode",campaignCode);
                    recordMap.put("supplierCode","YTD");
                    recordMap.put("telPhone",phone);
                    recordMap.put("equityRecordId",equityRecordId);
                    recordMap.put("equityName",cardName);
                    recordMap.put("status","2");
                    recordMap.put("createTime",date2);
                    recordMap.put("operateTime",date2);
                    recordMap.put("source","0");
                    list.add(recordMap);
                    map.put("record", list);
                    HttpRequestUtil.post(apiUrl,JSONUtil.toJsonStr(map));
                }
                return null;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                activate(appid,tenantId,achieveListId,phone, equityRecordId,equityRecordId,apiUrl,cardName);
                return null;
            }
        });
    }

    public String encryptMD5BySalt(String data, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data.getBytes());
            md.update(salt.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            return null;
        }
    }

    public String decryptAES(String data, String iv, String aesKey) {
        try {
            byte[] encrypted1 = decode(data);//先用base64解密
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey.getBytes(), "AES"),
                    new IvParameterSpec(iv.getBytes()));
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original);
            return originalString.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] decode(String base64EncodedString) throws Exception {
        return new BASE64Decoder().decodeBuffer(base64EncodedString);
    }

    public String decryptAES(String data) { return decryptAES(data, VIPARA, AES_KEY); }
}
