package com.ytdinfo.inndoo.controller.base.manage;

import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.entity.Dict;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.base.service.DictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "字典数据管理接口")
@RequestMapping("/base/dictData")
@CacheConfig(cacheNames = "dictData")
public class DictDataController {

    @Autowired
    private DictService dictService;

    @Autowired
    private DictDataService dictDataService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, DictData> redisTemplate;

    public static final String SMS_SIGNATURE = "smsSignature";

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取用户列表")
    @SystemLog(description = "分页查询字典数据")
    public Result<Page<DictData>> listByCondition(@ModelAttribute DictData dictData,
                                                  @ModelAttribute PageVo pageVo) {

        Page<DictData> page = dictDataService.findByCondition(dictData, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<DictData>>().setData(page);
    }

    @RequestMapping(value = "/listByType/{type}", method = RequestMethod.GET)
    @ApiOperation(value = "通过类型获取")
    @Cacheable(key = "#type")
    @SystemLog(description = "按类型查询字典数据")
    public Result<Object> listByType(@PathVariable String type) {

        Dict dict = dictService.findByType(type);
        if (dict == null) {
            return new ResultUtil<Object>().setErrorMsg("字典类型Type不存在");
        }
        List<DictData> list = dictDataService.findByDictId(dict.getId());
        return new ResultUtil<Object>().setData(list);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ApiOperation(value = "添加")
    @SystemLog(description = "添加字典数据")
    public Result<Object> create(@ModelAttribute DictData dictData) {

        Dict dict = dictService.get(dictData.getDictId());
        if (dict == null) {
            return new ResultUtil<Object>().setErrorMsg("字典类型id不存在");
        }
        dictDataService.save(dictData);
        // 删除缓存
        stringRedisTemplate.delete("dictData::" + dict.getType());
        if (SMS_SIGNATURE.equals(dict.getType())) {
            //如果保存的是签名，则要放缓存
            stringRedisTemplate.opsForValue().set(SMS_SIGNATURE + ":" + dictData.getTitle(), dictData.getValue());
        }
        redisTemplate.delete("dictData::" + dictData.getTitle());
        stringRedisTemplate.delete("dictData::" + dictData.getId());
        return new ResultUtil<Object>().setSuccessMsg("添加成功");
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ApiOperation(value = "编辑")
    @SystemLog(description = "编辑字典数据")
    public Result<Object> update(@ModelAttribute DictData dictData) {

        dictDataService.update(dictData);
        // 删除缓存
        Dict dict = dictService.get(dictData.getDictId());
        stringRedisTemplate.delete("dictData::" + dict.getType());
        if (SMS_SIGNATURE.equals(dict.getType())) {
            //如果保存的是签名，则要放缓存
            stringRedisTemplate.opsForValue().set(SMS_SIGNATURE + ":" + dictData.getTitle(), dictData.getValue());
        }
        redisTemplate.delete("dictData::" + dictData.getTitle());
        stringRedisTemplate.delete("dictData::" + dictData.getId());
        return new ResultUtil<Object>().setSuccessMsg("编辑成功");
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "批量通过id删除")
    @SystemLog(description = "删除字典数据")
    public Result<Object> batchDelete(@PathVariable String[] ids) {

        for (String id : ids) {
            DictData dictData = dictDataService.get(id);
            Dict dict = dictService.get(dictData.getDictId());
            dictDataService.delete(id);
            // 删除缓存
            stringRedisTemplate.delete("dictData::" + dict.getType());
            stringRedisTemplate.delete("dictData::" + id);
            //如果删除的是短信签名，则删缓存
            if (SMS_SIGNATURE.equals(dict.getType())) {
                //如果保存的是签名，则要放缓存
                stringRedisTemplate.delete(SMS_SIGNATURE + ":" + dictData.getTitle());
            }
            redisTemplate.delete("dictData::" + dictData.getTitle());
        }
        return new ResultUtil<Object>().setSuccessMsg("批量通过id删除数据成功");
    }
}
