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
import com.ytdinfo.inndoo.modules.core.entity.AchieveList;
import com.ytdinfo.inndoo.modules.core.entity.LimitList;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import com.ytdinfo.inndoo.modules.core.service.AchieveListService;
import com.ytdinfo.inndoo.modules.core.service.LimitListService;
import com.ytdinfo.inndoo.modules.core.service.WhiteListRecordService;
import com.ytdinfo.inndoo.modules.core.service.WhiteListService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListRecordService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListService;
import com.ytdinfo.inndoo.vo.ListVo;
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
@Api(description = "白名单管理接口")
@RequestMapping("/whitelist")
public class WhiteListController extends BaseController<WhiteList, String> {

    @Autowired
    private WhiteListService whiteListService;

    @Autowired
    private WhiteListRecordService whiteListRecordService;

    @Autowired
    private IWhiteListRecordService iWhiteListRecordService;

    @Autowired
    private IWhiteListService iWhiteListService;

    @Override
    public WhiteListService getService() {
        return whiteListService;
    }

    @Autowired
    private AchieveListService achieveListService;

    @Autowired
    private LimitListService limitListService;
    @Autowired
    private ActivityApiUtil activityApiUtil;
    @Override
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @SystemLog(description = "保存数据")
    public Result<WhiteList> create(@ModelAttribute WhiteList entity) {
        String appid = UserContext.getAppid();
        long num = getService().countByAppidAndName(appid, entity.getName());
        if (num > 0) {
            return new ResultUtil<WhiteList>().setErrorMsg("白名单名称已被占用！");
        }
        if(!checkAndResetEncryptionPassword(entity)){
            return new ResultUtil<WhiteList>().setErrorMsg("加密密钥不能为空");
        }
        WhiteList e = getService().save(entity);
        return new ResultUtil<WhiteList>().setData(e);
    }

    @Override
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id删除数据")
    @SystemLog(description = "通过id删除数据")
    public Result<Object> deleteById(@PathVariable String id) {
        String listType = "whitelist";
        //判断名单是否被活动平台使用
        Result result = activityApiUtil.checkEmployList(listType,id);
        if(null != result && !result.isSuccess()){
            return new ResultUtil<Object>().setErrorMsg(result.getMessage());
        }
        long num = iWhiteListService.countBylistTypeAndlistId(LinkTypeConstant.WHITE, id);
        if (num > 0) {
            return new ResultUtil<Object>().setErrorMsg("该名单已被其他名单关联无法删除");
        } else {
            getService().delete(id);
        }
        return new ResultUtil<Object>().setSuccessMsg("删除数据成功");
    }

