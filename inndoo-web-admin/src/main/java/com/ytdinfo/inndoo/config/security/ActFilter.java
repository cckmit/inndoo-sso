package com.ytdinfo.inndoo.config.security;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.core.conf.Conf;
import com.ytdinfo.core.filter.ActSsoFilter;
import com.ytdinfo.core.path.impl.AntPathMatcher;
import com.ytdinfo.core.security.SsoManager;
import com.ytdinfo.core.user.SsoUser;
import com.ytdinfo.core.util.SsoUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.vo.Tenant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 活动平台加载用户
 * @author qinbaolei 2022/1/25 14:44
 * @param
 * @return
 **/
@Slf4j
public class ActFilter extends HttpServlet implements Filter {
    private static Logger logger = LoggerFactory.getLogger(ActSsoFilter.class);
    private MatrixApiUtil matrixApiUtil;
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();


    private static String logoutPath;
    private static String actLogoutPath;
    private static String excludedPaths;

    public MatrixApiUtil getMatrixApiUtil() {
        return matrixApiUtil;
    }

    public void setMatrixApiUtil(MatrixApiUtil matrixApiUtil) {
        this.matrixApiUtil = matrixApiUtil;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        logoutPath = filterConfig.getInitParameter(Conf.SSO_LOGOUT_PATH);
        excludedPaths = filterConfig.getInitParameter(Conf.SSO_EXCLUDED_PATHS);
        String serverUrl = filterConfig.getInitParameter(Conf.SSO_SERVER);
        String serverActUrl = filterConfig.getInitParameter(Conf.ACT_SSO_SERVER);
        actLogoutPath=filterConfig.getInitParameter(Conf.ACT_SSO_LOGOUT_PATH);

        logger.info("ActFilter init");
    }

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (req.getMethod().equals(RequestMethod.OPTIONS.name())) {
            chain.doFilter(request, response);
            return;
        }

        String origin = req.getHeader("origin");
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Access-Control-Allow-Origin",origin);
        res.setHeader("Access-Control-Allow-Methods",origin);
        res.setHeader("Access-Control-Allow-Headers",origin);

        //获取xxlsso登录用户信息
        SsoUser xxlUser = (SsoUser)request.getAttribute(Conf.SSO_USER);

        // login filter
        if (xxlUser == null) {
            xxlUser = SsoManager.getSsoTokenStore().loginCheck(req);
        }
       if(xxlUser!=null) {
           ActSsoFilter.addExcludedPaths("/base/user/tenant/list");
           if (!NumberUtil.isNumber(xxlUser.getUsername())) {
               res.getWriter().println("{\"success\":false,\"code\":401, \"message\":\"您还未登录\", \"loginurl\":\"" + actLogoutPath + "\"}");
               return;
           }
           Boolean msg1 = loginBySsoUser(xxlUser);
           if(!msg1){
               res.getWriter().println("{\"success\":false,\"code\":409, \"message\":\"您还未有租户权限，请联系管理员授权租户\"}");
               return;
           }
       }
        // already login, allow
        chain.doFilter(request, response);
        return;
    }
    private Boolean loginBySsoUser(SsoUser xxlSsoUser) {

        if (StrUtil.isEmpty(UserContext.getTenantId())) {
            List<Tenant> tenants = matrixApiUtil.getTenantByMoblie(xxlSsoUser.getUsername());
            if (tenants != null && tenants.size() > 0) {
//                UserContext.setTenantId(tenants.get(0).getId());
//                DynamicDataSourceContextHolder.setDataSourceType(UserContext.getTenantId());
                return true;
            }else{
                return false;
            }
        }else{
            return true;
        }
    }
}
