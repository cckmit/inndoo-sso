package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.vo.SmsCaptchaLogVo;
import com.ytdinfo.inndoo.common.vo.SmsSendLogSearchVo;
import com.ytdinfo.inndoo.modules.core.dao.mapper.SmsCaptchaLogMapper;
import com.ytdinfo.inndoo.modules.core.entity.SmsCaptchaLog;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ISmsCaptchaLogService;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 短信发送记录接口实现
 *
 * @author haiqing
 */
@Slf4j
@Service
public class ISmsCaptchaLogServiceImpl extends BaseServiceImpl<SmsCaptchaLogMapper, SmsCaptchaLog> implements ISmsCaptchaLogService {

    @Autowired
    private SmsCaptchaLogMapper smsCaptchaLogMapper;

    @Override
    public IPage<SmsCaptchaLogVo> listForHelper(SmsSendLogSearchVo searchVo) {
        return smsCaptchaLogMapper.listForHelper(PageUtil.initMpPage(searchVo.getPageVo()), searchVo);
    }
}