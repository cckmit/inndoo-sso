package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.vo.BindLogSearchVo;
import com.ytdinfo.inndoo.common.vo.BindLogVo;
import com.ytdinfo.inndoo.modules.core.dao.mapper.BindLogMapper;
import com.ytdinfo.inndoo.modules.core.entity.BindLog;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IBindLogService;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 绑定日志接口实现
 *
 * @author haiqing
 */
@Slf4j
@Service
public class IBindLogServiceImpl extends BaseServiceImpl<BindLogMapper, BindLog> implements IBindLogService {

    @Autowired
    private BindLogMapper bindLogMapper;

    @Override
    public IPage<BindLogVo> listForHelper(BindLogSearchVo searchVo) {
        return bindLogMapper.listForHelper(PageUtil.initMpPage(searchVo.getPageVo()), searchVo);
    }
}