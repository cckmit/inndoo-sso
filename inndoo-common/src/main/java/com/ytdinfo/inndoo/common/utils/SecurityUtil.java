package com.ytdinfo.inndoo.common.utils;

import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.core.conf.Conf;
import com.ytdinfo.core.security.SsoManager;
import com.ytdinfo.core.store.SsoTokenStore;
import com.ytdinfo.core.user.SsoUser;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.vo.TokenUser;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import com.ytdinfo.inndoo.modules.base.service.mybatis.IUserRoleService;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.ytdinfo.inndoo.modules.base.entity.Permission;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.utils.DtoUtil;
import com.ytdinfo.inndoo.modules.base.vo.MenuVo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Exrickx
 */
@Component
public class SecurityUtil {


    @XxlConf("core.inndoo.token.redis")
    private Boolean tokenRedis;

    @XxlConf("core.inndoo.savelogintime")
    private Integer saveLoginTime;

    @XxlConf("core.inndoo.tokenexpiretime")
    private Integer tokenExpireTime;

    @XxlConf("core.inndoo.token.storeperms")
    private Boolean storePerms;

    @Autowired
    private UserService userService;

    @Autowired
    private IUserRoleService iUserRoleService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public String getToken(String username, Boolean saveLogin){

        Boolean saved = false;
        if(saveLogin==null||saveLogin){
            saved = true;
            if(!tokenRedis){
                tokenExpireTime = saveLoginTime * 60 * 24;
            }
        }
        // ??????token
        User u = userService.findByUsername(username);
        List<String> list = new ArrayList<>();
        // ????????????
        if(storePerms) {
            for (Permission p : u.getPermissions()) {
                if (CommonConstant.PERMISSION_OPERATION.equals(p.getType())
                        && StrUtil.isNotBlank(p.getTitle())
                        && StrUtil.isNotBlank(p.getPath())) {
                    list.add(p.getTitle());
                }
            }
            for (Role r : u.getRoles()) {
                list.add(r.getName());
            }
        }
        // ??????????????????token
        String token;
        if(tokenRedis){
            // redis
            token = UUID.randomUUID().toString().replace("-", "");
            TokenUser user = new TokenUser(u.getUsername(), list, saved);
            // ???????????? ?????????token??????
            String oldToken = redisTemplate.opsForValue().get(SecurityConstant.USER_TOKEN + u.getUsername());
            if(StrUtil.isNotBlank(oldToken)){
                redisTemplate.delete(SecurityConstant.TOKEN_PRE + oldToken);
            }
            if(saved){
                redisTemplate.opsForValue().set(SecurityConstant.USER_TOKEN + u.getUsername(), token, saveLoginTime, TimeUnit.DAYS);
                redisTemplate.opsForValue().set(SecurityConstant.TOKEN_PRE + token, new Gson().toJson(user), saveLoginTime, TimeUnit.DAYS);
            }else{
                redisTemplate.opsForValue().set(SecurityConstant.USER_TOKEN + u.getUsername(), token, tokenExpireTime, TimeUnit.MINUTES);
                redisTemplate.opsForValue().set(SecurityConstant.TOKEN_PRE + token, new Gson().toJson(user), tokenExpireTime, TimeUnit.MINUTES);
            }
            RedisUtil.addKeyToStore(SecurityConstant.USER_TOKEN, SecurityConstant.USER_TOKEN + u.getUsername());
            RedisUtil.addKeyToStore(SecurityConstant.TOKEN_PRE,SecurityConstant.TOKEN_PRE + token);
        }else{
            // jwt
            token = SecurityConstant.TOKEN_SPLIT + Jwts.builder()
                    //?????? ???????????????
                    .setSubject(u.getUsername())
                    //??????????????? ??????????????????????????????
                    .claim(SecurityConstant.AUTHORITIES, new Gson().toJson(list))
                    //????????????
                    .setExpiration(new Date(System.currentTimeMillis() + tokenExpireTime * 60 * 1000))
                    //?????????????????????
                    .signWith(SignatureAlgorithm.HS512, SecurityConstant.JWT_SIGN_KEY)
                    .compact();
        }
        return token;
    }

