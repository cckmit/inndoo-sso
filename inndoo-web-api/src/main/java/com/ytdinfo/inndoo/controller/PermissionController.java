package com.ytdinfo.inndoo.controller;

import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * @author timmy
 * @date 2020/5/26
 */
@Slf4j
@RestController
@Api(description = "皮肤模板管理接口")
@RequestMapping("/permission")
@APIModifier(APIModifierType.PRIVATE)
public class PermissionController {
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/cache/clear", method = RequestMethod.GET)
    @ApiOperation(value = "清除缓存")
    public Result<String> clearCache() {
        String cacheKey = "permission:list";
        redisTemplate.unlink(cacheKey);
        //手动批量删除缓存
        Set<String> keys = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userPermission.getPrefixKey());
        redisTemplate.unlink(keys);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userPermission.getPrefixKey());
        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
        redisTemplate.unlink(keysUser);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());

        Set<String> keysUserToken = RedisUtil.membersFromKeyStore(RedisKeyStoreType.USER_TOKEN.getPrefixKey());
        Set keysTokenPre = RedisUtil.membersFromKeyStore(RedisKeyStoreType.TOKEN_PRE.getPrefixKey());
        redisTemplate.unlink(keysUserToken);
        redisTemplate.unlink(keysTokenPre);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.USER_TOKEN.getPrefixKey());
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.TOKEN_PRE.getPrefixKey());

        Set<String> keysUserMenu = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userMenuList.getPrefixKey());
        redisTemplate.unlink(keysUserMenu);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userMenuList.getPrefixKey());
        redisTemplate.unlink("permission::allList");
        return new ResultUtil<String>().setData("OK");
    }
}