package com.ytdinfo.inndoo.vo;

import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class IsStaffVo implements Serializable {
    private List<RoleStaff> roleStaffs;//员工角色

    private Boolean isStaff;
}
