package com.ytdinfo.inndoo.modules.base.serviceimpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.core.security.SsoManager;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.base.dao.DepartmentDao;
import com.ytdinfo.inndoo.modules.base.dao.UserDao;
import com.ytdinfo.inndoo.modules.base.dao.mapper.PermissionMapper;
import com.ytdinfo.inndoo.modules.base.dao.mapper.UserRoleMapper;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import com.ytdinfo.inndoo.modules.base.service.mybatis.IPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户接口实现
 * @author Exrickx
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IPermissionService iPermissionService;

    @XxlConf("core.server.appcode")
    private String appCode;

    @Override
    public UserDao getRepository() {
        return userDao;
    }

    @Override
    public User findByUsername(String username) {
        RedisUtil.addKeyToStore(RedisKeyStoreType.user.getPrefixKey(), "user::" + username);
        List<User> list=userDao.findByUsername(username);
        if(list!=null&&list.size()>0){
            User user = list.get(0);
            return resetUser(user,false);
        }
        return null;
    }

    @Override
    public User resetUserRedis(User user) {
        return resetUser(user,true);
    }

    private User resetUser(User user,Boolean isReset){
        user.setAppCode(appCode);
        // 关联部门
        if(StrUtil.isNotBlank(user.getDepartmentId())){
            Department department = departmentService.get(user.getDepartmentId());
            if(null != department ) {
                user.setDepartmentTitle(department.getTitle());
            }
        }
        // 关联角色
        if (isReset){
            redisTemplate.delete(RedisKeyStoreType.userPermission.getPrefixKey()+":"+user.getId());
        }
        List<Role> roleList = userRoleMapper.findByUserId(user.getId());
        user.setRoles(roleList);
        // 关联权限菜单
//        List<Permission> permissionList = iPermissionService.findByUserId(user.getId());
//        user.setPermissions(permissionList);
//        RedisUtil.addKeyToStore(RedisKeyStoreType.user.getPrefixKey(), "user::" + user.getUsername());
        return user;
    }

    @Override
    public User findByMobile(String mobile) {

        String token=securityUtil.getToken();
        String key ="";
        if(token!=null){
            key = RedisKeyStoreType.ssoUser.getPrefixKey().concat(token);
            String v = redisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(v)){
                return new Gson().fromJson(v,User.class);
            }
        }
        User user=null;
        List<User> list = userDao.findByMobile(mobile);
        if(list!=null&&list.size()==1){
            user = list.get(0);
            user= resetUser(user,false);
            if(StrUtil.isNotBlank(key)) {
                redisTemplate.opsForValue().set(key, new Gson().toJson(user), SsoManager.getCacheTimeout(), TimeUnit.SECONDS);
            }
        }
        return user;
    }

    @Override
    public User findByEmail(String email) {

        List<User> list = userDao.findByEmail(email);
        if(list!=null&&list.size()>0) {
            User user = list.get(0);
            return user;
        }
        return null;
    }

    @Override
    public Page<User> findByCondition(User user, SearchVo searchVo, Pageable pageable) {

        return userDao.findAll(new Specification<User>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<User> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                Path<String> usernameField = root.get("username");
                Path<String> mobileField = root.get("mobile");
                Path<String> emailField = root.get("email");
                Path<String> departmentIdField = root.get("departmentId");
                Path<String> sexField = root.get("sex");
                Path<Integer> typeField = root.get("type");
                Path<Integer> statusField = root.get("status");
                Path<Date> createTimeField = root.get("createTime");
                Path<Boolean> isDeletedField = root.get("isDeleted");

                List<Predicate> list = new ArrayList<Predicate>();

                //模糊搜素
                if(StrUtil.isNotBlank(user.getUsername())){
                    list.add(cb.like(usernameField,'%'+user.getUsername()+'%'));
                }
                if(StrUtil.isNotBlank(user.getMobile())){
                    list.add(cb.like(mobileField,'%'+user.getMobile()+'%'));
                }
                if(StrUtil.isNotBlank(user.getEmail())){
                    list.add(cb.like(emailField,'%'+user.getEmail()+'%'));
                }

                //部门
                if(StrUtil.isNotBlank(user.getDepartmentId())){
                    list.add(cb.equal(departmentIdField, user.getDepartmentId()));
                }

                //性别
                if(StrUtil.isNotBlank(user.getSex())){
                    list.add(cb.equal(sexField, user.getSex()));
                }
                //类型
                if(user.getType()!=null){
                    list.add(cb.equal(typeField, user.getType()));
                }
                //状态
                if(user.getStatus()!=null){
                    list.add(cb.equal(statusField, user.getStatus()));
                }
                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                list.add(cb.equal(isDeletedField,false));
                //数据权限
                List<String> depIds = securityUtil.getDeparmentIds();
                if(depIds!=null&&depIds.size()>0){
                    list.add(departmentIdField.in(depIds));
                }

                Predicate[] arr = new Predicate[list.size()];
                if(list.size() > 0){
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    @Override
    public List<User> findByDepartmentId(String departmentId) {

        return userDao.findByDepartmentId(departmentId);
    }

    @Override
    public List<User> findByUsernameLikeAndStatus(String username, Integer status) {

        return userDao.findByUsernameLikeAndStatus(username, status);
    }
}
