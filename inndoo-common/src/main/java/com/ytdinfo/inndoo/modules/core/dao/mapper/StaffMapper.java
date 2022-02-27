package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.common.vo.BusinessManagerVo;
import com.ytdinfo.inndoo.modules.base.vo.StaffSearchVo;
import com.ytdinfo.inndoo.modules.core.dto.SearchStaffDto;
import com.ytdinfo.inndoo.modules.core.dto.StaffDto;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.access.method.P;

import java.util.List;
import java.util.Map;

public interface StaffMapper extends BaseInndooMapper<Staff> {

    List<Staff> findByMap(Map<String,Object> map);

    List<StaffDto> find2LevelStaffBySearchStaffDto(SearchStaffDto search);

    List<StaffDto> find3LevelStaffBySearchStaffDto(SearchStaffDto search);

    Staff findByStaffNo(String staffNo);

    List<Staff> findWithoutRole();

    int batchUpdateImg(List<Staff> smallBatch);

    Staff findById(String id);

    IPage<Staff> listInfoForHelper(Page initMpPage,  @Param("searchVo") StaffSearchVo searchVo);

    Integer getStaffCount(String appid);

    List<Staff> getStaffData( Map<String,Object> map);

    Integer aesDataSwitchPassword(Map<String,Object> map);

    Integer countByMap(Map<String,Object> map);

    List<BusinessManagerVo> queryStaffByRoleCode(@Param("roleCode")String roleCode, @Param("recommendFlag")Integer recommendFlag);

    void updateRecommendFlag(@Param("recommendFlag")Integer recommendFlag, @Param("id")String id);

    List<String> validBusinessManagerByStaffId(@Param("list")List<String> staffIds,@Param("roleCode")String roleCode);

    BusinessManagerVo searchStaffById(@Param("id") String id);
}
