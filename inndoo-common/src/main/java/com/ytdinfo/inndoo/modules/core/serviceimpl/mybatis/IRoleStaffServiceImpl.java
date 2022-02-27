package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.RoleStaffMapper;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IRoleStaffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信发送记录接口实现
 *
 * @author haiqing
 */
@Slf4j
@Service
public class IRoleStaffServiceImpl extends BaseServiceImpl<RoleStaffMapper, RoleStaff> implements IRoleStaffService {

    @Autowired
    private RoleStaffMapper roleStaffMapper;

    @Override
    public RoleStaff findByCode( String code){
        return roleStaffMapper.findByCode(code);
    }

}