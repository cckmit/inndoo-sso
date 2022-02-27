package com.ytdinfo.inndoo.common.datasource;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.SecurityConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.exception.InndooException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author timmy
 * @date 2019/9/6
 */
@Slf4j
@Component
public class DynamicDataSourceFilter implements Filter, Ordered {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String tenantId = req.getHeader(SecurityConstant.TENANT_ID);
        if (StrUtil.isBlank(tenantId)) {
            tenantId = req.getParameter(SecurityConstant.TENANT_ID);
        }
        if(StrUtil.isBlank(tenantId)){
            tenantId = req.getHeader(SecurityConstant.TENANT_ID.toLowerCase());
            if (StrUtil.isBlank(tenantId)) {
                tenantId = req.getParameter(SecurityConstant.TENANT_ID.toLowerCase());
            }
        }
        String wxappid = req.getHeader(SecurityConstant.WXAPPID);
        if (StrUtil.isBlank(wxappid)) {
            wxappid = req.getParameter(SecurityConstant.WXAPPID);
        }
//        if(StrUtil.isEmpty(tenantId)){
//            throw new InndooException("The tenantId is required, please set tenantId value in the header or paramater.");
//        }
        if(StrUtil.isNotEmpty(tenantId)){
            if(tenantId.length() != 18 || !NumberUtil.isNumber(tenantId)){
                return;
            }
        }
        if(StrUtil.isNotEmpty(wxappid)){
            if(wxappid.length() != 18 || !ReUtil.isMatch("^wx(?=.*\\d)(?=.*[a-z])[\\da-z]{16}$",wxappid)){
                return;
            }
        }
        DynamicDataSourceContextHolder.setDataSourceType(tenantId);
        UserContext.setTenantId(tenantId);
        UserContext.setWxAppId(wxappid);
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {
        UserContext.remove();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}