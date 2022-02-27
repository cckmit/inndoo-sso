package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.common.vo.BindLogSearchVo;
import com.ytdinfo.inndoo.common.vo.BindLogVo;
import com.ytdinfo.inndoo.modules.core.entity.BindLog;

import java.util.List;

/**
 * 绑定日志接口
 * @author haiqing
 */
public interface IBindLogService extends IService<BindLog> {

    IPage<BindLogVo> listForHelper(BindLogSearchVo searchVo);
}