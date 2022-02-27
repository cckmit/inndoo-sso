package com.ytdinfo.inndoo.common.exception;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.ExceptionLog;
import com.ytdinfo.inndoo.modules.core.service.ExceptionLogService;
import com.ytdinfo.inndoo.modules.core.mqutil.ExceptionLogUtil;

import cn.hutool.core.exceptions.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Exrickx
 */
@Slf4j
@RestControllerAdvice
public class RestCtrlExceptionHandler {

	@Autowired
    public ExceptionLogUtil ExceptionLogUtil;
	@Autowired
	public ExceptionLogService ExceptionLogService;

    @XxlConf("core.exception.msg.send.to.matrix")
    private String exceptionSend2Matrix;

    @ExceptionHandler(InndooException.class)
    @ResponseStatus(value = HttpStatus.OK)
    public Result<Object> handleXCloudException(HttpServletRequest request, InndooException e) {
        String errorMsg = "Inndoo exception";
        if (e!=null){
            errorMsg = e.getMessage();
            log.error(exceptionStackTrace(e,request));
        }
        if(StrUtil.containsAnyIgnoreCase(request.getContextPath(), "core-admin")){
            return new ResultUtil<>().setErrorMsg(500, errorMsg);
        }
        return new ResultUtil<>().setErrorMsg(500, DateUtil.formatDateTime(new Date()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.OK)
    public Result<Object> handleException(HttpServletRequest request, Exception e) {
        String errorMsg = "Exception";
        if (e!=null){
            errorMsg = e.getMessage();
            log.error(exceptionStackTrace(e,request));
        }
        if(StrUtil.containsAnyIgnoreCase(request.getContextPath(), "core-admin")){
            return new ResultUtil<>().setErrorMsg(500, errorMsg);
        }
        return new ResultUtil<>().setErrorMsg(500, DateUtil.formatDateTime(new Date()));
    }

    private String exceptionStackTrace(Exception ex,HttpServletRequest request){
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        ex.printStackTrace(ps);
        String exception = ex.getMessage() + "\n" + os.toString();
        String error = ExceptionUtil.stacktraceToString(ex);
        ExceptionLog mqException = new ExceptionLog();
        String requestStr = printRequest(request);
        mqException.setMsgBody(requestStr);
        mqException.setUrl(request.getRequestURI());
        mqException.setException(exception+ "\n" +error);
        mqException.setCreateTime(new Date());
        mqException.setUpdateTime(new Date());
        //向matrix项目发送MQ信息
        if(exceptionSend2Matrix.equalsIgnoreCase("Y")){
            ExceptionLogUtil.sendMessage(mqException);
        }else{
            ExceptionLogService.save(mqException);
        }
        return exception;
    }
    
    private String printRequest(HttpServletRequest httpRequest) {
        StringBuffer sb = new StringBuffer();
        /*String url = httpRequest.getRequestURL().toString();
        String queryString = httpRequest.getQueryString();
        if(StrUtil.isNotEmpty(queryString)){
            url = url + "?" + queryString;
        }
        sb.append(url);*/
        sb.append("\n ====Headers====\n");

        Enumeration headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            sb.append(headerName + "=" + httpRequest.getHeader(headerName) + "\n");
        }

        sb.append("\n====Parameters====\n");

        Enumeration params = httpRequest.getParameterNames();
        while (params.hasMoreElements()) {
            String paramName = (String) params.nextElement();
            sb.append(paramName + "=" + httpRequest.getParameter(paramName) + "\n");
        }

        sb.append("\n====Row data====\n");
        sb.append(extractPostRequestBody(httpRequest));
        return sb.toString();
    }
    
    static String extractPostRequestBody(HttpServletRequest request) {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            Scanner s = null;
            try {
                s = new Scanner(request.getInputStream(), "UTF-8").useDelimiter("\\A");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return s.hasNext() ? s.next() : "";
        }
        return "";
    }
}
