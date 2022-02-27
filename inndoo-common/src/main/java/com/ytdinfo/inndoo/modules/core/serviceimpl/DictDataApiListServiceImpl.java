package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.HttpRequestUtil;
import com.ytdinfo.inndoo.common.vo.NameListValidateResultVo;
import com.ytdinfo.inndoo.common.vo.RequestDefineVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.entity.ApiCheck;
import com.ytdinfo.inndoo.modules.core.entity.ExternalApiInfo;
import com.ytdinfo.inndoo.modules.core.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 接口名单接口实现
 *
 * @author zhuzheng
 */
@Slf4j
@Service
public class DictDataApiListServiceImpl implements DictDataApiListService {

    @Autowired
    private DictDataService dictDataService;

    @XxlConf("core.front.rooturl")
    private String coreRootUrl;

    @XxlConf("core.api.appkey")
    private String appKey;

    @XxlConf("core.api.appsecret")
    private String appSecret;

    @Autowired
    private ExternalApiInfoService externalApiInfoService;

    @Autowired
    protected ActivityApiUtil activityApiUtil;

    @Autowired
    private ApiCheckService apiCheckService;

    @Autowired
    private DynamicApiService dynamicApiService;

    @Autowired
    private ActAccountService actAccountService;

    @Override
    public NameListValidateResultVo verify(String dictDataId, String recordIdentifier, String openId) {
        DictData dictData = dictDataService.get(dictDataId);
        NameListValidateResultVo resultVo = new NameListValidateResultVo();
        resultVo.setMatch(false);
        resultVo.setTimes(0);
        if (dictData == null) {
            return resultVo;
        }
        // 动态接口劫持处理 暂时支持辽宁中行私有化数据接口
        if (!dictData.getValue().startsWith("http")) {
            String apiId = dictData.getValue();
            if (apiId.startsWith("$")) {
                ApiCheck apiCheck = apiCheckService.get(apiId.replace("$", ""));
                ActAccount actAccount = actAccountService.findByActAccountId(recordIdentifier);
                Result<Object> result = dynamicApiService.getValue(apiCheck, actAccount.getActAccountId(), actAccount.getCoreAccountId(), openId,null);
                boolean vertify = "Y".equals(result.getResult().toString());
                resultVo.setMatch(vertify);
                resultVo.setTimes(0);
                return resultVo;
            } else {
                String ext = "";
                boolean vertify = externalApiInfoService.vertify(apiId, recordIdentifier, ext);
                resultVo.setMatch(vertify);
                resultVo.setTimes(0);
                return resultVo;
            }
        }
        Map paramMap = new HashMap<String, Object>();
        paramMap.put("record", recordIdentifier);
        paramMap.put("openId", openId);
        paramMap.put("appKey", appKey);
        paramMap.put("tenantId", UserContext.getTenantId());
        paramMap.put("wxappid", UserContext.getAppid());
        paramMap.put("timestamp", System.currentTimeMillis());
        paramMap.put("appSecret", appSecret);
        String sign = SecureUtil.signParams(DigestAlgorithm.MD5, paramMap, "&", "=", true);
        paramMap.put("sign", sign);
        paramMap.remove("appSecret");
        String params = HttpUtil.toParams(paramMap);
        String requestUrl = dictData.getValue() + "?" + params;
        String content = HttpRequestUtil.get(requestUrl);
        Result result = JSONUtil.toBean(content, Result.class);
        if (result.isSuccess()) {
            resultVo.setTimes(1);
            resultVo.setMatch(true);
        } else {
            resultVo.setTimes(-1);
            resultVo.setMatch(false);
        }
        return resultVo;
    }

}