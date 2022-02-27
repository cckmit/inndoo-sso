package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.modules.core.dao.mapper.LnCdldRecordMapper;
import com.ytdinfo.inndoo.modules.core.entity.LnCdldRecord;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILnCdldRecordService;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 辽宁存贷联动记录表接口实现
 * @author haiqing
 */
@Slf4j
@Service
public class ILnCdldRecordServiceImpl extends BaseServiceImpl<LnCdldRecordMapper, LnCdldRecord> implements ILnCdldRecordService {

    @Autowired
    private LnCdldRecordMapper lnCdldRecordMapper;
}