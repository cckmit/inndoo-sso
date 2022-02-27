package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.util.StringUtils;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.LinkTypeConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.EncryptionMethodType;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.DateUtils;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import com.ytdinfo.inndoo.modules.core.service.LimitListRecordService;
import com.ytdinfo.inndoo.modules.core.service.LimitListService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILimitListRecordService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "受限名单管理接口")
@RequestMapping("/limitlist")
public class LimitListController extends BaseController<LimitList, String> {

    @Autowired
    private LimitListService limitListService;

    @Autowired
    private LimitListRecordService limitListRecordService;

    @Autowired
    private IWhiteListService iWhiteListService;

    @Autowired
    private ILimitListRecordService iLimitListRecordService;

    @Override
    public LimitListService getService() {
        return limitListService;
    }
    @Autowired
    private ActivityApiUtil activityApiUtil;
    @Override
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @SystemLog(description = "保存数据")
    public Result<LimitList> create(@ModelAttribute LimitList entity){
        String appid = UserContext.getAppid();
        long num =getService().countByAppidAndName(appid,entity.getName());
        if(num > 0){
            return new ResultUtil<LimitList>().setErrorMsg("受限名单名称已被占用！");
        }
        if(!checkAndResetEncryptionPassword(entity)){
            return new ResultUtil<LimitList>().setErrorMsg("加密密钥不能为空");
        }
        LimitList e = getService().save(entity);
        return new ResultUtil<LimitList>().setData(e);
    }

    @Override
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id删除数据")
    @SystemLog(description = "通过id删除数据")
    public Result<Object> deleteById(@PathVariable String id){
        String listType = "limitlist";
        //判断名单是否被活动平台使用
        Result result = activityApiUtil.checkEmployList(listType,id);
        if(null != result && !result.isSuccess()){
            return new ResultUtil<Object>().setErrorMsg(result.getMessage());
        }
        long num = iWhiteListService.countBylistTypeAndlistId( LinkTypeConstant.LIMIT,id);
        if(num > 0){
            return new ResultUtil<Object>().setErrorMsg("该名单已被其他名单关联无法删除");
        }else {
            getService().delete(id);
        }
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }

    @Override
    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量删除")
    @SystemLog(description = "通过id批量删除")
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids){

        List<String> name = new ArrayList<>();
        for(String id:ids){
            String listType = "limitlist";
            //判断名单是否被活动平台使用
            Result result = activityApiUtil.checkEmployList(listType,id);
            if(null != result && !result.isSuccess()){
                return new ResultUtil<Object>().setErrorMsg(result.getMessage());
            }
            long num = iWhiteListService.countBylistTypeAndlistId( LinkTypeConstant.LIMIT, id);
            if(num > 0){
                LimitList limitList =  getService().get(id);
                name.add(limitList.getName());
            }else {
                getService().delete(id);
            }
        }
        if(name.size() == 0){
            return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
        }else{
            String[] names=name.toArray(new String[name.size()]);
            if(ids.length == name.size()){
                return new ResultUtil<Object>().setErrorMsg("部分删除成功，其中"+ Arrays.toString(names) +"已被其他名单关联无法删除");
            }else{
                return new ResultUtil<Object>().setErrorMsg(Arrays.toString(names) +"已被其他名单关联无法删除");
            }
        }
    }



    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<LimitList>> listByCondition(@ModelAttribute LimitList limitList,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){
        String appid = UserContext.getAppid();
        limitList.setAppid(appid);
        Page<LimitList> page = limitListService.findByCondition(limitList, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<LimitList>>().setData(page);
    }

    @Override
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "更新数据")
    @SystemLog(description = "更新数据")
    public Result<LimitList> update(@ModelAttribute LimitList entity){
        LimitList e =null;
        long total = iLimitListRecordService.countByListId(entity.getId());
        String appid = UserContext.getAppid();
        List<LimitList> lists= limitListService.findByAppidAndName(appid,entity.getName());
        if(lists.size() > 0){
            if(lists.size()>1){
                return new ResultUtil<LimitList>().setErrorMsg("受限名单名称已被占用");
            }
            LimitList limitList = lists.get(0);
            if(!StringUtils.equals(limitList.getId(),entity.getId())){
                return new ResultUtil<LimitList>().setErrorMsg("受限名单名称已被占用");
            }
        }
        LimitList old =  limitListService.get(entity.getId());
        if(old!=null){
            entity.setCreateBy(old.getCreateBy());
            entity.setCreateTime(old.getCreateTime());
        }
        if(!checkAndResetEncryptionPassword(entity)){
            return new ResultUtil<LimitList>().setErrorMsg("加密密钥不能为空");
        }
        if(total == 0){
            e = getService().update(entity);
        }else {
            e =getService().get(entity.getId());
            Date date =entity.getExpireDate();
            String expireDate=  DateUtils.format(date,"yyyy-MM-dd");
            String now=  DateUtils.format(new Date(),"yyyy-MM-dd");
            int comparaFlag  =DateUtils.compare_date(expireDate,now);
            if(comparaFlag == -1){
                return new ResultUtil<LimitList>().setErrorMsg("过期时间不得早于今天");
            }
            e.setIsTimes(entity.getIsTimes());
            e.setName(entity.getName());
            e.setRemark(entity.getRemark());
            e.setExpireDate(entity.getExpireDate());
            e.setIsEncryption(entity.getIsEncryption());
            e.setEncryptionMethod(entity.getEncryptionMethod());
            e.setEncryptionPassword(entity.getEncryptionPassword());
            getService().update(e);
            // 更新过期时间
            limitListRecordService.updateCacheTime(e);
        }
        limitListRecordService.loadCache(entity.getId());
        return new ResultUtil<LimitList>().setData(e);
    }

    private boolean checkAndResetEncryptionPassword(LimitList entity){
        if (entity.getIsEncryption() == 0) {
            entity.setEncryptionPassword(StrUtil.EMPTY);
        } else {
            EncryptionMethodType type = EncryptionMethodType.getByValue(entity.getEncryptionMethod());
            if (type != null ){
                if(type.getPasswordRequired()) {
                    if(StrUtil.EMPTY.equals(entity.getEncryptionPassword())){
                        return false;
                    }
                }else {
                    entity.setEncryptionPassword(StrUtil.EMPTY);
                }
            }
        }
        return true;
    }

}