    /**
     * ????????????????????????
     * @return
     */
    public User getCurrUser(){
        User u=getCurrUserForSso();
        if(u!=null){
            return u;
        }
        SecurityContext context = SecurityContextHolder.getContext();
        if(context == null) {
            return null;
        }
        Authentication authentication = context.getAuthentication();
        if(authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if(principal == null || "anonymousUser".equals(principal)) {
            return null;
        }
        UserDetails user = (UserDetails) principal;
        if(user == null) {
            return null;
        }
        return userService.findByUsername(user.getUsername());
       }
    public String getToken(){
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request =servletRequestAttributes.getRequest();
        String headerSessionId = request.getHeader(Conf.HEADER);
        if (StringUtils.isEmpty(headerSessionId)) {
            headerSessionId = request.getParameter(Conf.HEADER);
        }
        return headerSessionId;
    }
    /**
     * ?????????????????????????????????
     * @return
     */
    private User  getCurrUserForSso(){
        SsoUser xxlSsoUser= getCurrSsoUser();

        if(xxlSsoUser == null){
            return null;
        }
        return userService.findByMobile(xxlSsoUser.getUsername());
    }
    public SsoUser  getCurrSsoUser(){
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request =servletRequestAttributes.getRequest();
        SsoTokenStore ssoTokenStore= SsoManager.getSsoTokenStore();
        if (ssoTokenStore==null){
            return null;
        }
        SsoUser xxlSsoUser= SsoManager.getSsoTokenStore().loginCheck(request);
        if(xxlSsoUser == null){
            xxlSsoUser= (SsoUser) request.getAttribute(Conf.SSO_USER);
            if(xxlSsoUser == null) {
                return null;
            }
        }
        return xxlSsoUser;
    }
    /**
     * ?????????????????????????????? null????????????????????????
     */
    public List<String> getDeparmentIds(){

        List<String> deparmentIds = new ArrayList<>();
        User u = getCurrUser();
        // ????????????
        List<Role> userRoleList = iUserRoleService.findByUserId(u.getId());
        // ?????????????????????????????????
        Boolean flagAll = false;
        for(Role r : userRoleList){
            if(r.getDataType()==null||r.getDataType().equals(CommonConstant.DATA_TYPE_ALL)){
                flagAll = true;
                break;
            }
        }
        if(flagAll){
            return null;
        }
        // ???????????????
        return iUserRoleService.findDepIdsByUserId(u.getId());
    }

    /**
     * ???????????????????????????????????????
     * @param username
     */
    public List<GrantedAuthority> getCurrUserPerms(String username){

        List<GrantedAuthority> authorities = new ArrayList<>();
        for(Permission p : userService.findByUsername(username).getPermissions()){
            authorities.add(new SimpleGrantedAuthority(p.getTitle()));
        }
        return authorities;
    }

    /**
     * ??????????????????
     *
     * @param user
     */
    public Boolean resetUserMenuList(User user) {

        //??????????????????
        String userKey = RedisKeyStoreType.user.getPrefixKey()+":"+user.getUsername();
        redisTemplate.delete(userKey);
        user = userService.resetUserRedis(user);

        //??????????????????
        String userMenuListKey = RedisKeyStoreType.userMenuList.getPrefixKey()+":"+user.getId();
        redisTemplate.delete(userMenuListKey);
        List<MenuVo> menuList = new ArrayList<>();
        // ?????????????????? ???????????????
        List<Permission> list = user.getPermissions();

        // ??????0?????????
        for(Permission p : list){
            if(CommonConstant.PERMISSION_NAV.equals(p.getType())&&CommonConstant.LEVEL_ZERO.equals(p.getLevel())){
                menuList.add(DtoUtil.permissionToMenuVo(p));
            }
        }
        // ??????????????????
        List<MenuVo> firstMenuList = new ArrayList<>();
        for(Permission p : list){
            if(CommonConstant.PERMISSION_PAGE.equals(p.getType())&&CommonConstant.LEVEL_ONE.equals(p.getLevel())){
                firstMenuList.add(DtoUtil.permissionToMenuVo(p));
            }
        }
        // ??????????????????
        List<MenuVo> secondMenuList = new ArrayList<>();
        for(Permission p : list){
            if(CommonConstant.PERMISSION_PAGE.equals(p.getType())&&CommonConstant.LEVEL_TWO.equals(p.getLevel())){
                secondMenuList.add(DtoUtil.permissionToMenuVo(p));
            }
        }
        // ???????????????????????????????????????
        List<MenuVo> buttonPermissions = new ArrayList<>();
        for(Permission p : list){
            if(CommonConstant.PERMISSION_OPERATION.equals(p.getType())&&CommonConstant.LEVEL_THREE.equals(p.getLevel())){
                buttonPermissions.add(DtoUtil.permissionToMenuVo(p));
            }
        }

        // ??????????????????????????????
        for(MenuVo m : secondMenuList){
            List<String> permTypes = new ArrayList<>();
            for(MenuVo me : buttonPermissions){
                if(m.getId().equals(me.getParentId())){
                    permTypes.add(me.getButtonType());
                }
            }
            m.setPermTypes(permTypes);
        }
        // ????????????????????????????????????
        for(MenuVo m : firstMenuList){
            List<MenuVo> secondMenu = new ArrayList<>();
            for(MenuVo me : secondMenuList){
                if(m.getId().equals(me.getParentId())){
                    secondMenu.add(me);
                }
            }
            m.setChildren(secondMenu);
        }
        // ??????0???????????????????????????
        for(MenuVo m : menuList){
            List<MenuVo> firstMenu = new ArrayList<>();
            for(MenuVo me : firstMenuList){
                if(m.getId().equals(me.getParentId())){
                    firstMenu.add(me);
                }
            }
            m.setChildren(firstMenu);
        }

        // ??????
        redisTemplate.opsForValue().set(userMenuListKey, new Gson().toJson(menuList), 8, TimeUnit.HOURS);
        RedisUtil.addKeyToStore(RedisKeyStoreType.userMenuList.getPrefixKey(),userMenuListKey);
        return Boolean.TRUE;
    }
}
