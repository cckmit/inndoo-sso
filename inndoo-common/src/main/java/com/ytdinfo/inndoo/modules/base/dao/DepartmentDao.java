package com.ytdinfo.inndoo.modules.base.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 部门数据处理层
 * @author Exrick
 */
public interface DepartmentDao extends BaseDao<Department,String> {

    /**
     * 通过父id获取 升序
     * @param parentId
     * @return
     */
    List<Department> findByParentIdOrderBySortOrder(String parentId);

    /**
     * 通过父id获取 升序 数据权限
     * @param parentId
     * @param departmentIds
     * @return
     */
    List<Department> findByParentIdAndIdInOrderBySortOrder(String parentId, List<String> departmentIds);

    /**
     * 通过父id和状态获取 升序
     * @param parentId
     * @param status
     * @return
     */
    List<Department> findByParentIdAndStatusOrderBySortOrder(String parentId, Integer status);

    /**
     * 部门名模糊搜索 升序
     * @param title
     * @return
     */
    List<Department> findByTitleLikeOrderBySortOrder(String title);

    /**
     * 部门名,部门编号 模糊搜索 升序
     * @param title
     * @return
     */
    List<Department> findByTitleLikeOrDeptCodeLikeOrderBySortOrder(String title,String deptCode);

    /**
     * 部门名模糊搜索 升序 数据权限
     * @param title
     * @param departmentIds
     * @return
     */
    List<Department> findByTitleLikeAndIdInOrderBySortOrder(String title, List<String> departmentIds);

    /**
     * 部门名,部门编号 模糊搜索 升序 数据权限
     * @param title
     * @param departmentIds
     * @return
     */
    List<Department> findByTitleLikeOrDeptCodeLikeAndIdInOrderBySortOrder(String title,String deptCode,List<String> departmentIds);


    @Modifying
    @Query(value = ("select id,create_by,create_time,update_by,update_time,parent_id,sort_order,`status`,title,is_parent,is_deleted,dept_code from t_department where dept_code in (:deptCodes) and is_deleted = 0"),nativeQuery = true)
    List<Department> findByDeptCodes(@Param("deptCodes") List<String> deptCodes);

    /**
     * 查询所有部门
     * @param status
     * @return
     */
    List<Department> findByStatusAndAppid(Integer status,String appid);

    List<Department> findByIdIn(List<String> ids);

    @Query(value = "select * from t_department where dept_code = ?1 and is_deleted = 0",nativeQuery = true)
	Department findByDeptCode(String deptCode);

    List<Department> findByAppid(String appid);

    List<Department> findByTitleAndAppid(String title,String appid);

    List<Department> findByAppidAndParentIdAndStatus(String appid, String parentId, Integer status);
}