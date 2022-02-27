package com.ytdinfo.inndoo.config.filter;

import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * xss过滤包装类
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private static final Logger logger = LoggerFactory.getLogger(XssHttpServletRequestWrapper.class);
    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request The request to wrap
     * @throws IllegalArgumentException if the request is null
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getHeader(String name) {
        String strHeader = super.getHeader(name);
        if(StrUtil.isEmpty(strHeader)){
            return strHeader;

        }
        return cleanContent(super.getHeader(name));
    }

    private String cleanContent(String value){
        String clean = Jsoup.clean(value, Whitelist.relaxed());
        if(StrUtil.isNotEmpty(clean)){
            clean = clean.replace("atob", "").replace("eval", "");
        }
        return clean;
    }

    @Override
    public String getParameter(String name) {
        String strParameter = super.getParameter(name);
        if(StrUtil.isEmpty(strParameter)){
            return strParameter;
        }
        return cleanContent(super.getParameter(name));
    }


    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if(values==null){
            return values;
        }
        int length = values.length;
        String[] escapseValues = new String[length];
        for(int i = 0;i<length;i++){
            //过滤一切可能的xss攻击字符串
            escapseValues[i] = cleanContent(values[i]).trim();
            if(!StrUtil.equals(escapseValues[i],values[i])){
                logger.debug("xss字符串过滤前："+values[i]+"\r\n"+"过滤后："+escapseValues[i]);
            }
        }
        return escapseValues;
    }
}