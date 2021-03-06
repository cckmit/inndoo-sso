package com.ytdinfo.inndoo.config.security.jwt;

import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.utils.ResponseUtil;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.vo.Tenant;
import com.ytdinfo.inndoo.common.vo.TokenUser;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.core.user.XxlSsoUser;
import com.ytdinfo.inndoo.modules.base.entity.Permission;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Exrickx
 */
@Slf4j
public class JWTAuthenticationFilter extends BasicAuthenticationFilter {

    private Boolean tokenRedis;

    private Integer tokenExpireTime;

    private Boolean storePerms;

    private StringRedisTemplate redisTemplate;

    private SecurityUtil securityUtil;

    private UserService userService;

    private MatrixApiUtil apiUtil;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, Boolean tokenRedis, Integer tokenExpireTime,
                                   Boolean storePerms, StringRedisTemplate redisTemplate, SecurityUtil securityUtil, UserService userService, MatrixApiUtil apiUtil) {
        super(authenticationManager);
        this.tokenRedis = tokenRedis;
        this.tokenExpireTime = tokenExpireTime;
        this.storePerms = storePerms;
        this.redisTemplate = redisTemplate;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.apiUtil = apiUtil;
    }

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        String header = request.getHeader(SecurityConstant.HEADER);
        if (StrUtil.isBlank(header)) {
            header = request.getParameter(SecurityConstant.HEADER);
        }
        Boolean notValid = StrUtil.isBlank(header) || (!tokenRedis && !header.startsWith(SecurityConstant.TOKEN_SPLIT));
        if (notValid) {
            chain.doFilter(request, response);
            return;
        }
        //??????xxlsso??????????????????
        XxlSsoUser xxlSsoUser = (XxlSsoUser) request.getAttribute("xxl_sso_user");
        if (xxlSsoUser == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            loginBySsoUser(xxlSsoUser);
            UsernamePasswordAuthenticationToken authentication = getAuthentication(header, response);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            e.toString();
            System.out.println(e.toString());
        }

        chain.doFilter(request, response);
    }

    private void loginBySsoUser(XxlSsoUser xxlSsoUser) {

        if (xxlSsoUser == null) {
            return;
        }
        // ??????????????????token
        String token = xxlSsoUser.getVersion();
        String username = xxlSsoUser.getUsername();

        if (StrUtil.isEmpty(UserContext.getTenantId())) {
            List<Tenant> tenants = apiUtil.getTenantByUserId(xxlSsoUser.getUserid());
            if (tenants.size() > 0) {
                UserContext.setTenantId(tenants.get(0).getId());
            }
        }

        long expiredTime = xxlSsoUser.getExpireFreshTime() + xxlSsoUser.getExpireMinite() * 60 * 1000;

        List<String> list = new ArrayList<>();
        if (tokenRedis) {
            String oldToken = redisTemplate.opsForValue().get(SecurityConstant.USER_TOKEN + username);
            String v = redisTemplate.opsForValue().get(SecurityConstant.TOKEN_PRE + oldToken);
            if (oldToken != null && oldToken.equals(token) && StrUtil.isNotEmpty(v)) {
                return;
            }

            redisTemplate.delete("user::" + username);
            com.ytdinfo.inndoo.modules.base.entity.User sysUser = userService.findByUsername(username);
            for (Permission g : sysUser.getPermissions()) {
                list.add(g.getTitle());
            }
            if (sysUser != null) {
                redisTemplate.delete("permission::userMenuList:" + sysUser.getId());
                redisTemplate.delete("userPermission::" + sysUser.getId());
            }
            // redis
            TokenUser user = new TokenUser(username, list, true);
            // ???????????????
            if (!storePerms) {
                user.setPermissions(null);
            }
            // ???????????? ?????????token??????
            if (StrUtil.isNotBlank(oldToken)) {
                redisTemplate.delete(SecurityConstant.TOKEN_PRE + oldToken);
            }
            long saveLoginTime = expiredTime - System.currentTimeMillis();
            redisTemplate.opsForValue().set(SecurityConstant.USER_TOKEN + username, token, saveLoginTime, TimeUnit.MILLISECONDS);
            redisTemplate.opsForValue().set(SecurityConstant.TOKEN_PRE + token, new Gson().toJson(user), saveLoginTime, TimeUnit.MILLISECONDS);
            RedisUtil.addKeyToStore(SecurityConstant.USER_TOKEN, SecurityConstant.USER_TOKEN + username);
            RedisUtil.addKeyToStore(SecurityConstant.TOKEN_PRE,SecurityConstant.TOKEN_PRE + token);

        } else {
            // ???????????????
            if (!storePerms) {
                list = null;
            }
            // jwt
            token = SecurityConstant.TOKEN_SPLIT + Jwts.builder()
                    //?????? ???????????????
                    .setSubject(username)
                            //??????????????? ??????????????????????????????
                    .claim(SecurityConstant.AUTHORITIES, new Gson().toJson(list))
                            //????????????
                    .setExpiration(new Date(expiredTime))
                            //?????????????????????
                    .signWith(SignatureAlgorithm.HS512, SecurityConstant.JWT_SIGN_KEY)
                    .compact();
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(String header, HttpServletResponse response) {

        // ?????????
        String username = null;
        // ??????
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (tokenRedis) {
            // redis
            String v = redisTemplate.opsForValue().get(SecurityConstant.TOKEN_PRE + header);
            if (StrUtil.isBlank(v)) {
                ResponseUtil.out(response, ResponseUtil.resultMap(false, 401, "?????????????????????????????????"));
                return null;
            }
            TokenUser user = new Gson().fromJson(v, TokenUser.class);
            username = user.getUsername();
            if (storePerms) {
                // ???????????????
                for (String ga : user.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(ga));
                }
            } else {
                // ????????? ??????????????????
                authorities = securityUtil.getCurrUserPerms(username);
            }
        } else {
            // JWT
            try {
                // ??????token
                Claims claims = Jwts.parser()
                        .setSigningKey(SecurityConstant.JWT_SIGN_KEY)
                        .parseClaimsJws(header.replace(SecurityConstant.TOKEN_SPLIT, ""))
                        .getBody();

                //???????????????
                username = claims.getSubject();
                //????????????
                if (storePerms) {
                    // ???????????????
                    String authority = claims.get(SecurityConstant.AUTHORITIES).toString();
                    if (StrUtil.isNotBlank(authority)) {
                        List<String> list = new Gson().fromJson(authority, new TypeToken<List<String>>() {
                        }.getType());
                        for (String ga : list) {
                            authorities.add(new SimpleGrantedAuthority(ga));
                        }
                    }
                } else {
                    // ????????? ??????????????????
                    authorities = securityUtil.getCurrUserPerms(username);
                }
            } catch (ExpiredJwtException e) {
                ResponseUtil.out(response, ResponseUtil.resultMap(false, 401, "?????????????????????????????????"));
            } catch (Exception e) {
                log.error(e.toString());
                ResponseUtil.out(response, ResponseUtil.resultMap(false, 500, "??????token??????"));
            }
        }

        if (StrUtil.isNotBlank(username)) {
            // ???????????? ??????password?????????null
            User principal = new User(username, "", authorities);
            return new UsernamePasswordAuthenticationToken(principal, null, authorities);
        }
        return null;
    }
}

