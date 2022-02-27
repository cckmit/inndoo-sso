package com.ytdinfo.inndoo.modules.base.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.ModifyDepartmentVo;
import com.ytdinfo.inndoo.modules.base.entity.Department;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门接口
 * @author Exrick
 */
public interface DepartmentService extends BaseService<Department,String> {

    /**
     * 通过父id获取 升序
     * @param parentId
     * @param openDataFilter 是否开启数据权限
     * @return
     */
    List<Department> findByParentIdOrderBySortOrder(String parentId, Boolean openDataFilter);

    /**
     * 通过父id和状态获取
     * @param parentId
     * @param status
     * @return
     */
    List<Department> findByParentIdAndStatusOrderBySortOrder(String parentId, Integer status);

    /**
     * 部门名模糊搜索 升序
     * @param title
     * @param openDataFilter 是否开启数据权限
     * @return
     */
    List<Department> findByTitleLikeOrderBySortOrder(String title, Boolean openDataFilter);

    List<Department> findByTitleLikeOrDeptCodeLikeOrderBySortOrder(String title,String deptCode, Boolean openDataFilter);

    List<Department> findByDeptCodes(List<String> deptNos);

    List<Department> findAllToTree();

    void deleteAll();

    List<Department> findByIdIn(List<String> deptIds);

	Department findByDeptCode(String deptCode);

    List<Department> findByAppid(String appid);

    List<Department> findByAppidAndParentIdAndStatus(String appid,String parentId, Integer status);

    List<Department> transformToTree(List<Department> paramList, Map<String,Object> map);

    Department findNodeByParentid(Department rootNode , String parentid);

    void tranRow( Department tempNode, LinkedList<LinkedList<String>> row, LinkedList<String> repeatRow);

    List<Department> findByTitle(String title);

    Department selectById(String id);

    List<Department> findParentDepartmentById(String id);

    ModifyDepartmentVo getModiifyDepartment(String id,Department  newdepartment);

    ModifyDepartmentVo getModiifyDepartment(String id);

    ModifyDepartmentVo getModiifyDepartment(Department department);

    Integer countLevel();

    List<Department> findAllDepartmentByPId(String departmentId);

    /**
     * 根据过滤部门id集合 返回树形结构
     * @param filterIds  过滤部门ID集合
     */
    List<Department> filterTree(List<String> filterIds);

}