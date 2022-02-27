package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.DynamicCode;

import java.util.List;

/**
 * 动态代码数据处理层
 * @author zhuzheng
 */
public interface DynamicCodeDao extends BaseDao<DynamicCode,String> {

    List<DynamicCode> findByAppid(String appid);
}