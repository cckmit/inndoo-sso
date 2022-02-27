package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.ExcelConstant;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.ExcelRow;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.service.FileService;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import com.ytdinfo.inndoo.modules.core.entity.WhiteList;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListExtendRecord;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListRecord;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author yaochangning
 */
@Slf4j
@RestController
@Api(description = "账号自定义导出信息接口")
@RequestMapping("/accountIdExchange")
public class AccountIdExchangeController {

    @Autowired
    private FileService fileService;

    // 私有化导入监听器
    public class ExcelListener extends AnalysisEventListener<ExcelRow> {
        private List<ExcelRow> datas = new ArrayList<>();
        @Override
        public void invoke(ExcelRow excelRow, AnalysisContext analysisContext) {
            if (excelRow != null) {
                datas.add(excelRow);
            }
        }
        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        }

        public List<ExcelRow> getDatas() {
            return this.datas;
        }
    }

    @ResponseBody
    @RequestMapping(value = "/importData")
    @ApiOperation(value = "导入白名单信息")
    @SystemLog(description = "导入白名单信息")
    public Result<Map<String,Object>> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                      HttpServletRequest request, HttpSession session, HttpServletResponse response) throws IOException{
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        /*1.读Excel, 检查是否存在
          2.数据处理（判定这个名单是否含高级校验），如果含高级校验组装扩展表的数据
          2.批量插入
         */
        if(file == null){
            return new ResultUtil<Map<String,Object>>().setErrorMsg("未能读取到文件");
        }
        String fileName=file.getOriginalFilename();
        if(StringUtils.isBlank(fileName) ){
            return new ResultUtil<Map<String,Object>>().setErrorMsg("请上传xlsx，xls文件");
        }
        String suffix = StringUtils.substring(fileName,StringUtils.lastIndexOf(fileName,"."));
        if(!StringUtils.equalsAnyIgnoreCase(suffix,".xlsx") &&  !StringUtils.equalsAnyIgnoreCase(suffix,".xls") ){
            return new ResultUtil<Map<String,Object>>().setErrorMsg("请上传xlsx，xls文件");
        }
        long start = System.currentTimeMillis();
        System.out.println(start);
        String contentType = file.getContentType();
        // 导入文件地址
        String filePath = ReadExcelUtil.FilePath(file, request);
        // excel读取方法
        ExcelListener excelListener = new ExcelListener();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            return new ResultUtil<Map<String,Object>>().setErrorMsg("文件读取异常！");
        }
        List<ExcelRow> excelRows = excelListener.getDatas();
        long redtime = (System.currentTimeMillis()-start)/1000;
        long preTime = 0;
        long writeTime1 = 0;
        long writeTime2 = 0;
        long writeTime3 = 0;
        long writeTime4 = 0;

        if (excelRows != null && excelRows.size() > 0) {
            // 批量处理

            List<List<String>> rows = new ArrayList<>();
            for (ExcelRow temp : excelRows) {
                List<String> encryptList = new ArrayList<>();
                String identifier = temp.getColum0();
                if(StrUtil.isNotBlank(identifier)){
                    String encryptValue = AESUtil.encrypt( identifier,AESUtil.WXLOGIN_PASSWORD);
//                    String encryptValue = FBAesUtil.aesEncrypt(identifier);
                    encryptList.add(encryptValue);
                }
                rows.add(encryptList);
            }
            preTime = (System.currentTimeMillis()-start)/1000 - redtime;

            try {
                ApplicationHome home = new ApplicationHome(getClass());
                File jarFile = home.getSource();
                String path = jarFile.getParentFile().getPath();
                String rootPath = path + File.separator ;
                File dir = new File(rootPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String excelFileName =File.separator +  "static" + File.separator +"ytdexports"  + File.separator  +
                        UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
                String fullFileName = rootPath + excelFileName;
                BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
                // 一次性写出内容，使用默认样式
                writer.write(rows);
                // 关闭writer，释放内存
                writer.close();
                File filee = new File(fullFileName);
             //   ServletUtil.write(response, filee);
                System.out.println(fullFileName);
                Result<Object> result =  fileService.upload(filee,contentType);
                if(result.isSuccess()){
                    System.out.println(result.getResult().toString());
                    Map<String,Object> map = new HashMap<>();
                    map.put("url",result.getResult().toString());
                    return new ResultUtil<Map<String,Object>>().setData(map);
                }
                rows.clear();
                filee.delete();
            } catch (Exception e) {
                // 重置response
                response.reset();
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("下载文件失败")));
            }
        }


        System.out.println("总耗时"+(System.currentTimeMillis()-start)/1000);
        return new ResultUtil<Map<String,Object>>().setSuccessMsg("导入成功");
