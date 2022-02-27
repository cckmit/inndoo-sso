package com.ytdinfo.inndoo.controller;

import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.LiaoNingResult;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.utils.LiaoNingUrlUtil;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.ActAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "辽宁中行和星兴app对接")
@RequestMapping("/h5Boc4Xing")
@APIModifier(APIModifierType.PRIVATE)
public class H5Boc4XingController  {
    @Autowired
    private LiaoNingUrlUtil liaoNingUrlUtil;
    @Autowired
    private ActAccountService actAccountService;
    @Autowired
    private AccountService  accountService;


    @RequestMapping(value = "/query/cdld", method = RequestMethod.GET)
    @ApiOperation(value = "获取辽宁用户存款接口")
    public Result<Object> queryCdld(@RequestParam String accountId, HttpServletRequest request) {
        ActAccount actAccount = actAccountService.findByActAccountId(accountId);
        if(null == actAccount ) {
            return new ResultUtil<Object>().setErrorMsg("未查询到改用户的绑定关系");
        }
        String coreAccountId = actAccount.getCoreAccountId();
        Account account = accountService.get(coreAccountId);
        if(null == account){
            return new ResultUtil<Object>().setErrorMsg("未查询到注册信息");
        }
        String phone = account.getPhone();
        String url = "http://ln-biams.bank-of-china.com:8060/openapi/commonapi/query/cdld";
        LiaoNingResult liaoNingResult = new LiaoNingResult();
        try {
            Map<String, String> mapdata = new HashMap<>();
//            mapdata.put("cust_phone","18866668888");
            mapdata.put("cust_phone",phone);
            String content = liaoNingUrlUtil.getContent(url,mapdata,true);
            liaoNingResult = JSONUtil.toBean(content, LiaoNingResult.class);
            return new ResultUtil<Object>().setData(liaoNingResult);
        } catch (Exception e) {
            return new ResultUtil<Object>().setErrorMsg(e.getMessage());
        }
    }
}
