package com.ytdinfo.inndoo.config.security.jwt;

import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.utils.ResponseUtil;
import com.ytdinfo.inndoo.common.exception.LoginFailLimitException;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Exrickx
 */
@Slf4j
@Component
public class AuthenticationFailHandler extends SimpleUrlAuthenticationFailureHandler {

    //@Value("${inndoo.loginTimeLimit}")
    @XxlConf("core.inndoo.logintimelimit")
    private Integer loginTimeLimit;

    //@Value("${inndoo.loginaftertime}")
    @XxlConf("core.inndoo.loginaftertime")
    private Integer loginAfterTime;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {

        if (e instanceof UsernameNotFoundException || e instanceof BadCredentialsException) {
            String username = request.getParameter("username");
            recordLoginTime(username);
            String key = "loginTimeLimit:"+username;
            String value = redisTemplate.opsForValue().get(key);
            if(StrUtil.isBlank(value)){
                value = "0";
            }
            //???????????????????????????
            int loginFailTime = Integer.parseInt(value);
            int restLoginTime = loginTimeLimit - loginFailTime;
            log.info("??????"+username+"?????????????????????"+restLoginTime+"?????????");
            if(restLoginTime<=3&&restLoginTime>0){
                ResponseUtil.out(response, ResponseUtil.resultMap(false, 500, "?????????????????????????????????" + restLoginTime + "???????????????"));
            } else if(restLoginTime<=0) {
                ResponseUtil.out(response, ResponseUtil.resultMap(false,500,"????????????????????????????????????"+loginAfterTime+"???????????????"));
            } else {
                ResponseUtil.out(response, ResponseUtil.resultMap(false,500,"????????????????????????"));
            }
        } else if (e instanceof DisabledException) {
            ResponseUtil.out(response, ResponseUtil.resultMap(false,500,"????????????????????????????????????"));
        } else if (e instanceof LoginFailLimitException){
            ResponseUtil.out(response, ResponseUtil.resultMap(false,500,((LoginFailLimitException) e).getMsg()));
        } else {
            ResponseUtil.out(response, ResponseUtil.resultMap(false,500,"?????????????????????????????????"));
        }
    }

    /**
     * ??????????????????????????????
     */
    public boolean recordLoginTime(String username){

        String key = "loginTimeLimit:"+username;
        String flagKey = "loginFailFlag:"+username;
        String value = redisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(value)){
            value = "0";
        }
        //???????????????????????????
        int loginFailTime = Integer.parseInt(value) + 1;
        redisTemplate.opsForValue().set(key, String.valueOf(loginFailTime), loginAfterTime, TimeUnit.MINUTES);
        if(loginFailTime>=loginTimeLimit){

            redisTemplate.opsForValue().set(flagKey, "fail", loginAfterTime, TimeUnit.MINUTES);
            return false;
        }
        return true;
    }
}
