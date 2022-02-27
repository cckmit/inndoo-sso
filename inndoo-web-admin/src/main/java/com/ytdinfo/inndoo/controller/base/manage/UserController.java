package com.ytdinfo.inndoo.controller.base.manage;

import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.core.entity.CoreApp;
import com.ytdinfo.core.security.SsoManager;
import com.ytdinfo.core.user.SsoUser;
import com.ytdinfo.inndoo.base.utils.AddMessage;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.activiti.service.ActNodeService;
import com.ytdinfo.inndoo.modules.base.entity.*;
import com.ytdinfo.inndoo.modules.base.service.*;
import com.ytdinfo.inndoo.modules.base.service.mybatis.IUserRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * @author Exrickx
 */
@Slf4j
@RestController
@Api(description = "用户接口")
@RequestMapping("/base/user")
@CacheConfig(cacheNames = "user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentHeaderService departmentHeaderService;

    @Autowired
    private IUserRoleService iUserRoleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private AddMessage addMessage;

    @Autowired
    private ActNodeService actNodeService;

    @Autowired
    private QQService qqService;

    @Autowired
    private WeiboService weiboService;

    @Autowired
    private GithubService githubService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SecurityUtil securityUtil;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MatrixApiUtil apiUtil;

    @XxlConf("core.server.appcode")
    private String appCode;

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ApiOperation(value = "获取当前登录用户接口")
    public Result<User> getUserInfo(HttpServletRequest request){

        User u = securityUtil.getCurrUser();
        // 清除持久上下文环境 避免后面语句导致持久化
        entityManager.clear();
        u.setPassword(null);
        return new ResultUtil<User>().setData(u);
    }

    @RequestMapping(value = "/unlock", method = RequestMethod.POST)
    @ApiOperation(value = "解锁验证密码")
    public Result<Object> unLock(@RequestParam String password){

        User u = securityUtil.getCurrUser();
        if(!new BCryptPasswordEncoder().matches(password, u.getPassword())){
            return new ResultUtil<Object>().setErrorMsg("密码不正确");
        }
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/admin/update", method = RequestMethod.POST)
    @ApiOperation(value = "管理员修改资料",notes = "需要通过id获取原用户信息 需要username更新缓存")
    @CacheEvict(key = "#u.username")
    public Result<Object> update(@ModelAttribute User u,
                               @RequestParam(required = false) String[] roles){

        User old = userService.get(u.getId());
        if(null == u.getType()){
            u.setType(0);
        }
        if (null == u.getStatus()) {
            u.setStatus(0);
        }
        //若修改了用户名
        if(!old.getUsername().equals(u.getUsername())){
            //判断新用户名是否存在
            if(userService.findByUsername(u.getUsername())!=null){
                return new ResultUtil<Object>().setErrorMsg("该用户名已存在");
            }
            //若修改用户名删除原用户名缓存
            redisTemplate.delete("user::"+old.getUsername());
            //删除缓存
            redisTemplate.delete("user::"+u.getUsername());
        }

        // 若修改了手机和邮箱判断是否唯一
        if(!old.getMobile().equals(u.getMobile())&&userService.findByMobile(u.getMobile())!=null){
            return new ResultUtil<Object>().setErrorMsg("该手机号已绑定其他账户");
        }
        if(!old.getEmail().equals(u.getEmail())&&userService.findByMobile(u.getEmail())!=null){
            return new ResultUtil<Object>().setErrorMsg("该邮箱已绑定其他账户");
        }

        u.setPassword(old.getPassword());
        User user = userService.update(u);
        if(user==null){
            return new ResultUtil<Object>().setErrorMsg("修改失败");
        }
        //删除该用户角色
        userRoleService.deleteByUserId(u.getId());
        if(roles!=null&&roles.length>0){
            //新角色
            for(String roleId : roles){
                UserRole ur = new UserRole();
                ur.setRoleId(roleId);
                ur.setUserId(u.getId());
                userRoleService.save(ur);
            }
        }
        //手动删除缓存
        redisTemplate.delete("userRole::"+u.getId());
        redisTemplate.delete("userRole::depIds:"+u.getId());
        Set<String> userIdList = RedisUtil.membersFromKeyStore(RedisKeyStoreType.roleUserList.getPrefixKey());
        userIdList.add(user.getId());
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.roleUserList.getPrefixKey());
        RedisUtil.addAllKeyToStore(RedisKeyStoreType.roleUserList.getPrefixKey(),userIdList);
        return new ResultUtil<Object>().setSuccessMsg("修改成功");
    }

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取用户列表")
    public Result<Page<User>> listByCondition(@ModelAttribute User user,
                                             @ModelAttribute SearchVo searchVo,
                                             @ModelAttribute PageVo pageVo){

        Page<User> page = userService.findByCondition(user, searchVo, PageUtil.initPage(pageVo));
        for(User u: page.getContent()){
            // 关联部门
            if(StrUtil.isNotBlank(u.getDepartmentId())){
                Department department = departmentService.get(u.getDepartmentId());
                if(department!=null){
                    u.setDepartmentTitle(department.getTitle());
                }
            }
            // 关联角色
            List<Role> list = iUserRoleService.findByUserId(u.getId());
            u.setRoles(list);
            // 清除持久上下文环境 避免后面语句导致持久化
            entityManager.clear();
            u.setPassword(null);
        }
        return new ResultUtil<Page<User>>().setData(page);
    }

    @RequestMapping(value = "/listByDepartmentId/{departmentId}", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取用户列表")
    public Result<List<User>> listByDepartmentId(@PathVariable String departmentId){

        List<User> list = userService.findByDepartmentId(departmentId);
        entityManager.clear();
        list.forEach(u -> {
            u.setPassword(null);
        });
        return new ResultUtil<List<User>>().setData(list);
    }

    @RequestMapping(value = "/searchByName/{username}", method = RequestMethod.GET)
    @ApiOperation(value = "通过用户名搜索用户")
    public Result<List<User>> searchByName(@PathVariable String username) throws UnsupportedEncodingException {

        List<User> list = userService.findByUsernameLikeAndStatus("%"+ URLDecoder.decode(username, "utf-8")+"%", CommonConstant.STATUS_NORMAL);
        return new ResultUtil<List<User>>().setData(list);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiOperation(value = "获取全部用户数据")
    public Result<List<User>> list(){

        List<User> list = userService.findAll();
        for(User u: list){
            // 关联部门
            if(StrUtil.isNotBlank(u.getDepartmentId())){
                Department department = departmentService.get(u.getDepartmentId());
                if ( null != department ){
                    u.setDepartmentTitle(department.getTitle());
                }
            }
            // 清除持久上下文环境 避免后面语句导致持久化
            entityManager.clear();
            u.setPassword(null);
        }
        return new ResultUtil<List<User>>().setData(list);
    }

    @RequestMapping(value = "/admin/create", method = RequestMethod.POST)
    @ApiOperation(value = "添加用户")
    public Result<Object> regist(@ModelAttribute User u,
                                 @RequestParam(required = false) String[] roles){

        if(StrUtil.isBlank(u.getUsername()) || StrUtil.isBlank(u.getPassword())){
            return new ResultUtil<Object>().setErrorMsg("缺少必需表单字段");
        }
        if(null == u.getType()){
            u.setType(0);
        }
        if (null == u.getStatus()) {
            u.setStatus(0);
        }
        if(userService.findByUsername(u.getUsername())!=null){
            return new ResultUtil<Object>().setErrorMsg("该用户名已被注册");
        }
        //删除缓存
        redisTemplate.delete("user::"+u.getUsername());

        String encryptPass = new BCryptPasswordEncoder().encode(u.getPassword());
        u.setPassword(encryptPass);
        User user=userService.save(u);
        if(user==null){
            return new ResultUtil<Object>().setErrorMsg("添加失败");
        }
        if(roles!=null&&roles.length>0){
            //添加角色
            for(String roleId : roles){
                UserRole ur = new UserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(roleId);
                userRoleService.save(ur);
            }
        }
        // 发送创建账号消息
        addMessage.addSendMessage(user.getId());

        return new ResultUtil<Object>().setSuccessMsg("添加成功");
    }

    @RequestMapping(value = "/admin/disable/{userId}", method = RequestMethod.POST)
    @ApiOperation(value = "后台禁用用户")
    public Result<Object> disable(@ApiParam("用户唯一id标识") @PathVariable String userId){

        User user = userService.get(userId);
        if(user==null){
            return new ResultUtil<Object>().setErrorMsg("通过userId获取用户失败");
        }
        user.setStatus(CommonConstant.USER_STATUS_LOCK);
        userService.update(user);
        //手动更新缓存
        redisTemplate.delete("user::"+user.getUsername());
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/admin/enable/{userId}", method = RequestMethod.POST)
    @ApiOperation(value = "后台启用用户")
    public Result<Object> enable(@ApiParam("用户唯一id标识") @PathVariable String userId){

        User user = userService.get(userId);
        if(user==null){
            return new ResultUtil<Object>().setErrorMsg("通过userId获取用户失败");
        }
        user.setStatus(CommonConstant.USER_STATUS_NORMAL);
        userService.update(user);
        //手动更新缓存
        redisTemplate.delete("user::"+user.getUsername());
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "批量通过ids删除")
    public Result<Object> batchDelete(@PathVariable String[] ids){

        for(String id:ids){
            User u = userService.get(id);
            //删除相关缓存
            redisTemplate.delete("user::" + u.getUsername());
            redisTemplate.delete("userRole::" + u.getId());
            redisTemplate.delete("userRole::depIds:" + u.getId());
            redisTemplate.delete("permission::userMenuList:" + u.getId());
            Set<String> keys = RedisUtil.keys("department::*");
            redisTemplate.unlink(keys);

            // 删除关联社交账号
            qqService.deleteByUsername(u.getUsername());
            weiboService.deleteByUsername(u.getUsername());
            githubService.deleteByUsername(u.getUsername());

            userService.delete(id);

            //删除关联角色
            userRoleService.deleteByUserId(id);
            // 删除关联部门负责人
            departmentHeaderService.deleteByUserId(id);
            // 删除流程关联节点
            actNodeService.deleteByRelateId(id);
        }
        return new ResultUtil<Object>().setSuccessMsg("批量通过id删除数据成功");
    }

    @RequestMapping(value = "/importData", method = RequestMethod.POST)
    @ApiOperation(value = "导入用户数据")
    public Result<Object> importData(@RequestBody List<User> users){

        List<Integer> errors = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        int count = 0;
        for(User u: users){
            count++;
            // 验证用户名密码不为空
            if(StrUtil.isBlank(u.getUsername())||StrUtil.isBlank(u.getPassword())){
                errors.add(count);
                reasons.add("用户名或密码为空");
                continue;
            }
            // 验证用户名唯一
            if(userService.findByUsername(u.getUsername())!=null){
                errors.add(count);
                reasons.add("用户名已存在");
                continue;
            }
            //删除缓存
            redisTemplate.delete("user::"+u.getUsername());
            // 加密密码
            u.setPassword(new BCryptPasswordEncoder().encode(u.getPassword()));
            // 验证部门id正确性
            if(StrUtil.isNotBlank(u.getDepartmentId())){
                try {
                    Department d = departmentService.get(u.getDepartmentId());
                    log.info(d.toString());
                }catch (Exception e){
                    errors.add(count);
                    reasons.add("部门id不存在");
                    continue;
                }
            }
            if(u.getStatus()==null){
                u.setStatus(CommonConstant.USER_STATUS_NORMAL);
            }
            // 分配默认角色
            if(u.getDefaultRole()!=null&&u.getDefaultRole()==1){
                List<Role> roleList = roleService.findByDefaultRole(true);
                if(roleList!=null&&roleList.size()>0){
                    for(Role role : roleList){
                        UserRole ur = new UserRole();
                        ur.setUserId(u.getId());
                        ur.setRoleId(role.getId());
                        iUserRoleService.save(ur);
                    }
                }
            }
            // 保存数据
            userService.save(u);
        }
        int successCount = users.size() - errors.size();
        String successMessage = "全部导入成功，共计 " + successCount + " 条数据";
        String failMessage = "导入成功 " + successCount + " 条，失败 " + errors.size() + " 条数据。<br>" +
                "第 " + errors.toString() + " 行数据导入出错，错误原因分别为：<br>" + reasons.toString();
        String message = "";
        if(errors.size()==0){
            message = successMessage;
        }else{
            message = failMessage;
        }
        return new ResultUtil<Object>().setSuccessMsg(message);
    }

    @RequestMapping(value = "/app/list", method = RequestMethod.GET)
    @ApiOperation(value = "获取appList的值")
    public Result<List<CoreApp>> appListInfo(HttpServletRequest request) {
//        XxlSsoUser xxlSsoUser = SsoTokenLoginHelper.loginCheck(request);
        SsoUser xxlSsoUser = SsoManager.getSsoTokenStore().loginCheck(request);
        List<CoreApp> caList = xxlSsoUser.getAppList();
        Iterator<CoreApp> it = caList.iterator();
        while (it.hasNext()) {
            CoreApp ca = it.next();
            if(ca.getCode().equals(appCode)){
                it.remove();
            }
        }
        return new ResultUtil<List<CoreApp>>().setData(caList);
    }

    /**
     * Logout
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = request.getHeader(SecurityConstant.HEADER);
        if(StrUtil.isEmpty(sessionId)){
            return null;
        }
        SsoManager.getSsoTokenStore().logout(sessionId);
        User currUser = securityUtil.getCurrUser();
        if(currUser != null){
            SsoManager.getSsoTokenStore().removeUserPermissions(currUser.getMobile());
        }
        String userJson = redisTemplate.opsForValue().get(SecurityConstant.TOKEN_PRE + sessionId);
        if(StrUtil.isNotEmpty(userJson)){
            TokenUser user = new Gson().fromJson(userJson,TokenUser.class);
            redisTemplate.delete(SecurityConstant.USER_TOKEN + user.getUsername());
            redisTemplate.delete(SecurityConstant.TOKEN_PRE + sessionId);
            redisTemplate.delete("user::" + user.getUsername());

            RedisUtil.removeKeyFromStore(RedisKeyStoreType.USER_TOKEN.getPrefixKey(), SecurityConstant.USER_TOKEN + user.getUsername());
            RedisUtil.removeKeyFromStore(RedisKeyStoreType.TOKEN_PRE.getPrefixKey(), SecurityConstant.TOKEN_PRE + sessionId);
            RedisUtil.removeKeyFromStore(RedisKeyStoreType.user.getPrefixKey(), "user::" + user.getUsername());
        }

        // logout
//        SsoTokenLoginHelper.logout(sessionId);
        return new ResultUtil<String>().setSuccessMsg("退出成功!");
    }

    @RequestMapping(value = "/tenant/list", method = RequestMethod.GET)
    @ApiOperation(value = "获取当前用户授权租户清单")
    public Result<List<TenantVo>> userTenant(){
        List<TenantVo> list = new ArrayList<>();
//        List<Tenant> tenants = apiUtil.getTenantByCurrentUser();
        SsoUser ssoUser= securityUtil.getCurrSsoUser();
        if(ssoUser!=null) {
            List<Tenant> tenants = apiUtil.getTenantByMobile(ssoUser.getUsername());

            for (Tenant tenant : tenants) {
                TenantVo vo = new TenantVo();
                vo.setId(tenant.getId());
                vo.setName(tenant.getName());
                list.add(vo);
            }
        }else {
            new ResultUtil<List<TenantVo>>().setErrorMsg("未获取登录信息");
        }
        return new ResultUtil<List<TenantVo>>().setData(list);
    }

    @RequestMapping(value = "/authorizer/list", method = RequestMethod.GET)
    @ApiOperation(value = "获取当前用户授权租户清单")
    public Result<List<WxAuthorizerVo>> userTenant(@RequestParam String tenantId){
        List<WxAuthorizer> authorizers = apiUtil.getWxAuthorizerListByTenantAndMobile(tenantId);
        List<WxAuthorizerVo> list = new ArrayList<>();
        for (WxAuthorizer authorizer:authorizers){
            WxAuthorizerVo vo = new WxAuthorizerVo();
            vo.setId(authorizer.getId());
            vo.setName(authorizer.getNickName());
            vo.setAppid(authorizer.getAppid());
            list.add(vo);
        }
        return new ResultUtil<List<WxAuthorizerVo>>().setData(list);
    }
}
