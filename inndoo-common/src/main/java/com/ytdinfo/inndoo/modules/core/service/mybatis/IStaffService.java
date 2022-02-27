package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ytdinfo.inndoo.base.mybatis.BaseIService;
import com.ytdinfo.inndoo.modules.base.vo.StaffSearchVo;
import com.ytdinfo.inndoo.modules.core.dto.SearchStaffDto;
import com.ytdinfo.inndoo.modules.core.dto.StaffDto;
import com.ytdinfo.inndoo.modules.core.entity.Staff;

import java.util.List;
import java.util.Map;

public interface IStaffService extends BaseIService<Staff> {

    List<Staff> findByMap(Map<String,Object> map);

    /**
     * 查找机构为2层结构的员工
     * @param search
     * @return
     */
    List<StaffDto> find2LevelStaffBySearchStaffDto(SearchStaffDto search);

    /**
     * 查找机构为3层结构的员工
     * @param search
     * @return
     */
    List<StaffDto> find3LevelStaffBySearchStaffDto(SearchStaffDto search);

    /**
     * 查询未绑定 员工角色的用户
     * @return
     */
    List<Staff> findWithoutRole();

    /**
     * 批量更新 员工的头像和二维码
     * @param updateList
     * @return
     */
    int updateImg(List<Staff> updateList);

    /**
     * 查询用户(包含用户头像)
     * @param id
     * @return
     */
    Staff findImgStaffById(String id);

    Integer getStaffCount(String appid);

    List<Staff> getStaffData(String wxappid,Integer start,Integer end) ;

    IPage<Staff> listInfoForHelper(Page initPage, StaffSearchVo staffSearchVo);

    Integer aesDataSwitchPassword(Map<String,Object> map);

    Integer countByMap(Map<String,Object> map);

}
