package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.sun.javafx.scene.shape.PathUtils;
import com.ytdinfo.conf.core.XxlConfClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import sun.net.www.content.image.gif;
import sun.net.www.content.image.jpeg;
import sun.net.www.content.image.png;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件头工具类
 *
 * @author zhulin
 * @Version v1.0.0
 * @date 2022-01-28 1:21 下午
 **/
@Slf4j
public class FileHeaderUtil {
    private final static Logger logger = LoggerFactory.getLogger(FileHeaderUtil.class);

    // 文件类型->文件头
    public static final Map<String, Set<String>> TYPE_HEADER_MAP = new HashMap<>();
    // 文件头->文件类型
    public static final Map<String, Set<String>> HEADER_TYPE_MAP = new HashMap<>();

    // 文件头为空（比如空的Word文件）
    public static final String EMPTY_HEADER = "00000000";


    // 忽略类型
    public static  final Set<String>  IGNORE_TYPE = new HashSet<>();


    static {
        /*
         * 文件头与文件类型是多对多的关系，如zip：504b0304、504b0506，以及504b0304：zip、docx、pptx，另外有些文件如txt没有文件头
         * 文件头尽可能宽松，防止常见类型无法通过校验，比如遇到通过视频抓拍的截图是ffd8fffe，与查到详细文件头是没有匹配的
         * 参考来源：https://www.filesignatures.net/index.php , http://www.nicetool.net/embed/file_signature.html
         */

        //   .gif;.png;.bmp;.jpg;.jpeg;.mp3;.wma;.flv;.mp4;.wmv;.ogg;.avi;.csv;.doc;.docx;.xls;.xlsx;.ppt;.pptx;.pdf;.sketch;.psd

        Map<String, String[]> typeHeaderArrMap = new HashMap<>();
        typeHeaderArrMap.put("txt", new String[]{""});
        typeHeaderArrMap.put("jpg", new String[]{"FFD8FF"});
        typeHeaderArrMap.put("jpeg", new String[]{"FFD8FF"});
        typeHeaderArrMap.put("png", new String[]{"89504E470D0A1A0A"});
        typeHeaderArrMap.put("bmp", new String[]{"424D"});
        typeHeaderArrMap.put("gif", new String[]{"47494638"});
        typeHeaderArrMap.put("tif", new String[]{"492049","49492A00","4D4D002A","4D4D002B"});
        typeHeaderArrMap.put("pic", new String[]{""});
        typeHeaderArrMap.put("doc", new String[]{"D0CF11E0A1B11AE1", "0D444F43", "CF11E0A1B11AE100", "DBA52D00", "ECA5C100"});
        typeHeaderArrMap.put("docx", new String[]{"504B0304", "504B030414000600"});
        typeHeaderArrMap.put("wps", new String[]{"0E574B53", "FF00020004040554", "D0CF11E0A1B11AE1"});
        typeHeaderArrMap.put("xls", new String[]{"D0CF11E0A1B11AE1", "0908100000060500", "FDFFFFFF10", "FDFFFFFF1F", "FDFFFFFF22", "FDFFFFFF23", "FDFFFFFF28", "FDFFFFFF29"});
        typeHeaderArrMap.put("xlsx", new String[]{"504B0304", "504B030414000600"});
        typeHeaderArrMap.put("et", new String[]{""});
        typeHeaderArrMap.put("ppt", new String[]{"D0CF11E0A1B11AE1", "006E1EF0", "0F00E803", "A0461DF0", "FDFFFFFF0E000000", "FDFFFFFF1C000000", "FDFFFFFF43000000"});
        typeHeaderArrMap.put("pptx", new String[]{"504B0304", "504B030414000600"});
        typeHeaderArrMap.put("pps", new String[]{"D0CF11E0A1B11AE1"});
        typeHeaderArrMap.put("pot", new String[]{""});
        typeHeaderArrMap.put("pdf", new String[]{"25504446"});
        typeHeaderArrMap.put("dwg", new String[]{"41433130"});
        typeHeaderArrMap.put("mp4", new String[]{"000000146674797069736F6D", "0000001866747970", "0000001C66747970"});
        typeHeaderArrMap.put("avi", new String[]{"52494646"});
        typeHeaderArrMap.put("rmvb", new String[]{"2E524D46"});
        typeHeaderArrMap.put("rm", new String[]{"2E524D46"});
        typeHeaderArrMap.put("flv", new String[]{"464C56"});
        typeHeaderArrMap.put("wma", new String[]{"3026B2758E66CF11"});
        typeHeaderArrMap.put("wmv", new String[]{"3026B2758E66CF11"});
        typeHeaderArrMap.put("wma", new String[]{"3026B2758E66CF11"});
        typeHeaderArrMap.put("asf", new String[]{"3026B2758E66CF11"});
        typeHeaderArrMap.put("mkv", new String[]{"1A45DFA393428288"});
        typeHeaderArrMap.put("mov", new String[]{"6D6F6F76", "66726565", "6D646174", "77696465", "706E6F74", "736B6970"});
        typeHeaderArrMap.put("mpeg", new String[]{""});
        typeHeaderArrMap.put("zip", new String[]{"504B0304", "504B4C495445", "504B537058", "504B0506", "504B0708", "57696E5A6970", "504B030414000100"});
        typeHeaderArrMap.put("mr", new String[]{"504B0304", "504B4C495445", "504B537058", "504B0506", "504B0708", "57696E5A6970", "504B030414000100"});
        typeHeaderArrMap.put("rar", new String[]{"526172211A0700"});

        //新增类型
        typeHeaderArrMap.put("mp3", new String[]{"49443303000000002176"});
        typeHeaderArrMap.put("ogg", new String[]{"4F67675300020000"});
        // Photoshop (psd)
        typeHeaderArrMap.put("psd", new String[]{"38425053000100000000"});
        // Rich Text Format (rtf)
        //typeHeaderArrMap.put("rtf", new String[]{"7B5C727466315C616E73"});
        // Email [Outlook Express 6] (eml)
        //typeHeaderArrMap.put("eml", new String[]{"46726F6D3A203D3F6762"});
        //Visio 绘图
        //typeHeaderArrMap.put("vsd", new String[]{"D0CF11E0A1B11AE10000"});
        // MS Access (mdb)
        //typeHeaderArrMap.put("mdb", new String[]{"5374616E64617264204A"});
        //typeHeaderArrMap.put("ps", new String[]{"252150532D41646F6265"});
        //typeHeaderArrMap.put("mpg", new String[]{"000001ba210001000180"});
        // Wave (wav)
        //typeHeaderArrMap.put("wav", new String[]{"52494646e27807005741"});
        // MIDI (mid)
        //typeHeaderArrMap.put("mid", new String[]{"4d546864000000060001"});
        //typeHeaderArrMap.put("ini", new String[]{"235468697320636f6e66"});
        // Real Audio (ram)
        //typeHeaderArrMap.put("ram", new String[]{"2E7261FD"});
        // Windows Password (pwl)
        //typeHeaderArrMap.put("pwl", new String[]{"E3828596"});
        // Quicken (qdf)
        //typeHeaderArrMap.put("qdf", new String[]{"AC9EBD8F"});
        // Outlook (pst)
        //typeHeaderArrMap.put("pst", new String[]{"2142444E"});
        // Outlook Express (dbx)
        //typeHeaderArrMap.put("dbx", new String[]{"CFAD12FEC5FD746F"});
        // WordPerfect (wpd)
        //typeHeaderArrMap.put("wpd", new String[]{"FF575043"});
        //typeHeaderArrMap.put("torrent", new String[]{"6431303a637265617465"});
        //bat文件
        //typeHeaderArrMap.put("mxp", new String[]{"04000000010000001300"});
        //bat文件
        //typeHeaderArrMap.put("chm", new String[]{"49545346030000006000"});
        //typeHeaderArrMap.put("class", new String[]{"cafebabe0000002e0041"});
        //typeHeaderArrMap.put("gz", new String[]{"1f8b0800000000000000"});
        //typeHeaderArrMap.put("bat", new String[]{"406563686f206f66660d"});
        //typeHeaderArrMap.put("java", new String[]{"7061636b616765207765"});
        //typeHeaderArrMap.put("mf", new String[]{"4d616e69666573742d56"});
        //typeHeaderArrMap.put("jsp", new String[]{"3c25402070616765206c"});
        //typeHeaderArrMap.put("exe", new String[]{"4d5a9000030000000400"});
        //typeHeaderArrMap.put("jar", new String[]{"504B03040a0000000000"});


        IGNORE_TYPE.add("quicktime");
        IGNORE_TYPE.add("sketch");


        // 文件类型->文件头
        TYPE_HEADER_MAP.putAll(typeHeaderArrMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(Arrays.asList(entry.getValue())))));
        // 文件头->文件类型
        for (Map.Entry<String, Set<String>> entry : TYPE_HEADER_MAP.entrySet()) {
            for (String header : entry.getValue()) {
                Set<String> types = HEADER_TYPE_MAP.getOrDefault(header, new HashSet<>());
                types.add(entry.getKey());
                HEADER_TYPE_MAP.put(header, types);
            }
        }
    }

    /**
     * 获取文件头
     *
     * @param file
     *            MultipartFile
     * @return 文件头
     */
    public static String getFileHeader(MultipartFile file) {
        String value = null;
        try (InputStream is = file.getInputStream()) {
            byte[] b = new byte[4];
            // noinspection ResultOfMethodCallIgnored
            is.read(b, 0, b.length);
            value = bytesToHexString(b);
        } catch (Exception e) {
            logger.error("读取文件头信息异常", e);
        }
        return value;
    }

    /**
     * 将要读取文件头信息的文件的byte数组转换成string类型表示
     *
     * @param src
     *            要读取文件头信息的文件的byte数组
     * @return 文件头十六进制信息
     */
    private static String bytesToHexString(byte[] src) {
        StringBuilder builder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        String hv;
        for (byte b : src) {
            // 以十六进制（基数 16）无符号整数形式返回一个整数参数的字符串表示形式，并转换为大写
            hv = Integer.toHexString(b & 0xFF).toUpperCase();
            if (hv.length() < 2) {
                builder.append(0);
            }
            builder.append(hv);
        }
        return builder.toString();
    }

    /**
     * 是否为无法校验的文件类型
     *
     * @param type
     *            文件类型
     * @return true：需跳过；false：无需跳过；
     */
    private static boolean isSkipType(String type) {
        // 获取文件头为空的类型
        Set<String> types = HEADER_TYPE_MAP.get(StrUtil.EMPTY);
        return type != null && types.contains(type);
    }

    /**
     * 获取指定文件头对应的文件类型
     *
     * @param fileHeader
     *            文件头
     * @return 文件类型
     */
    public static Set<String> getFileTypesByFileHeader(String fileHeader) {
        // 直接查找，完全匹配
        Set<String> fileTypes = HEADER_TYPE_MAP.get(fileHeader);
        // 未找到再去部分匹配一遍
        if (fileHeader != null && fileTypes == null) {
            // 宽松匹配，文件头没有直接对应，查询部分匹配的
            for (Map.Entry<String, Set<String>> entry : HEADER_TYPE_MAP.entrySet()) {
                if (StrUtil.isNotBlank(entry.getKey())
                        && (entry.getKey().startsWith(fileHeader) || fileHeader.startsWith(entry.getKey()))) {
                    // 记录符合条件的文件头
                    if (fileTypes == null) {
                        fileTypes = new HashSet<>();
                    }
                    fileTypes.addAll(entry.getValue());
                }
            }
        }
        return fileTypes;
    }

    /**
     * 判断文件扩展名是否有效
     *
     * @param file
     *            文件
     * @return true：有效；false：无效；
     */
    public static boolean isValidExtension(MultipartFile file) {
        // 获取文件扩展名
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        // 查询文件头对应的文件类型
        return TYPE_HEADER_MAP.containsKey(extension);
    }


    /**
     * 判断文件头是否有效
     *
     * @param file
     *            文件
     * @return true：有效；false：无效；
     */
    public static boolean isValidHeader(MultipartFile file) {
        // 获取文件头
        String fileHeader = FileHeaderUtil.getFileHeader(file);
        // 查询文件头对应的文件类型
        return getFileTypesByFileHeader(fileHeader) != null;
    }

    public static boolean isConsistentType(File file)  {
        String check =  XxlConfClient.get("matrix.fileheader.check");
        if(!StrUtil.equalsIgnoreCase("true",check)){
            return true;
        }
        // 获取文件扩展名
        String extension =  cn.hutool.core.io.FileUtil.extName(file);
        // 如果是特殊类型，跳过判断
        if (isSkipType(extension)) {
            return true;
        }
        // 是否是忽略检查类型
        if(IGNORE_TYPE.contains(extension)){
            return true;
        }
        String fileHeader = "";
        // 获取文件头

        try (    FileInputStream is = new FileInputStream(file)) {
            byte[] b = new byte[4];
            // noinspection ResultOfMethodCallIgnored
            is.read(b, 0, b.length);
            fileHeader = bytesToHexString(b);
        } catch (Exception e) {
            logger.error("读取文件头信息异常", e);
        }
        // 空文件判断？目前已知空word文档是空文件头
        if (EMPTY_HEADER.equals(fileHeader)) {
            return true;
        }
        // 查询文件类型预期对应的文件头
        Set<String> fileHeaders = TYPE_HEADER_MAP.get(extension);
        if (StrUtil.isNotBlank(extension) && fileHeader != null && !fileHeaders.isEmpty()) {
            // 对比实际文件头与预期文件头是否一致
            boolean consistent = fileHeaders.contains(fileHeader);
            if (!consistent) {
                // 部分匹配
                for (String header : fileHeaders) {
                    if (StrUtil.isNotBlank(header)
                            && (header.startsWith(fileHeader) || fileHeader.startsWith(header))) {
                        consistent = true;
                        break;
                    }
                }
            }
            return consistent;
        }
        return false;
    }


    /**
     * 判断文件头对应的类型是否与扩展名一致
     *
     * @param file
     *            文件
     * @return true：一致的；false：不一致的；
     */
    public static boolean isConsistentType(MultipartFile file) {
        String check =  XxlConfClient.get("matrix.fileheader.check");
        if(!StrUtil.equalsIgnoreCase("true",check)){
            return true;
        }
        // 获取文件扩展名
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        // 如果是特殊类型，跳过判断
        if (isSkipType(extension)) {
            return true;
        }
        // 是否是忽略检查类型
        if(IGNORE_TYPE.contains(extension)){
            return true;
        }
        String fileHeader = "";
        // 获取文件头
        fileHeader = FileHeaderUtil.getFileHeader(file);
        // 空文件判断？目前已知空word文档是空文件头
        if (EMPTY_HEADER.equals(fileHeader)) {
            return true;
        }
        // 查询文件类型预期对应的文件头
        Set<String> fileHeaders = TYPE_HEADER_MAP.get(extension);
        if (StrUtil.isNotBlank(extension) && fileHeader != null && fileHeaders !=null  && !fileHeaders.isEmpty()) {
            // 对比实际文件头与预期文件头是否一致
            boolean consistent = fileHeaders.contains(fileHeader);
            if (!consistent) {
                // 部分匹配
                for (String header : fileHeaders) {
                    if (StrUtil.isNotBlank(header)
                            && (header.startsWith(fileHeader) || fileHeader.startsWith(header))) {
                        consistent = true;
                        break;
                    }
                }
            }
            return consistent;
        }
        return false;
    }
}