package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;


public interface RoleStaffMapper extends BaseInndooMapper<RoleStaff> {


    RoleStaff findByCode(String code);
}
