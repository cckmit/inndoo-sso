package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.script.ScriptUtil;

import javax.script.SimpleBindings;
import java.util.*;

public class TestMain {
    public static void main(String[] args) throws Exception {
//        String apiSandboxScript = "function func1(){ var tmp = data[0]; var val = tmp.val; var content = '当前中行一类储蓄卡资产的新增金额（不含股票市值与财富产品未结算收益）比本期房贷还款金额高' +val + '元，即可进行抽奖，抽奖时需保持资产达标状态。'; var result = {}; result.content = content; return result; } func1();";
//
//        apiSandboxScript = HtmlUtil.unescape(apiSandboxScript);
//        Map<String, Object> data = new HashMap<>();
//        data.put("val",31.2);
//        data.put("t2_bal",1);
//        List<Map<String,Object>> list = new ArrayList<>();
//        list.add(data);
//        SimpleBindings simpleBindings = new SimpleBindings();
//        simpleBindings.put("data", list);
//        Object obj = ScriptUtil.eval(apiSandboxScript, simpleBindings);
//        System.out.println(obj.toString());
//        String s = JSONUtil.toJsonStr(obj);
//        System.out.println(s);
//        Map returnObj = JSONUtil.toBean(s, Map.class);
//        System.out.println(returnObj);

//        ScriptingSandbox scriptingSandbox = new ScriptingSandbox(ScriptUtil.getJavaScriptEngine());
//        Object obj = scriptingSandbox.eval(apiSandboxScript, data);
//        String s = JSONUtil.toJsonStr(obj);
//        System.out.println(s);
        Date now = new Date();
        Date begin = DateUtil.parse("2020-08-30 23:00:00","yyyy-MM-dd HH:mm:ss");
        long l = DateUtil.betweenDay(begin, now, true);
        System.out.println(l);
    }
}
