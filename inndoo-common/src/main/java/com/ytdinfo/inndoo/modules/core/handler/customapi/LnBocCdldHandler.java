package com.ytdinfo.inndoo.modules.core.handler.customapi;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.vo.ResultCustomVo;
import com.ytdinfo.inndoo.modules.core.entity.LnCdldRecord;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILnCdldRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/***
 * 辽宁存贷联动活动处理逻辑
 */
@Component(value = "LnBocCdldHandler")
public class LnBocCdldHandler implements BaseCustomAPIHandler {

    @Autowired
    private ILnCdldRecordService iLnCdldRecordService;

    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    @Override
    public ResultCustomVo process(String accountId, Map<String, String> defineMap, Map actMap, Map resultMap) {
        String lockId = "lncdld:" + accountId;
        Object object = lockTemplate.execute(lockId, 2000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                ResultCustomVo resultCustomVo = new ResultCustomVo();
                String type = defineMap.get("type");
                QueryWrapper<LnCdldRecord> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("act_account_id", accountId);
                LnCdldRecord one = iLnCdldRecordService.getOne(queryWrapper);
                if ("record".equals(type)) {
                    // 首次记录
                    if (one == null) {
                        one = new LnCdldRecord();
                        one.setStartAmt(new BigDecimal(resultMap.get("t2_bal").toString()));
                        one.setCreateTime(new Date());
                        one.setUpdateTime(new Date());
                        one.setActAccountId(accountId);
                        one.setVal(new BigDecimal(resultMap.get("val").toString()));
                        iLnCdldRecordService.save(one);
                    }
                    resultCustomVo.setSuccess(true);
                } else if ("t1achieve".equals(type)) {
                    //String valStr = resultMap.get("val").toString();
                    // 一重礼达标判断
                    if (one != null) {
                        BigDecimal t2_bal = new BigDecimal(resultMap.get("t2_bal").toString());
                        BigDecimal val = one.getVal();
                        if (one.getT1date() == 0) {
                            if (t2_bal.subtract(one.getStartAmt()).compareTo(val) >= 0) {
                                one.setT1amt(t2_bal);
                                one.setT1date(Integer.parseInt(resultMap.get("t2_date").toString()));
                                one.setT1dateTime(new Date());
                                one.setUpdateTime(new Date());
                                iLnCdldRecordService.updateById(one);
                                resultCustomVo.setSuccess(true);
                                resultCustomVo.setContent(defineMap.get("achieveTpl"));
                            } else {
                                String tpl = defineMap.get("notTpl");
                                // 当前日期加2天
                                String dateStr = DateUtil.format(DateUtil.offsetDay(new Date(), 2), "yyyy年MM月dd日");
                                tpl = String.format(tpl, val, dateStr);
                                resultCustomVo.setContent(tpl);
                            }
                        } else {
                            resultCustomVo.setSuccess(true);
                            resultCustomVo.setContent(defineMap.get("achieveTpl"));
                        }
                    } else {
                        String valStr = resultMap.get("val").toString();
                        String tpl = defineMap.get("notTpl");
                        // 当前日期加2天
                        String dateStr = DateUtil.format(DateUtil.offsetDay(new Date(), 2), "yyyy年MM月dd日");
                        tpl = String.format(tpl, valStr, dateStr);
                        resultCustomVo.setContent(tpl);
                    }
                } else if ("t2achieve".equals(type)) {
                    // 二重礼达标判断
                    if (one != null) {
                        //String valStr = resultMap.get("val").toString();
                        //BigDecimal val = new BigDecimal(valStr);
                        if (Boolean.TRUE.equals(one.getT2end())) {
                            // 达标了
                            if (one.getT2date() > 0) {
                                resultCustomVo.setSuccess(true);
                                resultCustomVo.setContent(defineMap.get("achieveTpl"));
                                return resultCustomVo;
                            } else {
                                String tpl = defineMap.get("notForChangeTpl");
                                //String lastDate = DateUtil.format(one.getT1dateTime(), "yyyy年MM月dd日");
                                String lastDate = DateUtil.format(DateUtil.parse(one.getT1date() + "", "yyyyMMdd"), "yyyy年MM月dd日");
                                String endDate = DateUtil.format(DateUtil.offsetDay(one.getT1dateTime(), 30), "yyyy年MM月dd日");
                                BigDecimal addAmt = one.getT1amt().subtract(one.getStartAmt());
                                tpl = String.format(tpl, lastDate, addAmt, one.getVal(), endDate, one.getVal());
                                resultCustomVo.setContent(tpl);
                            }
                        }
                        if (DateUtil.betweenDay(one.getT1dateTime(), new Date(), true) >= 30) {
                            BigDecimal t30_bal = new BigDecimal(resultMap.get("t30_bal").toString());
                            one.setT2end(true);
                            one.setUpdateTime(new Date());
                            if (t30_bal.subtract(one.getStartAmt()).compareTo(one.getVal()) >= 0) {
                                one.setT2amt(t30_bal);
                                one.setT2date(Integer.parseInt(resultMap.get("t30_date").toString()));
                                one.setT2dateTime(new Date());
                            } else {
                                String tpl = defineMap.get("notForChangeTpl");
                                String lastDate = DateUtil.format(DateUtil.parse(one.getT1date() + "", "yyyyMMdd"), "yyyy年MM月dd日");
                                String endDate = DateUtil.format(DateUtil.offsetDay(one.getT1dateTime(), 30), "yyyy年MM月dd日");
                                BigDecimal addAmt = one.getT1amt().subtract(one.getStartAmt());
                                tpl = String.format(tpl, lastDate, addAmt, one.getVal(), endDate, one.getVal());
                                resultCustomVo.setContent(tpl);
                            }
                            iLnCdldRecordService.updateById(one);
                        } else {
                            String tpl = defineMap.get("notForDateTpl");
                            String lastDate = DateUtil.format(DateUtil.parse(one.getT1date() + "", "yyyyMMdd"), "yyyy年MM月dd日");
                            String endDate = DateUtil.format(DateUtil.offsetDay(one.getT1dateTime(), 30), "yyyy年MM月dd日");
                            BigDecimal addAmt = one.getT1amt().subtract(one.getStartAmt());
                            tpl = String.format(tpl, lastDate, addAmt, endDate, one.getVal());
                            resultCustomVo.setContent(tpl);
                        }
//                        if (t30_bal.subtract(one.getStartAmt()).compareTo(val) >= 0) {
//                            one.setT2amt(t30_bal);
//                            one.setT2date(Integer.parseInt(resultMap.get("t30_date").toString()));
//                            one.setT2dateTime(new Date());
//                            one.setUpdateTime(new Date());
//                            if (DateUtil.betweenDay(one.getT1dateTime(), one.getT2dateTime(), false) >= 30) {
//                                one.setT2end(true);
//                                iLnCdldRecordService.updateById(one);
//                                // 达标了
//                                resultCustomVo.setSuccess(true);
//                                resultCustomVo.setContent(defineMap.get("achieveTpl"));
//                            } else {
//                                iLnCdldRecordService.updateById(one);
//                                String tpl = defineMap.get("notForDateTpl");
//                                String lastDate = DateUtil.format(one.getT2dateTime(), "yyyy年MM月dd日");
//                                String endDate = DateUtil.format(DateUtil.offsetDay(one.getT1dateTime(), 30), "yyyy年MM月dd日");
//                                BigDecimal addAmt = one.getT2amt().subtract(one.getStartAmt());
//                                tpl = String.format(tpl, lastDate, addAmt, endDate, valStr);
//                                resultCustomVo.setContent(tpl);
//                            }
//                        } else {
//                            if (one.getT2date() > 0) {
//                                String tpl = defineMap.get("notForChangeTpl");
//                                String lastDate = DateUtil.format(one.getT2dateTime(), "yyyy年MM月dd日");
//                                String endDate = DateUtil.format(DateUtil.offsetDay(one.getT1dateTime(), 30), "yyyy年MM月dd日");
//                                BigDecimal addAmt = one.getT2amt().subtract(one.getStartAmt());
//                                tpl = String.format(tpl, lastDate, addAmt, valStr, endDate, valStr);
//                                resultCustomVo.setContent(tpl);
//                            } else {
//                                //String tpl = defineMap.get("notTpl");
//                                String tpl = defineMap.get("notForDateTpl");
//                                String lastDate = DateUtil.format(one.getT2dateTime(), "yyyy年MM月dd日");
//                                String endDate = DateUtil.format(DateUtil.offsetDay(one.getT1dateTime(), 30), "yyyy年MM月dd日");
//                                BigDecimal addAmt = one.getT2amt().subtract(one.getStartAmt());
//                                tpl = String.format(tpl, lastDate, addAmt, endDate, valStr);
//                                resultCustomVo.setContent(tpl);
//                            }
//                        }
                    } else {
                        resultCustomVo.setContent("没有领取一重礼");
                    }
                } else if ("whitelist".equals(type)) {
                    // 统一白名单到这里就可以直接返回true了
                    String valStr = resultMap.get("val").toString();
                    resultCustomVo.setSuccess(true);
                    resultCustomVo.setContent(String.format(defineMap.get("tpl"), valStr));
                } else if ("t2whitelist".equals(type)) {
                    // 二重李达标白名单
                    if (one != null && one.getT1date() > 0) {
                        resultCustomVo.setSuccess(true);
                    } else {
                        resultCustomVo.setContent("没有领取一重礼");
                    }
                }
                return resultCustomVo;
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                return process(accountId, defineMap, actMap, resultMap);
            }
        });
        return (ResultCustomVo) object;
    }

    @Override
    public Map<String, Object> getParams(String accountId) {
        QueryWrapper<LnCdldRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("act_account_id", accountId);
        LnCdldRecord one = iLnCdldRecordService.getOne(queryWrapper);
        if (one != null) {
            Map<String, Object> map = new HashMap<>();
//            DateTime time = DateUtil.parse(Integer.toString(one.getT1date()), "yyyyMMdd");
//            time = DateUtil.offsetDay(time, -1);
//            map.put("receive_time", DateUtil.format(time, "yyyyMMdd"));
            map.put("receive_time", one.getT1date());
            return map;
        }
        return null;
    }
}
