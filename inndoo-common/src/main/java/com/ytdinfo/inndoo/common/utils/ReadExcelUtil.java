package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.BaseRowModel;
import com.alibaba.excel.metadata.Sheet;
import com.ytdinfo.inndoo.common.vo.ExcelRow;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @description:
 * @author: jiangxingyao
 * @date: 19-10-11 16:16
 */
@Slf4j
public class ReadExcelUtil {

    private static Sheet initSheet;

    static {
        initSheet = new Sheet(1, 0);
        initSheet.setSheetName("sheet");
        //设置自适应宽度
        initSheet.setAutoWidth(Boolean.TRUE);
    }

    /**
     * 读取少于1000行数据
     * @param filePath 文件绝对路径
     * @return
     */
    public static List<Object> readLessThan1000Row(String filePath){
        return readLessThan1000RowBySheet(filePath,null);
    }

    /**
     * 读小于1000行数据, 带样式
     * filePath 文件绝对路径
     * initSheet ：
     *      sheetNo: sheet页码，默认为1
     *      headLineMun: 从第几行开始读取数据，默认为0, 表示从第一行开始读取
     *      clazz: 返回数据List<Object> 中Object的类名
     */
    public static List<Object> readLessThan1000RowBySheet(String filePath, Sheet sheet){
        if(!StringUtils.hasText(filePath)){
            return null;
        }

        sheet = sheet != null ? sheet : initSheet;

        InputStream fileStream = null;
        try {
            fileStream = new BufferedInputStream(new FileInputStream(filePath));
            return EasyExcelFactory.read(fileStream, sheet);
        } catch (FileNotFoundException e) {
            log.info("找不到文件或文件路径错误, 文件：{}", filePath);
        }finally {
            try {
                if(fileStream != null){
                    fileStream.close();
                }
            } catch (IOException e) {
                log.info("excel文件读取失败, 失败原因：{}", e);
            }
        }
        return null;
    }

    /**
     * 读大于1000行数据
     * @param filePath 文件绝对路径
     * @return
     */
    public static List<Object> readMoreThan1000Row(String filePath){
        return readMoreThan1000RowBySheet(filePath,null,null);
    }


    /**
     * 读大于1000行数据
     * @param filePath 文件绝对路径
     * @return
     */
    public static List<Object> readMoreThan1000Row(String filePath, Class<? extends BaseRowModel> clazz){
        return readMoreThan1000RowBySheet(filePath,null,clazz);
    }


