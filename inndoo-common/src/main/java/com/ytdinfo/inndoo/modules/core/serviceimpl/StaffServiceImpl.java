package com.ytdinfo.inndoo.modules.core.serviceimpl;
import java.util.LinkedList;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.vo.BusinessManagerVo;
import com.ytdinfo.inndoo.modules.base.entity.Department;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.ActivityApiUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.vo.ModifyStaffVo;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.core.dao.StaffDao;
import com.ytdinfo.inndoo.modules.core.dao.mapper.StaffMapper;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.entity.RoleStaff;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import com.ytdinfo.inndoo.modules.core.entity.StaffRole;
import com.ytdinfo.inndoo.modules.core.service.RoleStaffService;
import com.ytdinfo.inndoo.modules.core.service.StaffRoleService;
import com.ytdinfo.inndoo.modules.core.service.StaffService;
import com.ytdinfo.inndoo.modules.core.service.ActAccountService;
import com.ytdinfo.util.StringUtils;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 员工信息接口实现
 *
 * @author Timmy
 */
@Slf4j
@Service

@CacheConfig(cacheNames = "Staff")
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private StaffDao staffDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ActAccountService ActAccountService;

    @Override
    public StaffDao getRepository() {
        return staffDao;
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String,List<Staff>> StaffListTemplate;

    @Autowired
    private RoleStaffService roleStaffService;

    @Autowired
    private StaffRoleService staffRoleService;

    private void cleanRedisCacheByDepartmentId(String departmentId){
        String staffkey = "department_allstaff::"+departmentId;
        redisTemplate.delete(staffkey);
        String staffNumkey = "department_allstaff_num::"+departmentId;
        redisTemplate.delete(staffNumkey);
    }

    private void cleanRedisCacheByDepartmentIds(List<String> departmentIds){
        if(CollectionUtil.isEmpty(departmentIds)){
            return;
        }
        for(String departmentId:departmentIds ){
            cleanRedisCacheByDepartmentId(departmentId);
        }
    }

    private void cleanRedisCacheByDepartments(List<Department> departmentIds){
        if(CollectionUtil.isEmpty(departmentIds)){
            return;
        }
        for(Department department:departmentIds ){
            cleanRedisCacheByDepartmentId(department.getId());
            String cacheKey = "contact_stafflist::"+department.getId();
            redisTemplate.delete(cacheKey);
        }
        String cacheKey = "contact_departmentlist::all";
        redisTemplate.delete(cacheKey);
    }

    /**
     * 根据父级部门id 获取其所有子级部门的所有行员
     * @param departmentId
     * @return
     */
    @Override
    public List<Staff> findAllByParentDeptNo(String departmentId){
        String staffkey = "department_allstaff::"+departmentId;
        List<Staff> staff = StaffListTemplate.opsForValue().get(staffkey);
        if(staff == null || staff.size()<1){
            List<Department> departments = departmentService.findAllDepartmentByPId(departmentId);
            List<String> departmentIds = new ArrayList<>();
            for(Department department: departments){
                departmentIds.add(department.getId());
            }
            departmentIds.add(departmentId);
            staff = staffDao.findByDeptNoIn(departmentIds);
            StaffListTemplate.opsForValue().set(staffkey,staff);
            StaffListTemplate.expire(staffkey,1, TimeUnit.DAYS);
        }
        return staff;
    }

    @Override
    public Long countAllByParentDeptNo(String departmentId){
        String staffNumkey = "department_allstaff_num::"+departmentId;
        String v = stringRedisTemplate.opsForValue().get(staffNumkey);
        Long num = 0L;
        if(StrUtil.isNotBlank(v)){
            num = Long.parseLong(v);
        }else{
            List<Department> departments = departmentService.findAllDepartmentByPId(departmentId);
            List<String> departmentIds = new ArrayList<>();
            for(Department department: departments){
                departmentIds.add(department.getId());
            }
            departmentIds.add(departmentId);
            num =  staffDao.countByDeptNoIn(departmentIds);
            stringRedisTemplate.opsForValue().set(staffNumkey,num.toString());
            stringRedisTemplate.expire(staffNumkey,1, TimeUnit.DAYS);
        }
        return num;
    }

    @Override
    public List<Staff> findBatchfindByAccountIds(List<String> accountIds, int num) {
        List<Staff> allStaffs = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(accountIds)) {
            num = (num > 0) ? num : 10000;
            int length = accountIds.size();
            if (length <= num) {
                allStaffs = getRepository().findByAccountIdIn(accountIds);
            } else {
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = accountIds.subList(i * num, (i + 1) * num);
                    if (CollectionUtil.isNotEmpty(temp)) {
                        List<Staff> selectStaffs = getRepository().findByAccountIdIn(temp);
                        if (CollectionUtil.isNotEmpty(selectStaffs)) {
                            allStaffs.addAll(selectStaffs);
                        }
                    }
                }
                List<String> temp1 = accountIds.subList(times * num, length);
                if (CollectionUtil.isNotEmpty(temp1)) {
                    List<Staff> selectStaffs = getRepository().findByAccountIdIn(temp1);
                    if (CollectionUtil.isNotEmpty(selectStaffs)) {
                        allStaffs.addAll(selectStaffs);
                    }
                }
            }
        }
        return allStaffs;

    }


    /**
     * 根据父级部门id 获取其所有子级部门的所有行员
     * @param departmentId
     * @return
     */
    public List<String> findAllCoreAccountIdByParentDeptNo(String departmentId){
        List<Staff> staffs = findAllByParentDeptNo(departmentId);
        List<String> coreAccountID = new ArrayList<>();
        for(Staff staff:staffs){
            if(StrUtil.isNotBlank(staff.getAccountId())){
                coreAccountID.add(staff.getAccountId());
            }
        }
        return coreAccountID;
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    public Staff get(String id) {
        Optional<Staff> entity = getRepository().findById(id);
        if (!entity.isPresent()) {
            return null;
        }
        Staff staff = entity.get();
        Staff decryptStaff = new Staff();
        String name = staff.getName();
        String phone = staff.getPhone();
        BeanUtils.copyProperties(staff, decryptStaff);
        if (StrUtil.isNotBlank(name)) {
            decryptStaff.setName(AESUtil.decrypt(name));
        }
        if (StrUtil.isNotBlank(phone)) {
            decryptStaff.setPhone(AESUtil.decrypt(phone));
        }
        return decryptStaff;
    }

    /**
     * 保存
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    @Transactional
    public Staff save(Staff entity) {
        Staff staff = getRepository().save(entity);
        String imgcacheKey = "img_staff::"+entity.getId();
        redisTemplate.delete(imgcacheKey);
        if(StrUtil.isNotBlank(entity.getRoleIds())){
            List<String> roleIds = Arrays.asList(StrUtil.split(entity.getRoleIds(),","));
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
                    String cacheKey = "contact_stafflist::"+staff.getDeptNo();
                    redisTemplate.delete(cacheKey);
                }
                String cacheKey = "contact_departmentlist::all";
                redisTemplate.delete(cacheKey);
            }

        }
        String accountId = entity.getAccountId();
        Staff decryptStaff = new Staff();
        String name = staff.getName();
        String phone = staff.getPhone();
        BeanUtils.copyProperties(staff, decryptStaff);
        if (StrUtil.isNotBlank(name)) {
            decryptStaff.setName(AESUtil.decrypt(name));
        }
        if (StrUtil.isNotBlank(phone)) {
            decryptStaff.setPhone(AESUtil.decrypt(phone));
        }
        List<Department> newList = departmentService.findParentDepartmentById(entity.getDeptNo());
        cleanRedisCacheByDepartments(newList);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    if (StrUtil.isNotEmpty(accountId)) {
                        addToCache(accountId);
                    }
                }
            });
        } else {
            if (StrUtil.isNotEmpty(accountId)) {
                addToCache(accountId);
            }
        }
      /*  List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = getModifyStaffVo(entity.getId(),departments);
        modifyStaffVo.setType("Modify");
        activityApiUtil.modifyStaff(modifyStaffVo);*/
        return decryptStaff;
    }

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_10MUNITE)
    @Override
    @Transactional
    public Staff update(Staff entity) {
        List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = getModifyStaffVo(entity.getId(),departments);
        cleanRedisCacheByDepartments(modifyStaffVo.getOldDepartmentList());
        Staff staff = getRepository().saveAndFlush(entity);
        String imgcacheKey = "img_staff::"+entity.getId();
        redisTemplate.delete(imgcacheKey);
        if(StrUtil.isNotBlank(entity.getRoleIds())){
            List<String> roleIds = Arrays.asList(StrUtil.split(entity.getRoleIds(),","));
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
                    String cacheKey = "contact_stafflist::"+staff.getDeptNo();
                    redisTemplate.delete(cacheKey);
                }
                String cacheKey = "contact_departmentlist::all";
                redisTemplate.delete(cacheKey);
            }

        }
        String accountId = entity.getAccountId();
        if (StrUtil.isNotEmpty(accountId)) {
            addToCache(accountId);
        }
        Staff decryptStaff = new Staff();
        String name = staff.getName();
        String phone = staff.getPhone();
        BeanUtils.copyProperties(staff, decryptStaff);
        if (StrUtil.isNotBlank(name)) {
            decryptStaff.setName(AESUtil.decrypt(name));
        }
        if (StrUtil.isNotBlank(phone)) {
            decryptStaff.setPhone(AESUtil.decrypt(phone));
        }
        if(!StrUtil.equals(modifyStaffVo.getOldDeptNo(),staff.getDeptNo())){
            List<Department> newList = departmentService.findParentDepartmentById(staff.getDeptNo());
            cleanRedisCacheByDepartments(newList);
        }

       /* //修改同步到活动平台
        ModifyStaffVo SYNmodifyStaffVo = getModifyStaffVo(entity.getId(),departments);
        SYNmodifyStaffVo.setType("Modify");
        activityApiUtil.modifyStaff(SYNmodifyStaffVo);*/

        return decryptStaff;
    }

    /**
     * 删除
     *
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(Staff entity) {
        List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = getModifyStaffVo(entity.getId(),departments);
      /*  modifyStaffVo.setType("delete");
        activityApiUtil.modifyStaff(modifyStaffVo);*/
        cleanRedisCacheByDepartments(modifyStaffVo.getOldDepartmentList());
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
    @CacheEvict(key = "#id")
    @Override
    public void delete(String id) {
        List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = getModifyStaffVo(id,departments);
        cleanRedisCacheByDepartments(modifyStaffVo.getOldDepartmentList());
        Staff staff = getRepository().getOne(id);
        getRepository().deleteById(id);
        if(staff!=null){
            removeFromCache(staff.getAccountId());
        }
        /*modifyStaffVo.setType("delete");
        activityApiUtil.modifyStaff(modifyStaffVo);*/
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<Staff> saveOrUpdateAll(Iterable<Staff> entities) {
        List<ModifyStaffVo> modifyStaffVos = new ArrayList<>();
        List<Department> departments = departmentService.findAll();
        for(Staff entity:entities){
            ModifyStaffVo modifyStaffVo = getModifyStaffVo(entity.getId(),departments);
            if(modifyStaffVo != null){
                cleanRedisCacheByDepartments(modifyStaffVo.getOldDepartmentList());
                modifyStaffVos.add(modifyStaffVo);
            }
        }

        List<Staff> list = getRepository().saveAll(entities);
        Set<String> cacheKeys = new HashSet<>();
        for (Staff staff : entities) {
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
                    String cacheKey = "contact_departmentlist::all";
                    redisTemplate.delete(cacheKey);
                }
            }
        }
        redisTemplate.delete(cacheKeys);


       /* for(ModifyStaffVo modifyStaffVo:modifyStaffVos){
            activityApiUtil.modifyStaff(modifyStaffVo);
        }*/

        List<String> redisKeys = new ArrayList<>();
        for (Staff entity : entities) {
            List<Department> newList = departmentService.findParentDepartmentById(entity.getDeptNo());
            cleanRedisCacheByDepartments(newList);
            redisKeys.add("Staff::" + entity.getId());
            addToCache(entity.getAccountId());
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String[] ids) {
        StaffDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<Staff> list4Delete = repository.findAllById(list);
        for(Staff staff :list4Delete){
            List<Department> newList = departmentService.findParentDepartmentById(staff.getDeptNo());
            cleanRedisCacheByDepartments(newList);
        }
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        List<Department> departments = departmentService.findAll();
        for (String id : ids) {
            redisKeys.add("Staff::" + id);
           /* ModifyStaffVo modifyStaffVo = getModifyStaffVo(id,departments);
            modifyStaffVo.setType("delete");
            activityApiUtil.modifyStaff(modifyStaffVo);*/
            //同步到活动平台
        }
        redisTemplate.delete(redisKeys);
        //删除校验管理缓存
        for (Staff staff : list4Delete) {
            removeFromCache(staff.getAccountId());
        }


    }

    /**
     * 批量删除
     *
     * @param entities
     */

    @Override
    public void delete(Iterable<Staff> entities) {
        for(Staff staff :entities){
            List<Department> newList = departmentService.findParentDepartmentById(staff.getDeptNo());
            cleanRedisCacheByDepartments(newList);
        }
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        List<Department> departments = departmentService.findAll();
        for (Staff entity : entities) {
            redisKeys.add("Staff::" + entity.getId());
            removeFromCache(entity.getAccountId());
           /* ModifyStaffVo modifyStaffVo = getModifyStaffVo(entity.getId(),departments);
            modifyStaffVo.setType("delete");
            activityApiUtil.modifyStaff(modifyStaffVo);*/
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<Staff> findByCondition(Staff staff, SearchVo searchVo, Pageable pageable) {

        return staffDao.findAll(new Specification<Staff>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<Staff> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO 可添加你的其他搜索过滤条件 默认已有创建时间过滤
                Path<Date> createTimeField = root.get("createTime");
                Path<String> nameField = root.get("name");
                Path<String> staffNoField = root.get("staffNo");
                Path<String> phoneField = root.get("phone");
                Path<String> deptNoField = root.get("deptNo");
                Path<String> appidField = root.get("appid");
                Path<String> accountIdField = root.get("accountId");

                List<Predicate> list = new ArrayList<Predicate>();

                list.add(cb.equal(appidField, staff.getAppid()));

                //创建时间
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                //姓名
                if (StrUtil.isNotBlank(staff.getName())) {
                    String name = AESUtil.encrypt(staff.getName().trim());
                    list.add(cb.equal(nameField, name));
                }
                //工号
                if (StrUtil.isNotBlank(staff.getStaffNo())) {
                    list.add(cb.like(staffNoField, "%" + staff.getStaffNo().trim() + "%"));
                }
                //手机
                if (StrUtil.isNotBlank(staff.getPhone())) {
                    String phone = AESUtil.encrypt(staff.getPhone().trim());
                    list.add(cb.equal(phoneField, phone));
                }

                //部门号
                if (StrUtil.isNotBlank(staff.getDeptNo())) {
                    list.add(cb.equal(deptNoField, staff.getDeptNo().trim()));
                }

                //绑定账户id
                if (StrUtil.isNotBlank(staff.getAccountId())) {
                    ActAccount actAccount = ActAccountService.findByActAccountId(staff.getAccountId());
                    if (actAccount != null) {
                        list.add(cb.equal(accountIdField, StrUtil.trim(actAccount.getCoreAccountId())));
                    } else {
                        list.add(cb.equal(accountIdField, StrUtil.trim(staff.getAccountId())));
                    }
                }

                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    @Override
    public List<Staff> findByName(String name) {
        List<Staff> list = staffDao.findByNameAndAppid(AESUtil.encrypt(name.trim()),UserContext.getAppid());
        return list;
    }


    @Override
    public Staff findByStaffNo(String staffNo) {
        return staffDao.findByStaffNo(staffNo);
    }

    @Override
    public List<Staff> findByPhone(String phone) {
        phone = AESUtil.encrypt(phone.trim());
        return staffDao.findByPhoneAndAppid(phone,UserContext.getAppid());
    }

    @Override
    public List<Staff> findByStaffs(List<String> staffNos) {
        return getRepository().findByStaffNoIn(staffNos);
    }

    @Override
    public List<Staff> listByIds(List<String> list) {
        return getRepository().findAllById(list);
    }

    @Override
    public void loadCache() {
        Date d1 = new Date();
        String cacheKey = "Staff:All";
        BoundSetOperations boundSetOperations = redisTemplate.boundSetOps(cacheKey);
        int pageSize = 5000;
        PageVo page = new PageVo();
        page.setPageNumber(0);
        page.setPageSize(pageSize);
        page.setOrder(Sort.Direction.ASC.name());
        page.setSort("id");
        Pageable pageable = PageUtil.initPage(page);
        String id = "0";
        while (true) {
            final String finalId = id;
            List<Staff> listRecords = getRepository().findOnePage((root, query, criteriaBuilder) -> {
                List<Predicate> list = new ArrayList<Predicate>();
                Path<String> idField = root.get("id");
                list.add(criteriaBuilder.greaterThan(idField, finalId));
                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    query.where(list.toArray(arr));
                }
                return null;
            }, pageable);
            for (Staff record : listRecords) {
                String identifier = record.getAccountId();
                boundSetOperations.add(cacheKey, identifier);
            }
            if (listRecords.size() != pageSize) {
                break;
            }
            id = listRecords.get(pageSize - 1).getId();
        }
        Date d2 = new Date();
        System.out.println(d2.getTime() - d1.getTime());
    }

    @Override
    public void addToCache(String accountId) {
        if (StrUtil.isEmpty(accountId)) {
            return;
        }
        String cacheKey = "Staff:All";
        BoundSetOperations boundSetOperations = redisTemplate.boundSetOps(cacheKey);
        boundSetOperations.add(accountId);
    }

    @Override
    public void removeFromCache(String accountId) {
        if (StrUtil.isEmpty(accountId)) {
            return;
        }
        String cacheKey = "Staff:All";
        BoundSetOperations boundSetOperations = redisTemplate.boundSetOps(cacheKey);
        boundSetOperations.remove(accountId);
    }


    @Override
    public Boolean validate(String accountId) {
        String cacheKey = "Staff:All";
        BoundSetOperations boundSetOperations = redisTemplate.boundSetOps(cacheKey);
        return boundSetOperations.isMember(accountId);
    }

    @Override
    public List<Staff> findByDeptNo(String id) {
        return staffDao.findByDeptNo(id);
    }

    @Override
    public List<Staff> findByAccountIds(List<String> accountIds) {
        return staffDao.findByAccountIdIn(accountIds);
    }

    @Override
    public Staff findByAccountId(String accountId) {
        return staffDao.findByAccountId(accountId);
    }


    //保存数据库之前，用model
    public ModifyStaffVo getModifyStaffVo(List<Department> departments,Staff newStaff ){
        ModifyStaffVo modifyStaffVo = new ModifyStaffVo();
            if(newStaff != null){
                String departmentId = newStaff.getDeptNo();
                if(StrUtil.isNotBlank(departmentId)){
                    //List<Department> departments = departmentService.findAll();
                    if(departments.size()>0){
                        Map<String,Department> map = new HashMap<>();
                        for(Department department: departments){
                            map.put(department.getId(),department);
                        }
                        LinkedList<Department> list = new LinkedList<>();
                        while (map.get(departmentId) != null){
                            Department department = map.get(departmentId);
                            list.add(department);
                            departmentId = department.getParentId();
                        }
                        Collections.reverse(list);
                        modifyStaffVo.setOldDepartmentList(list);
                    }
                }
                modifyStaffVo.setOldDeptNo(newStaff.getDeptNo());
                modifyStaffVo.setId(newStaff.getId());
                modifyStaffVo.setOldName(newStaff.getName());
                modifyStaffVo.setOldStaffNo(newStaff.getStaffNo());
                //modifyStaffVo.setOldPhone(newStaff.getPhone());
                modifyStaffVo.setOldAccountId(newStaff.getAccountId());
                return modifyStaffVo;
            }
        return null;
    }



    public ModifyStaffVo getModifyStaffVo(String id, List<Department> departments){
        ModifyStaffVo modifyStaffVo = new ModifyStaffVo();
        Optional<Staff> opt = getRepository().findById(id);
        if (opt.isPresent()) {
            Staff oldStaff = opt.get();
            if(oldStaff != null){
                String oldDepartmentId = oldStaff.getDeptNo();
                if(StrUtil.isNotBlank(oldDepartmentId)){
                    //List<Department> departments = departmentService.findAll();
                    if(departments.size()>0){
                        Map<String,Department> map = new HashMap<>();
                        for(Department department: departments){
                            map.put(department.getId(),department);
                        }
                        LinkedList<Department> list = new LinkedList<>();
                        while (map.get(oldDepartmentId) != null){
                            Department department = map.get(oldDepartmentId);
                            department.setUpdateTime(null);
                            department.setCreateTime(null);
                            list.add(department);
                            oldDepartmentId = department.getParentId();
                        }
                        Collections.reverse(list);
                        modifyStaffVo.setOldDepartmentList(list);
                    }
                }
                modifyStaffVo.setOldDeptNo(oldStaff.getDeptNo());
                modifyStaffVo.setId(oldStaff.getId());
                modifyStaffVo.setOldName(oldStaff.getName());
                modifyStaffVo.setOldStaffNo(oldStaff.getStaffNo());
                modifyStaffVo.setAppid(oldStaff.getAppid());
                modifyStaffVo.setTenantId(UserContext.getTenantId());
                //modifyStaffVo.setOldPhone(oldStaff.getPhone());
                modifyStaffVo.setOldAccountId(oldStaff.getAccountId());
                return modifyStaffVo;
            }
        }
        return null;
    }


    @Override
    public List<BusinessManagerVo> queryStaffByRoleCode(String roleCde) {
        return staffMapper.queryStaffByRoleCode(roleCde,null);
    }

    @Override
    public void updateRecommendFlag(Integer recommendFlag, String id) {
        staffMapper.updateRecommendFlag(recommendFlag,id);
    }

    @Override
    public String getAdvBusinessManager(String staffIds,String roleCde,Integer recommendFlag) {
        if(StringUtils.isNotEmpty(staffIds)){
            String[] ids = staffIds.split(",");
            List<String> businessManagers = staffMapper.validBusinessManagerByStaffId(Arrays.asList(ids),roleCde);
            if(CollectionUtil.isNotEmpty(businessManagers)){
                return businessManagers.get(new Random().nextInt(businessManagers.size()));
            }
        }else{
            List<BusinessManagerVo> list = staffMapper.queryStaffByRoleCode(roleCde,recommendFlag);
            if(CollectionUtil.isNotEmpty(list)){
                return list.get(new Random().nextInt(list.size())).getId();
            }
        }
        return StrUtil.EMPTY;
    }

    @Override
    public BusinessManagerVo getBusinessManagerById(String id) {
        return staffMapper.searchStaffById(id);
    }
}