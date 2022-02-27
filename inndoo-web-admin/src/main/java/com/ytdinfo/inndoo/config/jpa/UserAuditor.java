package com.ytdinfo.inndoo.config.jpa;

import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.modules.base.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * 审计记录创建或修改用户
 * @author Exrickx
 */
@Configuration
@Slf4j
public class UserAuditor implements AuditorAware<String> {
    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public Optional<String> getCurrentAuditor() {
        User currUser;
        try {
            currUser = securityUtil.getCurrUser();
            return Optional.ofNullable(currUser.getUsername());
        }catch (Exception e){
            return Optional.empty();
        }
    }
}