    public static AnalysisEventListener readMoreThan1000RowBySheet(String filePath, Sheet sheet,AnalysisEventListener excelListener,  Class<? extends BaseRowModel> clazz){
        if(!StringUtils.hasText(filePath)){
            return null;
        }
        sheet = sheet != null ? sheet : initSheet;
        if(clazz != null){
            sheet.setClazz(clazz);
        }
        InputStream fileStream = null;
        try {
            fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, sheet, excelListener);
            return excelListener;
        } catch (FileNotFoundException e) {
            log.error("找不到文件或文件路径错误, 文件：{}", filePath);
        }finally {
            try {
                if(fileStream != null){
                    fileStream.close();
                }
            } catch (IOException e) {
                log.error("excel文件读取失败, 失败原因：{}", e);
            }
        }
        return null;
    }


    /**
     * 读大于1000行数据, 带样式
     * @param filePath 文件绝对路径
     * @return
     */
    public static List<Object> readMoreThan1000RowBySheet(String filePath, Sheet sheet,  Class<? extends BaseRowModel> clazz){
        if(!StringUtils.hasText(filePath)){
            return null;
        }

        sheet = sheet != null ? sheet : initSheet;
        if(clazz != null){
            sheet.setClazz(clazz);
        }
        InputStream fileStream = null;
        try {
            fileStream = new BufferedInputStream(new FileInputStream(filePath));
            ExcelListener excelListener = new ExcelListener();
//            context.readWorkbookHolder().getDefaultReturnMap()
            EasyExcelFactory.readBySax(fileStream, sheet, excelListener);
            return excelListener.getDatas();
        } catch (FileNotFoundException e) {
            log.error("找不到文件或文件路径错误, 文件：{}", filePath);
        }finally {
            try {
                if(fileStream != null){
                    fileStream.close();
                }
            } catch (IOException e) {
                log.error("excel文件读取失败, 失败原因：{}", e);
            }
        }
        return null;
    }

    /**
     * 生成excle
     * @param filePath  绝对路径, 如：/home/jiangxingyao/Downloads/aaa.xlsx
     * @param data 数据源
     * @param head 表头
     */
    public static void writeBySimple(String filePath, List<List<Object>> data, List<String> head){
        writeSimpleBySheet(filePath,data,head,null);
    }

    /**
     * 生成excle
     * @param filePath 绝对路径, 如：/home/jiangxingyao/Downloads/aaa.xlsx
     * @param data 数据源
     * @param sheet excle页面样式
     * @param head 表头
     */
    public static void writeSimpleBySheet(String filePath, List<List<Object>> data, List<String> head, Sheet sheet){
        sheet = (sheet != null) ? sheet : initSheet;

        if(head != null){
            List<List<String>> list = new ArrayList<>();
            head.forEach(h -> list.add(Collections.singletonList(h)));
            sheet.setHead(list);
        }

        OutputStream outputStream = null;
        ExcelWriter writer = null;
        try {
            outputStream = new FileOutputStream(filePath);
            writer = EasyExcelFactory.getWriter(outputStream);
            writer.write1(data,sheet);
        } catch (FileNotFoundException e) {
            log.error("找不到文件或文件路径错误, 文件：{}", filePath);
        }finally {
            try {
                if(writer != null){
                    writer.finish();
                }

                if(outputStream != null){
                    outputStream.close();
                }

            } catch (IOException e) {
                log.error("excel文件导出失败, 失败原因：{}", e);
            }
        }

    }

    /**
     * 生成excle
     * @param filePath 绝对路径, 如：/home/jiangxingyao/Downloads/aaa.xlsx
     * @param data 数据源
     */
    public static void writeWithTemplate(String filePath, List<? extends BaseRowModel> data){
        writeWithTemplateAndSheet(filePath,data,null);
    }

    /**
     * 生成excle
     * @param filePath 绝对路径, 如：/home/jiangxingyao/Downloads/aaa.xlsx
     * @param data 数据源
     * @param sheet excle页面样式
     */
    public static void writeWithTemplateAndSheet(String filePath, List<? extends BaseRowModel> data, Sheet sheet){
        if(CollectionUtils.isEmpty(data)){
            return;
        }

        sheet = (sheet != null) ? sheet : initSheet;
        sheet.setClazz(data.get(0).getClass());

        OutputStream outputStream = null;
        ExcelWriter writer = null;
        try {
            outputStream = new FileOutputStream(filePath);
            writer = EasyExcelFactory.getWriter(outputStream);
            writer.write(data,sheet);
        } catch (FileNotFoundException e) {
            log.error("找不到文件或文件路径错误, 文件：{}", filePath);
        }finally {
            try {
                if(writer != null){
                    writer.finish();
                }

                if(outputStream != null){
                    outputStream.close();
                }
            } catch (IOException e) {
                log.error("excel文件导出失败, 失败原因：{}", e);
            }
        }

    }

    /**
     * 生成多Sheet的excle
     * @param filePath 绝对路径, 如：/home/jiangxingyao/Downloads/aaa.xlsx
     * @param multipleSheelPropetys
     */
    public static void writeWithMultipleSheel(String filePath,List<MultipleSheelPropety> multipleSheelPropetys){
        if(CollectionUtils.isEmpty(multipleSheelPropetys)){
            return;
        }

        OutputStream outputStream = null;
        ExcelWriter writer = null;
        try {
            outputStream = new FileOutputStream(filePath);
            writer = EasyExcelFactory.getWriter(outputStream);
            for (MultipleSheelPropety multipleSheelPropety : multipleSheelPropetys) {
                Sheet sheet = multipleSheelPropety.getSheet() != null ? multipleSheelPropety.getSheet() : initSheet;
                if(!CollectionUtils.isEmpty(multipleSheelPropety.getData())){
                    sheet.setClazz(multipleSheelPropety.getData().get(0).getClass());
                }
                writer.write(multipleSheelPropety.getData(), sheet);
            }

        } catch (FileNotFoundException e) {
            log.error("找不到文件或文件路径错误, 文件：{}", filePath);
        }finally {
            try {
                if(writer != null){
                    writer.finish();
                }

                if(outputStream != null){
                    outputStream.close();
                }
            } catch (IOException e) {
                log.error("excel文件导出失败, 失败原因：{}", e);
            }
        }

    }


    /*********************匿名内部类开始，可以提取出去******************************/

    @Data
    public static class MultipleSheelPropety{

        private List<? extends BaseRowModel> data;

        private Sheet sheet;
    }

    /**
     * 解析监听器，
     * 每解析一行会回调invoke()方法。
     * 整个excel解析结束会执行doAfterAllAnalysed()方法
     *
     * @author: jiangxingyao
     * @date: 19-10-11 14:11
     */
    @Getter
    @Setter
    public static class ExcelListener<T> extends AnalysisEventListener<T> {
        private List<T> datas = new ArrayList<>();

        /**
         * 逐行解析
         * object : 当前行的数据
         */
        @Override
        public void invoke(T object, AnalysisContext context) {
            //当前行
//            context.readWorkbookHolder().setDefaultReturnMap(false);
            // context.getCurrentRowNum()s
            if (object != null) {
                datas.add(object);
            }
        }
        /**
         * 解析完所有数据后会调用该方法
         */
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            //解析结束销毁不用的资源
        }

    }

    /************************匿名内部类结束，可以提取出去***************************/

    /************************自定义公共方法***************************************/
    /**
     * 返回文件地址
     * @param file
     * @param request
     * @return
     */
    public static String FilePath(MultipartFile file, HttpServletRequest request) {
        String replyPicName = "";
        String realPath = "";
        File excelFile = null;
        InputStream is = null;
        Date date = new Date();
        String uploadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(date);
        try {
            replyPicName = FilenameUtils.normalize(uploadDate + file.getOriginalFilename());
            realPath = FileUtil.getAbsolutePath("excel");
            is = file.getInputStream();
            excelFile = new File(realPath, replyPicName);
            FileUtils.copyInputStreamToFile(is, excelFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return excelFile.getPath();
    }


    public static List<ExcelRow>  readExcel(String path, Class head){
        ExcelListener excelListener = new ExcelListener();
        ExcelReader excelReader = EasyExcelFactory.read(path, head, excelListener).headRowNumber(0).build();
        excelReader.read();
        List<ExcelRow> listMap = excelListener.getDatas();
        return listMap;
    }

    public static List<ExcelRow>  readExcel(String path){
       return readExcel(path,ExcelRow.class);
    }


//    public static <T extends BaseRowModel>  List<T> read(File file,Class<? extends BaseRowModel> clazz){
//        ExcelListener excelListener = new ExcelListener();
//        List<T> list =new ArrayList<>();
//        try {
//            InputStream inputStream = new FileInputStream(file);
//            List<Object> ExcelRow = null;
//            ExcelRow = EasyExcelFactory.read(inputStream, new Sheet(1, 1, clazz));
//            for(Object o:ExcelRow){
//                try {
//                    T entity = (T) o;
//                    list.add(entity);
//                } catch (Exception e) {
//                    continue;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//          return  list;
//        }
//    }
//
//

//    public static  <T extends BaseRowModel>  List<T>  read(InputStream inputStream, Class<? extends BaseRowModel> clazz){
//        ExcelListener excelListener = new ExcelListener();
//        List<T> list =new ArrayList<>();
//        List<Object> ExcelRow = null;
//
//        EasyExcel.read(inputStream, clazz, new ExcelListener()).sheet().doRead();
//        ExcelRow = EasyExcelFactory.read(new BufferedInputStream(inputStream), new Sheet(1, 1, clazz));
//        for(Object o:ExcelRow){
//                try {
//                    T entity = (T) o;
//                    list.add(entity);
//                } catch (Exception e) {
//                    continue;
//                }
//        }
//        return list;
//    }

}
