package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.common.vo.BindLogSearchVo;
import com.ytdinfo.inndoo.common.vo.BindLogVo;
import com.ytdinfo.inndoo.modules.core.entity.BindLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 绑定日志数据处理层
 *
 * @author haiqing
 */
public interface BindLogMapper extends BaseInndooMapper<BindLog> {

    IPage<BindLogVo> listForHelper(Page page, @Param("searchVo") BindLogSearchVo searchVo);
}