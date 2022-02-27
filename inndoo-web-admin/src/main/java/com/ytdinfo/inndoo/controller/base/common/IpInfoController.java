package com.ytdinfo.inndoo.controller.base.common;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.IpInfoUtil;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.Tenant;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "IP接口")
@RequestMapping("/base/common/ip")
public class IpInfoController {

    @Autowired
    private IpInfoUtil ipInfoUtil;
    @Autowired
    private MatrixApiUtil matrixApiUtil;

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ApiOperation(value = "IP及天气相关信息")
    public Result<Object> upload(HttpServletRequest request) {

        String result= ipInfoUtil.getIpWeatherInfo(ipInfoUtil.getIpAddr(request));
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/redisKeys", method = RequestMethod.GET)
    @ApiOperation(value = "redisKeys")
    public Result<String> redisKeys() {
        List<Tenant> tenantList = matrixApiUtil.getTenantList();
        RedisKeyStoreType[] values = RedisKeyStoreType.values();
        for (RedisKeyStoreType value : values) {
            Set<String> keys = RedisUtil.keys2("*" + value.getPrefixKey() + "*");
            for (Tenant tenant : tenantList) {
                UserContext.setTenantId(tenant.getId());
                Set<String> tenantKeys = (Set)CollectionUtil.filter(keys, new Filter<String>() {
                    @Override
                    public boolean accept(String s) {
                        return StrUtil.startWith(s, "core:" + tenant.getId());
                    }
                });
                if(tenantKeys.size() > 0){
                    List<String> list = new ArrayList<>();
                    list.addAll(tenantKeys);
                    int pageNo = 1;
                    while (true){
                        List<String> pageList = CollectionUtil.page(pageNo, 1000, list);
                        if(pageList.size() > 0){
                            Set<String> pageSet = new HashSet<>();
                            pageSet.addAll(pageList);
                            RedisUtil.addAllKeyToStore(value.getPrefixKey(), pageSet);
                            pageNo++;
                            if(pageList.size() < 1000){
                                break;
                            }
                        }else{
                            break;
                        }
                    }
                }
            }


        }

        return new ResultUtil<String>().setData("OK");
    }
}