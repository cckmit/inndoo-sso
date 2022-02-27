package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.base.vo.StaffSearchVo;
import com.ytdinfo.inndoo.modules.core.dao.mapper.StaffMapper;
import com.ytdinfo.inndoo.modules.core.dto.SearchStaffDto;
import com.ytdinfo.inndoo.modules.core.dto.StaffDto;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import com.ytdinfo.inndoo.modules.core.service.RoleStaffService;
import com.ytdinfo.inndoo.modules.core.service.StaffRoleService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 员工数据
 * @author Jxy
 */
@Slf4j
@Service
public class IStaffServiceImpl extends BaseServiceImpl<StaffMapper, Staff> implements IStaffService {

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private RoleStaffService roleStaffService;

    @Autowired
    private StaffRoleService staffRoleService;

    @Autowired
    private RedisTemplate<String,Staff> redisTemplate;

    @Override
    public List<Staff> findByMap(Map<String, Object> map) {
        return staffMapper.findByMap(map);
    }

    @Override
    public List<StaffDto> find2LevelStaffBySearchStaffDto(SearchStaffDto search) {
        return staffMapper.find2LevelStaffBySearchStaffDto(search);
    }

    @Override
    public List<StaffDto> find3LevelStaffBySearchStaffDto(SearchStaffDto search) {
        return staffMapper.find3LevelStaffBySearchStaffDto(search);
    }

    /**
     * 查询未绑定 员工角色的用户
     * @return
     */
    @Override
    public List<Staff> findWithoutRole() {
        return staffMapper.findWithoutRole();
    }

    /**
     * 批量更新 员工的头像和二维码
     * @param updateList
     * @return
     */
    @Override
    @Transactional
    public int updateImg(List<Staff> updateList){
        if(updateList== null || updateList.size() <1){
            return 0;
        }
        List<Staff> smallBatch = new ArrayList<>();
        int affect = 0;
        Set<String>  redisKeys = new HashSet<>();
        Set<String>  cacheKeys = new HashSet<>();
        for(Staff staff :updateList){
            redisKeys.add( "img_staff::"+staff.getId());
            redisKeys.add("Staff::" + staff.getId());
            if(StrUtil.isNotBlank(staff.getDeptNo())){
                cacheKeys.add("contact_stafflist::"+staff.getDeptNo());
            }
        }
        String cacheKey = "contact_departmentlist::all";
        redisKeys.add(cacheKey);
        for (int i = 0; i < updateList.size(); i++) {
            smallBatch.add(updateList.get(i));
            if (i != 0 && i % 2000 == 0) {
                affect += staffMapper.batchUpdateImg(smallBatch);
                smallBatch.clear();
            }
        }
        if (smallBatch.size() <= 2000) {
            affect += staffMapper.batchUpdateImg(smallBatch);
        }

        redisTemplate.delete(redisKeys);
        redisTemplate.delete(cacheKeys);
        return affect;
    }


    /**
     * 查询用户(包含用户头像)
     * @param id
     * @return
     */
    @Override
    public Staff findImgStaffById(String id){
        String cacheKey = "img_staff::"+id;
        Staff staff =  redisTemplate.opsForValue().get(cacheKey);
        if(staff != null){
            return staff;
        }
        staff = staffMapper.findById(id);
        if(staff != null){
            redisTemplate.opsForValue().set(cacheKey,staff);
            redisTemplate.expire(cacheKey,1, TimeUnit.DAYS);
        }
        return staff;
    }


    @Override
    @Transactional
    public boolean saveBatchOnDuplicateUpdate(Collection<Staff> entityList, int batchSize) {
        String sqlStatement = SqlHelper.table(currentModelClass()).getSqlStatement("insertOnDuplicateUpdate");
        try (SqlSession batchSqlSession = sqlSessionBatch()) {
            int i = 0;
            for (Staff anEntityList : entityList) {
                batchSqlSession.insert(sqlStatement, anEntityList);
                if (i >= 1 && i % batchSize == 0) {
                    batchSqlSession.flushStatements();
                }
                i++;
            }
            batchSqlSession.flushStatements();
        }
        Set<String> cacheKeys = new HashSet<>();
        for (Staff staff : entityList) {
            if(StrUtil.isNotBlank(staff.getRoleIds())){
                List<String> roleIds = Arrays.asList(StrUtil.split(staff.getRoleIds(),","));
                List<RoleStaff> roles = roleStaffService.findByIdIn(roleIds);
                //获取默认角色
                if(null != roles && roles.size() > 0 ) {
                    staffRoleService.deleteByStaffId(staff.getId());
                    for (RoleStaff role : roles) {
                        StaffRole ur = new StaffRole();
                        ur.setStaffId(staff.getId());
                        ur.setRoleId(role.getId());
                        staffRoleService.save(ur);
                    }
                    if(StrUtil.isNotBlank(staff.getDeptNo())){
                        cacheKeys.add("contact_stafflist::"+staff.getDeptNo());
                    }
                }
            }
        }
        String cacheKey = "contact_departmentlist::all";
        cacheKeys.add(cacheKey);
        redisTemplate.delete(cacheKeys);
        return true;
    }

    @Override
    public IPage<Staff> listInfoForHelper(Page initMpPage, StaffSearchVo searchVo) {
        IPage<Staff> result = staffMapper.listInfoForHelper(initMpPage, searchVo);
        return result;
    }

    @Override
    public  Integer getStaffCount(String appid)
    {
        return staffMapper.getStaffCount(appid);
    }


    @Override
    public  List<Staff> getStaffData(String wxappid,Integer start,Integer end)
    {
        Map<String,Object> map = new HashMap<>();
        map.put("appid",wxappid);
        map.put("start",start);
        map.put("end",end);
        return staffMapper.getStaffData(map);
    }

    @Override
    public Integer aesDataSwitchPassword(Map<String,Object> map)
    {
        return staffMapper.aesDataSwitchPassword(map);
    }

    @Override
    public Integer countByMap(Map<String, Object> map) {
        return staffMapper.countByMap(map);
    }

}
