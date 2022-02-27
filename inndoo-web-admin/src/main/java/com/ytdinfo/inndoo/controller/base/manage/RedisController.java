package com.ytdinfo.inndoo.controller.base.manage;

import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.base.vo.RedisVo;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "Redis缓存管理接口")
@RequestMapping("/base/redis")
public class RedisController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RequestMapping(value = "/listByPage", method = RequestMethod.GET)
    @ApiOperation(value = "分页获取全部")
    public Result<Page<RedisVo>> listAllByPage(@RequestParam(required = false) String key,
                                               @ModelAttribute SearchVo searchVo,
                                               @ModelAttribute PageVo pageVo){

        List<RedisVo> list = new ArrayList<>();
        if(StrUtil.isNotBlank(key)){
            key = "*" + key + "*";
        }else{
            key = "*";
        }
        for (String s : RedisUtil.keys(key)) {
            RedisVo redisVo = new RedisVo(s, "");
            list.add(redisVo);
        }
        Page<RedisVo> page = new PageImpl<RedisVo>(PageUtil.listToPage(pageVo, list), PageUtil.initPage(pageVo), list.size());
        page.getContent().forEach(e->{
            String value = "";
            try {
                value =  redisTemplate.opsForValue().get(e.getKey());
                if(value.length()>150){
                    value = value.substring(0, 149) + "...";
                }
            } catch (Exception exception){
                value = "非字符格式数据";
            }
            e.setValue(value);
        });
        return new ResultUtil<Page<RedisVo>>().setData(page);
    }

    @RequestMapping(value = "/listByKey/{key}", method = RequestMethod.GET)
    @ApiOperation(value = "通过key获取")
    public Result<Object> getByKey(@PathVariable String key){

        String value = redisTemplate.opsForValue().get(key);
        return new ResultUtil<Object>().setData(value);
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ApiOperation(value = "添加或编辑")
    public Result<Object> save(@RequestParam String key,
                               @RequestParam String value){

        redisTemplate.opsForValue().set(key, value);
        return new ResultUtil<Object>().setSuccessMsg("删除成功");
    }

    @RequestMapping(value = "/delByKeys", method = RequestMethod.DELETE)
    @ApiOperation(value = "批量删除")
    public Result<Object> delByKeys(@RequestParam String[] keys){

        for(String key : keys){
            redisTemplate.delete(key);
        }
        return new ResultUtil<Object>().setSuccessMsg("删除成功");
    }

    @RequestMapping(value = "/delAll", method = RequestMethod.DELETE)
    @ApiOperation(value = "全部删除")
    public Result<Object> delAll(){

        redisTemplate.delete(RedisUtil.keys("*"));
        return new ResultUtil<Object>().setSuccessMsg("删除成功");
    }
}