//        return new ResultUtil<Object>().setSuccessMsg("导入成功, \\n"
//                +"读取耗时："+redtime
//                +" \\n组装耗时:"+preTime
//                +" \\n写入耗时1:"+writeTime1
//                +" \\n写入耗时2:"+writeTime2
//                +" \\n写入耗时3:"+writeTime3
//                +" \\n写入耗时4:"+writeTime4
//                +" \\n总耗时"+(System.currentTimeMillis()-start)/1000);
    }

    @RequestMapping(value = "/importData1")
    @ApiOperation(value = "导入白名单信息")
    @SystemLog(description = "导入白名单信息")
    public void importExcel1(@RequestParam(value = "file", required = true) MultipartFile file,
                            HttpServletRequest request, HttpSession session, HttpServletResponse response) throws IOException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return;
        }
        /*1.读Excel, 检查是否存在
          2.数据处理（判定这个名单是否含高级校验），如果含高级校验组装扩展表的数据
          2.批量插入
         */
        if (file == null) {
            // 未能读取到文件
            return;
        }
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            //请上传xlsx，xls文件
            return;
        }
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (!StringUtils.equalsAnyIgnoreCase(suffix, ".xlsx") && !StringUtils.equalsAnyIgnoreCase(suffix, ".xls")) {
            //请上传xlsx，xls文件
            return;
        }
        long start = System.currentTimeMillis();
        System.out.println(start);

        // 导入文件地址
        String filePath = ReadExcelUtil.FilePath(file, request);
        // excel读取方法
        ExcelListener excelListener = new ExcelListener();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            //文件读取异常！
            return;
        }
        List<ExcelRow> excelRows = excelListener.getDatas();
        long redtime = (System.currentTimeMillis() - start) / 1000;
        long preTime = 0;
        long writeTime1 = 0;
        long writeTime2 = 0;
        long writeTime3 = 0;
        long writeTime4 = 0;

        if (excelRows != null && excelRows.size() > 0) {
            // 批量处理

            List<List<String>> rows = new ArrayList<>();
            for (ExcelRow temp : excelRows) {
                List<String> encryptList = new ArrayList<>();
                String identifier = temp.getColum0();
                if (StrUtil.isNotBlank(identifier)) {
                    String encryptValue = AESUtil.encrypt(identifier, AESUtil.WXLOGIN_PASSWORD);
                    encryptList.add(encryptValue);
                }
                rows.add(encryptList);
            }
            preTime = (System.currentTimeMillis() - start) / 1000 - redtime;
            ExcelWriter writer = null;
            OutputStream out = null;
            try {
//            response.setContentType("application/vnd.ms-excel");
//            response.setCharacterEncoding("utf-8");
                //String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
                String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String excelFileName = "解密数据" + downloadDate + ".xlsx";
//            response.setHeader("Content-disposition", "attachment;filename=" + excelFileName );
//            // 这里需要设置不关闭流
//            EasyExcel.write(response.getOutputStream()).autoCloseStream(Boolean.FALSE).sheet("模板")
//                    .doWrite(rows);
                out = response.getOutputStream();
                response.setContentType("multipart/form-data");
                response.setCharacterEncoding("utf-8");
                response.setHeader("Content-disposition", "attachment;filename=" + URLUtil.encode(excelFileName, StringUtil.UTF8));
                writer = new ExcelWriter(out, ExcelTypeEnum.XLSX, true);
                Sheet sheet = new Sheet(1, 0);
                sheet.setAutoWidth(Boolean.TRUE);
                writer.write(rows, sheet);
                rows.clear();
            } catch (Exception e) {
                // 重置response
                response.reset();
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getWriter().println(JSONUtil.toJsonStr(new ResultUtil<Object>().setErrorMsg("下载文件失败")));
            } finally {
                out.flush();
                writer.finish();
                out.close();
            }
        }
    }
}
