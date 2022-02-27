package com.ytdinfo.inndoo.modules.base.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.base.entity.DepartmentHeader;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 部门负责人数据处理层
 * @author Exrick
 */
public interface DepartmentHeaderDao extends BaseDao<DepartmentHeader,String> {

    /**
     * 通过部门和负责人类型获取
     * @param departmentId
     * @param type
     * @return
     */
    List<DepartmentHeader> findByDepartmentIdAndType(String departmentId, Integer type);

    /**
     * 通过部门获取
     * @param departmentIds
     * @return
     */
    List<DepartmentHeader> findByDepartmentIdIn(List<String> departmentIds);

    /**
     * 通过部门id删除
     * @param departmentId
     */
    @Transactional(rollbackFor = Exception.class)
    void deleteByDepartmentId(String departmentId);

    /**
     * 通过userId删除
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    void deleteByUserId(String userId);
}