    @Override
    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量删除")
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids) {

        List<String> name = new ArrayList<>();
        for (String id : ids) {
            String listType = "whitelist";
            //判断名单是否被活动平台使用
            Result result = activityApiUtil.checkEmployList(listType,id);
            if(null != result && !result.isSuccess()){
                return new ResultUtil<Object>().setErrorMsg(result.getMessage());
            }
            long num = iWhiteListService.countBylistTypeAndlistId(LinkTypeConstant.WHITE, id);
            if (num > 0) {
                WhiteList whiteList = getService().get(id);
                name.add(whiteList.getName());
            } else {
                getService().delete(id);
            }
        }
        if (name.size() == 0) {
            return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
        } else {
            String[] names = name.toArray(new String[name.size()]);
            if (ids.length == name.size()) {
                return new ResultUtil<Object>().setErrorMsg("部分删除成功，其中" + Arrays.toString(names) + "已被其他名单关联无法删除");
            } else {
                return new ResultUtil<Object>().setErrorMsg(Arrays.toString(names) + "已被其他名单关联无法删除");
            }
        }
    }


    @RequestMapping(value = "/list/{linkType}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    @SystemLog(description = "通过id获取")
    public Result<List<ListVo>> list(@PathVariable Byte linkType) {
        String appid = UserContext.getAppid();
        List<ListVo> voList = new ArrayList<>();
        if (linkType == LinkTypeConstant.WHITE) {
            List<WhiteList> formList = whiteListService.findByAppid(appid);
            formList.forEach(entity -> {
                ListVo vo = new ListVo();
                vo.setId(entity.getId());
                vo.setName(entity.getName());
                voList.add(vo);
            });
        }
        if (linkType == LinkTypeConstant.ACHIEVE) {
            List<AchieveList> formList = achieveListService.findByAppid(appid);
            formList.forEach(entity -> {
                ListVo vo = new ListVo();
                vo.setId(entity.getId());
                vo.setName(entity.getName());
                voList.add(vo);
            });
        }
        if (linkType == LinkTypeConstant.LIMIT) {
            List<LimitList> formList = limitListService.findByAppid(appid);
            formList.forEach(entity -> {
                ListVo vo = new ListVo();
                vo.setId(entity.getId());
                vo.setName(entity.getName());
                voList.add(vo);
            });
        }
        return new ResultUtil<List<ListVo>>().setData(voList);
    }

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<WhiteList>> listByCondition(@ModelAttribute WhiteList whiteList,
                                                   @ModelAttribute SearchVo searchVo,
                                                   @ModelAttribute PageVo pageVo) {
        String appid = UserContext.getAppid();
        whiteList.setAppid(appid);
        Page<WhiteList> page = whiteListService.findByCondition(whiteList, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<WhiteList>>().setData(page);
    }

    @Override
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "更新数据")
    @SystemLog(description = "更新数据")
    public Result<WhiteList> update(@ModelAttribute WhiteList entity) {
        WhiteList e = null;
        long total = iWhiteListRecordService.countByListId(entity.getId());
        String appid = UserContext.getAppid();
        List<WhiteList> lists = whiteListService.findByAppidAndName(appid, entity.getName());
        if (lists.size() > 0) {
            if (lists.size() > 1) {
                return new ResultUtil<WhiteList>().setErrorMsg("白名单名称已被占用");
            }
            WhiteList whiteList = lists.get(0);
            if (!StringUtils.equals(whiteList.getId(), entity.getId())) {
                return new ResultUtil<WhiteList>().setErrorMsg("白名单名称已被占用");
            }
        }
        WhiteList old = whiteListService.get(entity.getId());
        if (old != null) {
            entity.setCreateBy(old.getCreateBy());
            entity.setCreateTime(old.getCreateTime());
        }
        if(!checkAndResetEncryptionPassword(entity)){
            return new ResultUtil<WhiteList>().setErrorMsg("加密密钥不能为空");
        }
        if (total == 0) {
            e = getService().update(entity);
        } else {
            e = getService().get(entity.getId());
            Date date = entity.getExpireDate();
            String expireDate = DateUtils.format(date, "yyyy-MM-dd");
            String now = DateUtils.format(new Date(), "yyyy-MM-dd");
            int comparaFlag = DateUtils.compare_date(expireDate, now);
            if (comparaFlag == -1) {
                return new ResultUtil<WhiteList>().setErrorMsg("过期时间不得早于今天");
            }
            e.setIsTimes(entity.getIsTimes());
            e.setName(entity.getName());
            e.setRemark(entity.getRemark());
            e.setExpireDate(entity.getExpireDate());
            e.setIsEncryption(entity.getIsEncryption());
            e.setEncryptionMethod(entity.getEncryptionMethod());
            e.setEncryptionPassword(entity.getEncryptionPassword());
            if(entity.getIsTimes().intValue() == 1){
                e.setSuperimposed(entity.getSuperimposed());
            }else {
                Byte superimposed = 0;
                e.setSuperimposed(superimposed);
            }
            getService().update(e);
            // 更新过期时间
            whiteListRecordService.updateCacheTime(e);
        }
        whiteListRecordService.loadCache(entity.getId());
        return new ResultUtil<WhiteList>().setData(e);
    }

    private boolean checkAndResetEncryptionPassword(WhiteList entity){
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
