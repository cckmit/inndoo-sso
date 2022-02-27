package com.ytdinfo.inndoo.common.utils.excel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import org.apache.poi.util.StringUtil;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 文件导出相关内容封装
 *
 * @author xuewenlong
 * @Date 1/28/22
 */
@Service
public class WaterExcelUtil {

    /**
     * 获取文件项目路径
     *
     * @return
     */
    public  String getRootPath(){
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String path = jarFile.getParentFile().getPath();
        String rootPath = path + File.separator + "static/ytdexports";
        File dir = new File(rootPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return rootPath;
    }


    /**
     * 非对象输出
     */
    public void writeForList(HttpServletResponse response,String excelFileName,List<List<String>> rows) throws IOException {
        try {
            List<String> head = rows.remove(0);
            List<List<String>> headList = CollUtil.newArrayList();
            for (String s : head) {
                List<String> headLength = CollUtil.newArrayList();
                headLength.add(s);
                headList.add(headLength);
            }

            response.setContentType("multipart/form-data");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename="+ URLUtil.encode(excelFileName, StringUtil.UTF8) );
            String fullFileName = getRootPath() + File.separator + excelFileName;
            EasyExcel.write(fullFileName)
                    .inMemory(true) // 注意，此项配置不能少
                    .registerWriteHandler(new WaterMarkHandler(waterRemark()))
                    .head(headList)
                    .sheet("sheet1")
                    .doWrite(rows);
            File file = new File(fullFileName);
            ServletUtil.write(response, file);
            file.delete();

        }catch (Exception e){
            // 重置response
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("下载文件失败")));
        }

    }

    /**
     * excel导出水印内容
     */
    public static String waterRemark(){
        return UserContext.getUser().getUsername()+"\t"+ DateUtil.format(new Date(),"yyyy-MM-dd");
    }

}
