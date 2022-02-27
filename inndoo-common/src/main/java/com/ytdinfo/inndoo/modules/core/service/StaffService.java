package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.BusinessManagerVo;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 员工信息接口
 * @author Timmy
 */
public interface StaffService extends BaseService<Staff,String> {

    /**
    * 多条件分页获取
    * @param staff
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<Staff> findByCondition(Staff staff, SearchVo searchVo, Pageable pageable);

    /**
     * 通过员工名获取员工
     * @param name
     * @return
     */
//    @Cacheable(key = "#name")
    List<Staff> findByName(String name);

    List<Staff> findByStaffs(List<String> staffNos);

    List<Staff> listByIds(List<String> list);

    void loadCache();

    void addToCache(String accountId);

    void removeFromCache(String accountId);

    Boolean validate(String accountId);

    Staff findByStaffNo(String staffNO);

	List<Staff> findByPhone(String phone);

	List<Staff> findByDeptNo(String id);

    List<Staff> findByAccountIds(List<String> accountIds);

    Staff findByAccountId(String accountId);

    List<Staff> findAllByParentDeptNo(String departmentId);

    Long countAllByParentDeptNo(String departmentId);

    List<Staff> findBatchfindByAccountIds(List<String> accountIds,int num);

    /**
     * 根据角色code查询staff
     * @param
     * @return
     */
    List<BusinessManagerVo> queryStaffByRoleCode(String roleCde);

    void updateRecommendFlag(Integer recommendFlag,String id);

    String getAdvBusinessManager(String staffIds,String roleCode,Integer recommendFlag);

    /**
     * 查询业务经理详情
     * @param id
     * @return
     */
    BusinessManagerVo getBusinessManagerById(String id);

}