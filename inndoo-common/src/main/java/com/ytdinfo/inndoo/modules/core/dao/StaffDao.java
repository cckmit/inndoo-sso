package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.modules.core.entity.Staff;

import java.util.List;

/**
 * 员工信息数据处理层
 * @author Timmy
 */
public interface StaffDao extends BaseDao<Staff,String> {

    /**
     * 通过员工名获取员工
     * @param name
     * @return
     */
    List<Staff> findByNameAndAppid(String name,String appid);

    List<Staff> findByStaffNoIn(List<String> staffNos);

    Staff findByStaffNo(String staffNo);

	List<Staff> findByPhoneAndAppid(String phone, String appid);

	List<Staff> findByDeptNo(String id);

	List<Staff> findByAccountIdIn(List<String> accountIds);

    Staff findByAccountId(String accountId);

    List<Staff> findByDeptNoIn(List<String> deptNos);

    long countByDeptNoIn(List<String> deptNos);
}