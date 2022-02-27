package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.ThreadPoolType;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.utils.excel.WaterExcelUtil;
import com.ytdinfo.inndoo.common.utils.excel.WaterMarkHandler;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.base.service.FileService;
import com.ytdinfo.inndoo.modules.base.vo.StaffSearchVo;
import com.ytdinfo.inndoo.modules.core.dto.StaffDto;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.entity.export.StaffExport;
import com.ytdinfo.inndoo.modules.core.service.*;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffRoleService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IStaffService;
import com.ytdinfo.inndoo.modules.core.serviceimpl.StaffServiceImpl;
import com.ytdinfo.inndoo.utils.PrivacyUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * @author Timmy
 */
@Slf4j
@RestController
@Api(description = "员工信息管理接口")
@RequestMapping("/staff")
public class StaffController extends BaseController<Staff, String> {

    @Autowired
    private StaffService staffService;

    @Override
    public StaffService getService() {
        return staffService;
    }

    @Autowired
    private ActAccountService actAccountService;

    @Autowired
    private IStaffService iStaffService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private FileService fileService;

    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    @Autowired
    private StaffRoleService staffRoleService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @Autowired
    private StaffServiceImpl staffServiceImpl;

    @Autowired
    private IStaffRoleService iStaffRoleService;


    @XxlConf("core.spring.datasource.url")
    private String dbUrl;
    @XxlConf("core.spring.datasource.username")
    private String username;
    @XxlConf("core.spring.datasource.password")
    private String password;

    @Autowired
    private MatrixApiUtil matrixApiUtil;
    /**
     * 更新成功数量
     */
    volatile Integer successInteger = 0;
    /**
     * 更新失败数量
     */
    volatile Integer errInteger = 0;

    @Autowired
    private RoleStaffService roleStaffService;

    /**
     * 业务经理角色code
     */
    private final static String STAFF_BUSINESS_MANAGER = "STAFF_BUSINESS_MANAGER";

    @ResponseBody
    @RequestMapping(value = "/importPicture")
    @ApiOperation(value = "员工批量修改头像，个人二维码")
    @SystemLog(description = "员工批量修改头像，个人二维码")
    public Result<Map<String, Object>> importPicture(@RequestParam(value = "file", required = true) MultipartFile file,
                                                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        String fileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (".xls.xlsx".indexOf(suffix) == -1) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("文件类型有误, 请上传.xls或.xlsx格式的文件");
        }
        String filePath = ReadExcelUtil.FilePath(file, request);
        ExcelReader reader = ExcelUtil.getReader(ResourceUtil.getStream(filePath));
        List<List<Object>> allList = reader.read();
        if (allList.size() <= 1) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("请确认员工号不为空");
        } else {
            List<Object> excelHead = allList.get(0);
            if (excelHead == null) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("没有头列导入模板错误！");
            }
            if (!"员工号".equals(excelHead.get(0))) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含员工列,导入模板错误！");
            }
            if (!"头像图片".equals(excelHead.get(1))) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含头像图片列,导入模板错误！");
            }
            if (!"二维码图片".equals(excelHead.get(2))) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含二维码图片列,导入模板错误！");
            }
        }
        Workbook book;
        if (fileName.endsWith(".xls")) {
            book = new HSSFWorkbook(ResourceUtil.getStream(filePath));
        } else {
            book = new XSSFWorkbook(filePath);
        }
        Map<String, Object> imagmap = new HashMap<>();
        if (book instanceof XSSFWorkbook) {
            XSSFSheet sheet = ((XSSFWorkbook) book).getSheetAt(book.getActiveSheetIndex());
            imagmap = getXSSFPictures(sheet);
        } else {
            HSSFSheet sheet = ((HSSFWorkbook) book).getSheetAt(book.getActiveSheetIndex());
            imagmap = getPictures(sheet);
        }
        // 更新员工列表
        List<Staff> updateList = new ArrayList<>();
        // 返回处理的结果集
        Vector<List<String>> rows = new Vector<>();
        List<String> rowhead = new ArrayList<>();
        rowhead.add("员工号");
        rowhead.add("失败原因");
        rowhead.add("结果");
        rows.add(rowhead);
        Integer errorInt = 0;
        Integer successInt = 0;
        List<String> duplicateStaffNo = new ArrayList<>();
        for (int i = 1; i < allList.size(); i++) {
            List<Object> objectList = allList.get(i);
            String staffNo = objectList.get(0).toString();
            List<String> rowbody = new ArrayList<>();
            rowbody.add(staffNo);
            Staff oldStaff = staffService.findByStaffNo(staffNo);
            if (oldStaff == null) {
                rowbody.add("未找到员工");
                rowbody.add("失败");
                errorInt = errorInt + 1;
                rows.add(rowbody);
                continue;
            }
            if (duplicateStaffNo.contains(staffNo)) {
                rowbody.add("重复员工号");
                rowbody.add("忽略");
                rows.add(rowbody);
                errorInt = errorInt + 1;
                continue;
            } else {
                duplicateStaffNo.add(staffNo);
            }
            Staff staff = new Staff();
            BeanUtils.copyProperties(oldStaff, staff);
            staff.setStaffNo(staffNo);
            boolean upload = false;
            if (imagmap.containsKey(i + "-1")) {
                upload = true;
                String headImg = imagmap.get(i + "-1").toString();
                staff.setHeadImg(headImg);
            }
            if (imagmap.containsKey(i + "-2")) {
                upload = true;
                String qrcode = imagmap.get(i + "-2").toString();
                staff.setQrcode(qrcode);
            }
            if (!upload) {
                rowbody.add("未上传图片");
                rowbody.add("忽略更新");
                errorInt = errorInt + 1;
            } else {
                rowbody.add("");
                rowbody.add("成功");
                updateList.add(staff);
                successInt = successInt + 1;
            }
            rows.add(rowbody);
        }
        if (updateList.size() > 0) {
            iStaffService.updateImg(updateList);
            //同步到活动平台
            /*
             List<Department> departments = departmentService.findAll();
            for (Staff staff:updateList) {
                ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
                modifyStaffVo.setType("updateImg");
                activityApiUtil.modifyStaff(modifyStaffVo);
            }*/
        }
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String path = jarFile.getParentFile().getPath();
        String rootPath = path + File.separator;
        File dir = new File(rootPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String excelFileName = File.separator + "static" + File.separator + "ytdexports" + File.separator +
                UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
        String fullFileName = rootPath + excelFileName;
        BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
        // 一次性写出内容，使用默认样式
        writer.write(rows);
        // 关闭writer，释放内存
        writer.close();
        File filee = new File(fullFileName);
        //   ServletUtil.write(response, filee);
        String contentType = file.getContentType();
        Map<String, Object> map = new HashMap<>(16);
        Result<Object> result = fileService.upload(filee, contentType);
        if (result.isSuccess()) {
            map.put("url", result.getResult().toString());
        } else {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("数据导出异常");
        }
        rows.clear();
        map.put("successInteger", successInt);
        map.put("errInteger", errorInt);
        filee.delete();
        return new ResultUtil<Map<String, Object>>().setData(map);
    }

    public Map<String, Object> getPictures(HSSFSheet sheet) throws IOException {
        Map<String, Object> returnmap = new HashMap<>();
        Map<String, HSSFPictureData> map = new HashMap<String, HSSFPictureData>();
        if (null == sheet.getDrawingPatriarch()) {
            return returnmap;
        }
        List<HSSFShape> list = sheet.getDrawingPatriarch().getChildren();
        for (HSSFShape shape : list) {
            if (shape instanceof HSSFPicture) {
                Date date = new Date();
                String uploadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(date);
                HSSFPicture picture = (HSSFPicture) shape;
                HSSFClientAnchor cAnchor = picture.getClientAnchor();
                HSSFPictureData pdata = picture.getPictureData();
                String key = cAnchor.getRow1() + "-" + cAnchor.getCol1();// 行号-列号
                //获取文件后缀名
                String ext = pdata.suggestFileExtension();
                //获取文件名称
                String picName = uploadDate + "." + ext;
                String type = pdata.getMimeType();
                byte[] data = pdata.getData();//图片数据
                int size = data.length;
                String fKey = CommonUtil.renamePic(picName);
                //服务器上传可更该代码
                InputStream input = new ByteArrayInputStream(data);

                String path = fileService.uploadFileInputStream(input, fKey, type, size);
                map.put(key, pdata);
                returnmap.put(key, path);
            }
        }
        return returnmap;
    }


    public Map<String, Object> getXSSFPictures(XSSFSheet sheet) throws IOException {
        Map<String, Object> returnmap = new HashMap<>();
        Map<String, XSSFPictureData> map = new HashMap<String, XSSFPictureData>();
        if (null == sheet.getDrawingPatriarch()) {
            return returnmap;
        }
        List<XSSFShape> list = sheet.getDrawingPatriarch().getShapes();
        for (XSSFShape shape : list) {
            if (shape instanceof XSSFPicture) {
                Date date = new Date();
                String uploadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(date);
                XSSFPicture picture = (XSSFPicture) shape;
                XSSFClientAnchor cAnchor = picture.getClientAnchor();
                XSSFPictureData pdata = picture.getPictureData();
                String key = cAnchor.getRow1() + "-" + cAnchor.getCol1();// 行号-列号
                //获取文件后缀名
                String ext = pdata.suggestFileExtension();
                //获取文件名称
                String picName = uploadDate + "." + ext;
                String type = pdata.getMimeType();
                byte[] data = pdata.getData();//图片数据
                int size = data.length;
                String fKey = CommonUtil.renamePic(picName);
                //服务器上传可更该代码
                InputStream input = new ByteArrayInputStream(data);

                String path = fileService.uploadFileInputStream(input, fKey, type, size);
                map.put(key, pdata);
                returnmap.put(key, path);
            }
        }
        return returnmap;
    }


    @ResponseBody
    @RequestMapping(value = "/importUpdateData")
    @ApiOperation(value = "员工批量修改")
    @SystemLog(description = "员工批量修改")
    public Result<Map<String, Object>> importtUpdateExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                                          @RequestParam String type,
                                                          HttpServletRequest request, HttpSession session, HttpServletResponse response) throws IOException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        //每次进入初始化统计数据
        successInteger = 0;
        errInteger = 0;
//        String appid = UserContext.getAppid();
//        InputStream is = null;
        String fileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (".csv.xlsx".indexOf(suffix) == -1) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("文件类型有误, 请上传.csv或者.xlsx格式的文件");
        }
        // excel读取方法
        List<ExcelRow> excelRows = ReadExcelUtil.readExcel(ReadExcelUtil.FilePath(file, request), ExcelRow.class);
        Set<String> phones = new HashSet<>();
        List<String> phoneList = new ArrayList<>();
        Set<String> staffNos = new HashSet<>();
        Vector<String> staffNoList = new Vector<>();
        Vector<List<String>> rows = new Vector<>();
        Vector<Staff> updateStaffs = new Vector<>();
        Map<String ,Vector<List<String>>> allMap = new HashMap<>();
        Set<String> roleNames = roleStaffService.getNameMap().keySet();
        Map<String, RoleStaff> roles = roleStaffService.getNameMap();
        if (excelRows != null && excelRows.size() > 0) {
            // 校验excel头部
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("没有头列导入模板错误！");
            }
            if (!"姓名".equals(excelHead.getColum0())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含姓名列,导入模板错误！");
            }
            if (!"手机号".equals(excelHead.getColum1())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含手机号列,导入模板错误！");
            }
            if (!"机构编码".equals(excelHead.getColum2())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含机构编码列,导入模板错误！");
            }
            if (!"员工号".equals(excelHead.getColum3())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含员工号列,导入模板错误！");
            }
            if (!"员工角色".equals(excelHead.getColum4())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("未包含员工角色列,导入模板错误！");
            }
            List<String> totalList = new ArrayList<>();
            totalList.add("姓名");
            totalList.add("手机号");
            totalList.add("机构编码");
            totalList.add("员工号");
            totalList.add("员工角色");
            totalList.add("错误原因");
            totalList.add("成功");
            rows.add(totalList);
            excelRows.remove(0);

            ThreadPoolExecutor pool = ThreadPoolUtil.createPool(ThreadPoolType.handleAccount);
            int num = 600;
            Map<String, List<ExcelRow>> mapExcelRow = new HashMap<>();
            if (excelRows.size() < num) {
                mapExcelRow.put("1", excelRows);
            } else {
                int times = excelRows.size() / num;
                for (int i = 0; i < times; i++) {
                    List<ExcelRow> temp = excelRows.subList(i * num, (i + 1) * num);
                    mapExcelRow.put(i + 1 + "", temp);
                }
                List<ExcelRow> temp1 = excelRows.subList(times * num, excelRows.size());
                if(CollectionUtil.isNotEmpty(temp1)){
                    mapExcelRow.put(times + 1 + "", temp1);
                }
            }

            Set<String> keySet = mapExcelRow.keySet();
            for (String key : keySet) {
                pool.execute(() -> {
                    List<ExcelRow> mapexcelRows = mapExcelRow.get(key);
                    Vector<List<String>> handlerows = new Vector<>();
                    for (ExcelRow temp : mapexcelRows) {
                        //表示已员工号为唯一标识
                        if ("1".equals(type)) {
                            List<String> resultList = new ArrayList<>();
                            String name = "";
                            if (StrUtil.isNotBlank(temp.getColum0())) {
                                name = temp.getColum0();
                            }
                            String phone = "";
                            if (StrUtil.isNotBlank(temp.getColum1())) {
                                phone = temp.getColum1();
                            }
                            String code = "";
                            if (StrUtil.isNotBlank(temp.getColum2())) {
                                code = temp.getColum2();
                            }
                            String staffNo = "";
                            if (StrUtil.isNotBlank(temp.getColum3())) {
                                staffNo = temp.getColum3();
                            }
                            String roleName = "";
                            if (StrUtil.isNotBlank(temp.getColum4())) {
                                roleName = temp.getColum4();
                            }
                            resultList.add(name);
                            resultList.add(phone);
                            resultList.add(code);
                            resultList.add(staffNo);
                            resultList.add(roleName);
                            if (StrUtil.isNotBlank(staffNo)) {
                                if (StrUtil.isNotBlank(code)) {
                                    Department d = departmentService.findByDeptCode(temp.getColum2());
                                    if (d == null) {
                                        resultList.add(temp.getColum2() + "机构不存在！");
                                        resultList.add("");
                                        errInteger = errInteger + 1;
                                        handlerows.add(resultList);
                                        continue;
                                    } else {
                                        code = d.getId();
                                    }
                                }
                                String theRoleIds = "";
                                if (StrUtil.isNotBlank(roleName)) {
                                    roleName.replaceAll("，", ",");
                                    String[] des = roleName.split(",|，");
                                    List<String> tempNames = Arrays.asList(des);
                                    String roleIds = "";
                                    boolean checkRule = false;
                                    for (String tempName : tempNames) {
                                        tempName = StrUtil.trim(tempName);
                                        if (!roleNames.contains(tempName)) {
                                            checkRule = true;
                                            resultList.add(tempName + " 角色不存在！");
                                            break;
                                        } else {
                                            RoleStaff tempRole = roles.get(tempName);
                                            if(tempRole != null){
                                                roleIds += tempRole.getId() + ",";
                                            }
                                        }
                                    }
                                    if(checkRule){
                                        resultList.add("");
                                        errInteger = errInteger + 1;
                                        handlerows.add(resultList);
                                        continue;
                                    }
                                    if(StrUtil.isNotBlank(roleIds)){
                                        theRoleIds = roleIds.substring(0, roleIds.length() - 1);
                                    }
                                }
                                staffNos.add(staffNo);
                                staffNoList.add(staffNo);
                                Staff staf = staffService.findByStaffNo(staffNo);
                                if (null != staf) {
                                    Staff staff = new Staff();
                                    BeanUtils.copyProperties(staf, staff);
                                    if (StrUtil.isNotBlank(name)) {
                                        staff.setName(AESUtil.encrypt(name.trim()));
                                    }
                                    if (StrUtil.isNotBlank(phone)) {
                                        staff.setPhone(AESUtil.encrypt(phone.trim()));
                                    }
                                    if (StrUtil.isNotBlank(code)) {
                                        staff.setDeptNo(code);
                                    }
                                    if (StrUtil.isNotBlank(staffNo)) {
                                        staff.setStaffNo(staffNo.trim());
                                    }
                                    if (StrUtil.isNotBlank(theRoleIds)) {
                                        staff.setRoleIds(theRoleIds);
                                    }
                                    if (StrUtil.isNotBlank(phone)) {
                                        phones.add(phone.trim());
                                        phoneList.add(phone.trim());
                                        List<Staff> sts = staffService.findByPhone(phone);
                                        if (CollectionUtil.isNotEmpty(sts)) {
                                            if (sts.size() > 1) {
                                                resultList.add("手机号" + phone + "已在系统中导入，一个手机号只能绑定一个员工");
                                                resultList.add("");
                                                errInteger = errInteger + 1;
                                                handlerows.add(resultList);
                                                continue;
                                            } else {
                                                if (!staff.getId().equals(sts.get(0).getId())) {
                                                    resultList.add("手机号" + phone + "已在系统中导入，一个手机号只能绑定一个员工");
                                                    resultList.add("");
                                                    errInteger = errInteger + 1;
                                                    handlerows.add(resultList);
                                                    continue;
                                                }
                                            }
                                        }
                                    }
                                    updateStaffs.add(staff);
                                    resultList.add("");
                                    resultList.add("OK");
                                    successInteger = successInteger + 1;
                                } else {
                                    resultList.add("员工号" + staffNo + "未在系统中查询到");
                                    resultList.add("");
                                    errInteger = errInteger + 1;
                                }
                            } else {
                                resultList.add("员工号必须上传");
                                resultList.add("");
                                errInteger = errInteger + 1;
                            }
                            handlerows.add(resultList);
                        } else {
                            //标识已手机号为唯一标识
                            List<String> resultList = new ArrayList<>();
                            String name = "";
                            if (StrUtil.isNotBlank(temp.getColum0())) {
                                name = temp.getColum0();
                            }
                            String phone = "";
                            if (StrUtil.isNotBlank(temp.getColum1())) {
                                phone = temp.getColum1();
                            }
                            String code = "";
                            if (StrUtil.isNotBlank(temp.getColum2())) {
                                code = temp.getColum2();
                            }
                            String staffNo = "";
                            if (StrUtil.isNotBlank(temp.getColum3())) {
                                staffNo = temp.getColum3();
                            }
                            String roleName = "";
                            if (StrUtil.isNotBlank(temp.getColum4())) {
                                roleName = temp.getColum4();
                            }
                            resultList.add(name);
                            resultList.add(phone);
                            resultList.add(code);
                            resultList.add(staffNo);
                            resultList.add(roleName);
                            if (StrUtil.isNotBlank(phone)) {
                                if (StrUtil.isNotBlank(code)) {
                                    Department d = departmentService.findByDeptCode(temp.getColum2());
                                    if (d == null) {
                                        resultList.add(temp.getColum2() + "机构不存在！");
                                        resultList.add("");
                                        errInteger = errInteger + 1;
                                        handlerows.add(resultList);
                                        continue;
                                    } else {
                                        code = d.getId();
                                    }
                                }
                                String theRoleIds = "";
                                if (StrUtil.isNotBlank(roleName)) {
                                    roleName.replaceAll("，", ",");
                                    String[] des = roleName.split(",|，");
                                    List<String> tempNames = Arrays.asList(des);
                                    String roleIds = "";
                                    boolean checkRule =false;
                                    for (String tempName : tempNames) {
                                        tempName = StrUtil.trim(tempName);
                                        if (!roleNames.contains(tempName)) {
                                            resultList.add(tempName + " 角色不存在！");
                                            checkRule =true;
                                            break;
                                        } else {
                                            RoleStaff tempRole = roles.get(tempName);
                                            roleIds += tempRole.getId() + ",";
                                        }
                                    }
                                    if(checkRule){
                                        resultList.add("");
                                        errInteger = errInteger + 1;
                                        handlerows.add(resultList);
                                        continue;
                                    }
                                    if(StrUtil.isNotBlank(roleIds)){
                                        theRoleIds = roleIds.substring(0, roleIds.length() - 1);
                                    }
                                }
                                phones.add(phone.trim());
                                phoneList.add(phone.trim());
                                List<Staff> sts = staffService.findByPhone(phone);
                                if (CollectionUtil.isNotEmpty(sts)) {
                                    if (sts.size() > 1) {
                                        resultList.add("手机号" + phone + "已在系统中导入，一个手机号只能绑定一个员工");
                                        resultList.add("");
                                        errInteger = errInteger + 1;
                                        handlerows.add(resultList);
                                        continue;
                                    } else if (sts.size() == 1) {
                                        Staff staf = sts.get(0);
                                        Staff staff = new Staff();
                                        BeanUtils.copyProperties(staf, staff);
                                        if (StrUtil.isNotBlank(name)) {
                                            staff.setName(AESUtil.encrypt(name.trim()));
                                        }
                                        if (StrUtil.isNotBlank(phone)) {
                                            staff.setPhone(AESUtil.encrypt(phone.trim()));
                                        }
                                        if (StrUtil.isNotBlank(code)) {
                                            staff.setDeptNo(code);
                                        }
                                        if (StrUtil.isNotBlank(staffNo)) {
                                            staff.setStaffNo(staffNo.trim());
                                        }
                                        if (StrUtil.isNotBlank(theRoleIds)) {
                                            staff.setRoleIds(theRoleIds);
                                        }
                                        if (StrUtil.isNotBlank(staffNo)) {
                                            staffNos.add(staffNo);
                                            staffNoList.add(staffNo);
                                            Staff sta = staffService.findByStaffNo(staffNo);
                                            if (null != sta && !sta.getId().equals(staff.getId())) {
                                                resultList.add("员工号" + staffNo + "已在系统中导入，一个员工号只能绑定一个员工");
                                                resultList.add("");
                                                errInteger = errInteger + 1;
                                                handlerows.add(resultList);
                                                continue;
                                            }
                                        }
                                        updateStaffs.add(staff);
                                        resultList.add("");
                                        resultList.add("OK");
                                        successInteger = successInteger + 1;
                                    } else {
                                        resultList.add("手机号" + phone + "未在系统中查询到");
                                        resultList.add("");
                                        errInteger = errInteger + 1;
                                    }
                                } else {
                                    resultList.add("手机号" + phone + "未在系统中查询到");
                                    resultList.add("");
                                    errInteger = errInteger + 1;
                                }
                                handlerows.add(resultList);
                            } else {
                                resultList.add("手机号必填");
                                resultList.add("");
                                errInteger = errInteger + 1;
                                handlerows.add(resultList);
                            }
                        }
                    }
                    allMap.put(key,handlerows);
                });
            }
            pool.shutdown();
            while (!pool.isTerminated()) {
                try {
                    pool.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //没有重复数据
        if (staffNoList.size() == staffNos.size()) {
            if (phoneList.size() == phones.size()) {
                if (CollectionUtil.isNotEmpty(updateStaffs)) {
                    saveBatchOnDuplicateUpdate(updateStaffs, 10000);
                }
            } else {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("excel中存在重复手机号，请检查！");
            }
        } else {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("excel中存在重复员工号，请检查！");
        }
        //同步到活动平台
        List<Department> departments = departmentService.findAll();
        Vector<StaffRole> StaffRoles = new Vector<>();
        ThreadPoolExecutor pool1 = ThreadPoolUtil.createPool(ThreadPoolType.handleAccount);
        for (Staff staff:updateStaffs) {
            pool1.execute(() -> {
                String theRoleIds =  staff.getRoleIds();
                if(StrUtil.isNotBlank(theRoleIds)){
                    String[] croles = theRoleIds.split(",");
                    for(String roleId: croles){
                        StaffRole staffRole = staffRoleService.findByRoleIdAndStaffId(roleId,staff.getId());
                        if(null == staffRole){
                            StaffRole newStaffRole = new StaffRole();
                            newStaffRole.setRoleId(roleId);
                            newStaffRole.setStaffId(staff.getId());
                            StaffRoles.add(newStaffRole);
                        }
                    }
                }
            });
        }
        pool1.shutdown();
        while (!pool1.isTerminated()) {
            try {
                pool1.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Staff staff:updateStaffs) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
            modifyStaffVo.setType("updateImg");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }
        if(CollectionUtil.isNotEmpty(StaffRoles)){
            iStaffRoleService.saveOrUpdateBatch(StaffRoles,1000);
        }
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String path = jarFile.getParentFile().getPath();
        String rootPath = path + File.separator;
        File dir = new File(rootPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Set<String> keyAll = allMap.keySet();
        Set<Integer> ListKey = new TreeSet<>();
        for(String key: keyAll){
            ListKey.add(Integer.parseInt(key));
        }
        for(Integer key: ListKey){
            rows.addAll(allMap.get(key+""));
        }
        String excelFileName = File.separator + "static" + File.separator + "ytdexports" + File.separator +
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
        String contentType = file.getContentType();
        Map<String, Object> map = new HashMap<>(16);
        Result<Object> result = fileService.upload(filee, contentType);
        if (result.isSuccess()) {
            System.out.println(result.getResult().toString());
            map.put("url", result.getResult().toString());
        } else {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("数据导出异常");
        }
        rows.clear();
        map.put("successInteger", successInteger);
        map.put("errInteger", errInteger);
        map.put("updateInteger", updateStaffs.size());
        filee.delete();
        return new ResultUtil<Map<String, Object>>().setData(map);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "保存数据")
    @SystemLog(description = "保存数据")
    @Override
    public Result<Staff> create(@ModelAttribute Staff entity) {
        String phone = entity.getPhone();
        Staff staff = staffService.findByStaffNo(entity.getStaffNo());
        if (staff != null) {
            return new ResultUtil<Staff>().setErrorMsg("工号已被占用！");
        }
        if (StrUtil.isNotEmpty(entity.getPhone())) {
            entity.setPhone(AESUtil.encrypt(entity.getPhone()));
        }
        if (StrUtil.isNotBlank(entity.getPhone())) {
            List<Staff> staff1 = staffService.findByPhone(phone);
            if (staff1 != null && staff1.size() > 0) {
                return new ResultUtil<Staff>().setErrorMsg("手机号已被占用！");
            }
        }
        if (StrUtil.isNotEmpty(entity.getName())) {
            if (entity.getName().trim().length() > 10) {
                return new ResultUtil<Staff>().setErrorMsg("员工姓名长度不能超过10个长度");
            }
            entity.setName(AESUtil.encrypt(entity.getName()));
        }
        String roleIds = entity.getRoleIds();
        if (StrUtil.isNotBlank(roleIds)) {
            Boolean check = false;
            List<RoleStaff> roleStaffs = roleStaffService.findByName("自助表单用户");
            if (CollectionUtil.isNotEmpty(roleStaffs)) {
                String matrixRoleName = "自助表单用户";
                TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName(matrixRoleName);
                Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                //判断设置的自定义表单角色员工数量是否达到上线
                if ( null != selfCustomFormRole ) {
                    if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                        //查询已经绑定该角色的员工数量
                        RoleStaff roleStaff = roles.get("自助表单用户");
                        if(null != roleStaff){
                            List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                            if (CollectionUtil.isNotEmpty(staffRoles)){
                                if( staffRoles.size() >= selfCustomFormRole.getLimitSize()){
                                    return new ResultUtil<Staff>().setErrorMsg("自助表单用户角色最多只允许添加："+selfCustomFormRole.getLimitSize()+ "人，系统人数为："+ staffRoles.size()+"人，请检查" );
                                }
                            }
                        }
                    }
                }
            }
            //判断设置的自定义表单角色员工数量是否达到上线
            List<RoleStaff> businessRoleStaffs = roleStaffService.findByName("业务经理");
            if(CollectionUtil.isNotEmpty(businessRoleStaffs) && roleIds.contains(businessRoleStaffs.get(0).getId())){
                TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName("业务经理");
                Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                if ( null != selfCustomFormRole ) {
                    if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                        //查询已经绑定该角色的员工数量
                        RoleStaff roleStaff = roles.get("业务经理");
                        if(null != roleStaff){
                            List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                            if (CollectionUtil.isNotEmpty(staffRoles)){
                                if( staffRoles.size() >= selfCustomFormRole.getLimitSize()){
                                    return new ResultUtil<Staff>().setErrorMsg("业务经理角色最多只允许添加："+selfCustomFormRole.getLimitSize()+ "人，系统人数为："+ staffRoles.size()+"人，请检查" );
                                }
                            }
                        }
                    }
                }
            }

        }
        Staff e = getService().save(entity);
        //同步到活动平台
        List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(e.getId(),departments);
        modifyStaffVo.setType("add");
        activityApiUtil.modifyStaff(modifyStaffVo);


        return new ResultUtil<Staff>().setData(e);
    }

    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "更新数据")
    @Override
    public Result<Staff> update(@ModelAttribute Staff entity) {
        String phone = entity.getPhone();
        Staff staff = staffService.findByStaffNo(entity.getStaffNo());
        if (null != staff) {
            if (!entity.getId().equals(staff.getId())) {
                if (staff != null) {
                    return new ResultUtil<Staff>().setErrorMsg("工号已被占用！");
                }
            }
        }

        if (StrUtil.isNotEmpty(entity.getPhone())) {
            entity.setPhone(AESUtil.encrypt(entity.getPhone()));
        }
        if (StrUtil.isNotBlank(entity.getPhone())) {
            List<Staff> staff1 = staffService.findByPhone(phone);
            if (staff1 != null && staff1.size() > 0) {
                for (Staff staff2 : staff1) {
                    if (!staff2.getId().equals(entity.getId())) {
                        return new ResultUtil<Staff>().setErrorMsg("手机号已被占用！");
                    }
                }
            }
        }
        if (StrUtil.isNotEmpty(entity.getName())) {
            if (entity.getName().trim().length() > 10) {
                return new ResultUtil<Staff>().setErrorMsg("员工姓名长度不能超过10个长度");
            }
            entity.setName(AESUtil.encrypt(entity.getName()));
        }
        String roleIds = entity.getRoleIds();
        if (StrUtil.isNotBlank(roleIds)) {
            Boolean check = false;
            List<RoleStaff> roleStaffs = roleStaffService.findByName("自助表单用户");
            if (CollectionUtil.isNotEmpty(roleStaffs)) {
                if (roleIds.contains(roleStaffs.get(0).getId())) {
                    StaffRole staffRole = iStaffRoleService.findRoleByRoleIdAndStaffId(roleStaffs.get(0).getId(),entity.getId());
                    if ( null != staffRole ) {
                        check = true;
                    }
                }
                if (check) {
                    String matrixRoleName = "自助表单用户";
                    TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName(matrixRoleName);
                    Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                    //判断设置的自定义表单角色员工数量是否达到上线
                    if ( null != selfCustomFormRole ) {
                        if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                            //查询已经绑定该角色的员工数量
                            RoleStaff roleStaff = roles.get("自助表单用户");
                            if(null != roleStaff){
                                List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                                if (CollectionUtil.isNotEmpty(staffRoles)){
                                    if( staffRoles.size() >= selfCustomFormRole.getLimitSize()){
                                        return new ResultUtil<Staff>().setErrorMsg("自助表单用户角色最多只允许添加："+selfCustomFormRole.getLimitSize()+ "人，系统人数为："+ staffRoles.size()+"人，请检查" );
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //判断设置的自定义表单角色员工数量是否达到上线
            List<RoleStaff> businessRoleStaffs = roleStaffService.findByName("业务经理");
            if (CollectionUtil.isNotEmpty(businessRoleStaffs) && roleIds.contains(businessRoleStaffs.get(0).getId())) {
                StaffRole staffRole = iStaffRoleService.findRoleByRoleIdAndStaffId(businessRoleStaffs.get(0).getId(), entity.getId());
                if (null == staffRole) {
                    TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName("业务经理");
                    Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                    if (null != selfCustomFormRole) {
                        if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0) {
                            //查询已经绑定该角色的员工数量
                            RoleStaff roleStaff = roles.get("业务经理");
                            if (null != roleStaff) {
                                List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                                if (CollectionUtil.isNotEmpty(staffRoles)) {
                                    if (staffRoles.size() >= selfCustomFormRole.getLimitSize()) {
                                        return new ResultUtil<Staff>().setErrorMsg("业务经理角色最多只允许添加：" + selfCustomFormRole.getLimitSize() + "人，系统人数为：" + staffRoles.size() + "人，请检查");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Staff e = getService().update(entity);
        //同步到活动平台
        List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
        modifyStaffVo.setType("Modify");
        activityApiUtil.modifyStaff(modifyStaffVo);
        return new ResultUtil<Staff>().setData(e);
    }

    @RequestMapping(value = "/disable/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "后台禁用员工")
    @SystemLog(description = "后台禁用员工")
    public Result<Object> disable(@ApiParam("id") @PathVariable String id) {

        Staff staff = staffService.get(id);
        if (staff == null) {
            return new ResultUtil<Object>().setErrorMsg("通过id获取员工信息失败");
        }
        staff.setStatus(CommonConstant.USER_STATUS_LOCK);
        if (StrUtil.isNotEmpty(staff.getName())) {
            staff.setName(AESUtil.encrypt(staff.getName()));
        }
        if (StrUtil.isNotEmpty(staff.getPhone())) {
            staff.setPhone(AESUtil.encrypt(staff.getPhone()));
        }
        staffService.update(staff);
        staffService.removeFromCache(staff.getAccountId());
        //同步到活动平台
       /* List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
        modifyStaffVo.setType("NOLOG");
        activityApiUtil.modifyStaff(modifyStaffVo);*/
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/enable/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "后台启用员工")
    @SystemLog(description = "后台启用员工")
    public Result<Object> enable(@ApiParam("id") @PathVariable String id) {

        Staff staff = staffService.get(id);
        if (staff == null) {
            return new ResultUtil<Object>().setErrorMsg("通过id获取员工信息失败");
        }
        staff.setStatus(CommonConstant.USER_STATUS_NORMAL);
        if (StrUtil.isNotEmpty(staff.getName())) {
            staff.setName(AESUtil.encrypt(staff.getName()));
        }
        if (StrUtil.isNotEmpty(staff.getPhone())) {
            staff.setPhone(AESUtil.encrypt(staff.getPhone()));
        }
        staffService.update(staff);
        staffService.addToCache(staff.getAccountId());
        //同步到活动平台
        /*List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
        modifyStaffVo.setType("NOLOG");
        activityApiUtil.modifyStaff(modifyStaffVo);*/
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<Staff>> listByCondition(@ModelAttribute Staff staff,
                                               @ModelAttribute SearchVo searchVo,
                                               @ModelAttribute PageVo pageVo) {
        Map<String,Object> map = new HashMap<>();
        String appid = UserContext.getAppid();
        staff.setAppid(appid);
//        Page<Staff> page = staffService.findByCondition(staff, searchVo, PageUtil.initPage(pageVo));
        StaffSearchVo staffSearchVo = new StaffSearchVo();
        if (StrUtil.isNotBlank(staff.getName())) {
//            staffSearchVo.setName(AESUtil.encrypt(staff.getName().trim()));
            map.put("name",AESUtil.encrypt(staff.getName().trim()));
        }
        if (StrUtil.isNotBlank(staff.getPhone())) {
//            staffSearchVo.setPhone(AESUtil.encrypt(staff.getPhone().trim()));
            map.put("phone",AESUtil.encrypt(staff.getPhone().trim()));
        }
        //绑定账户id
        if (StrUtil.isNotBlank(staff.getAccountId())) {
            ActAccount actAccount = actAccountService.findByActAccountId(staff.getAccountId());
            if (actAccount != null) {
//                staffSearchVo.setAccountId(StrUtil.trim(actAccount.getCoreAccountId()));
                map.put("accountId",StrUtil.trim(actAccount.getCoreAccountId()));
            } else {
//                staffSearchVo.setAccountId(StrUtil.trim(staff.getAccountId()));
                map.put("accountId",StrUtil.trim(staff.getAccountId()));
            }
        }
        if (StrUtil.isNotBlank(staff.getIsBind())) {
//            staffSearchVo.setIsBind(staff.getIsBind());
            map.put("isBind",StrUtil.trim(staff.getIsBind()));
        }
//        staffSearchVo.setDeptNo(staff.getDeptNo());
//        staffSearchVo.setStaffNo(staff.getStaffNo());
//        staffSearchVo.setAppid(appid);
        map.put("appid",appid);
        map.put("staffNo",StrUtil.trim(staff.getStaffNo()));
        map.put("deptNo",StrUtil.trim(staff.getDeptNo()));
        String sort = pageVo.getSort();
        if(StrUtil.equalsIgnoreCase("createTime",sort)){
            pageVo.setSort("create_time");
        }
        Integer totalElements = iStaffService.countByMap(map);
        map.put("page",(pageVo.getPageNumber()-1)*pageVo.getPageSize());
        map.put("pageSize",pageVo.getPageSize());
        List<Staff> staffs = iStaffService.findByMap(map);
//        IPage<Staff> result= iStaffService.listInfoForHelper(PageUtil.initMpPage(pageVo),staffSearchVo);
        Page<Staff> page = new PageImpl<Staff>(staffs,PageUtil.initPage(pageVo),totalElements);
        if (page.getTotalElements() > 0) {
            List<String> deptIds = new ArrayList<>();
            Map<String, Department> departMap = new HashMap<>(16);
            for (int i = 0; i < page.getContent().size(); i++) {
                Staff tempStaff = page.getContent().get(i);
                deptIds.add(tempStaff.getDeptNo().trim());
                if (StrUtil.isNotEmpty(tempStaff.getName())) {
                    tempStaff.setName(AESUtil.decrypt(tempStaff.getName()));
                }
                if (StrUtil.isNotEmpty(tempStaff.getPhone())) {
                    tempStaff.setPhone(AESUtil.decrypt(tempStaff.getPhone()));
                }
            }
            List<Department> departments = departmentService.findByIdIn(deptIds);
            for (Department d : departments) {
                departMap.put(d.getId(), d);
            }
            for (int i = 0; i < page.getContent().size(); i++) {
                Department depart = departMap.get(page.getContent().get(i).getDeptNo().trim());
                if (depart != null) {
                    page.getContent().get(i).setTitle(depart.getTitle());
                    page.getContent().get(i).setDeptNumber(depart.getDeptCode());
                    //page.getContent().get(i).setDeptNo(depart.getDeptCode());

                } else {
                    page.getContent().get(i).setTitle("");
                    page.getContent().get(i).setDeptNo("");
                }
            }
            for (int i = 0; i < page.getContent().size(); i++) {
                List<StaffRole> roles = iStaffRoleService.findByStaffId(page.getContent().get(i).getId());
                page.getContent().get(i).setRoles(roles);
            }
        }
        for (Staff item : page.getContent()) {
            item.setTmname(PrivacyUtil.nameEncrypt(item.getName()));
            item.setTitle(PrivacyUtil.formatToMask(item.getTitle()));
            item.setTmstaffNo(PrivacyUtil.formatToMask(item.getStaffNo()));

        }
        return new ResultUtil<Page<Staff>>().setData(page);
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量删除")
    @Override
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids) {
        for (String id : ids) {
            Staff staff = getService().get(id);
            if (StrUtil.isNotBlank(staff.getAccountId())) {
                return new ResultUtil<Object>().setErrorMsg(staff.getName() + "已被绑定不能删除");
            }
        }
        getService().delete(ids);
        //同步到活动平台
        List<Department> departments = departmentService.findAll();
        for (String id:ids) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(id,departments);
            modifyStaffVo.setType("delete");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }
        return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
    }

    @ResponseBody
    @RequestMapping(value = "/importData1")
    @ApiOperation(value = "1.0员工信息迁移")
    @SystemLog(description = "1.0员工信息迁移")
    public Result<Object> importExcel1(@RequestParam(value = "file", required = true) MultipartFile file,
                                       HttpServletRequest request, HttpSession session) {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        /*1.读Excel, 检查是否存在
          2.数据处理（判定这个名单是否含高级校验），如果含高级校验组装扩展表的数据
          2.批量插入
         */
        if (file == null) {
            return new ResultUtil<Object>().setErrorMsg("未能读取到文件");
        }
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (!StringUtils.equalsAnyIgnoreCase(suffix, ".xlsx") && !StringUtils.equalsAnyIgnoreCase(suffix, ".xls")) {
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        // 导入文件地址
        String filePath = ReadExcelUtil.FilePath(file, request);
//        // excel读取方法
//        StaffController.ExcelListener excelListener = new StaffController.ExcelListener();
//        try {
//            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
//            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
//        } catch (FileNotFoundException e) {
//            return new ResultUtil<Object>().setErrorMsg("文件读取异常！");
//        }
//        List<ExcelRow> excelRows = excelListener.getDatas();

        Set<String> roleNames = roleStaffService.getNameMap().keySet();
        Map<String, RoleStaff> roles = roleStaffService.getNameMap();

        Vector<List<String>> rows = new Vector<>();

        List<ExcelRow> excelRows = ReadExcelUtil.readExcel(filePath, ExcelRow.class);
        if (excelRows != null && excelRows.size() > 0) {
            // 校验excel头部
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Object>().setErrorMsg("没有头列导入模板错误！");
            }
            if (!"员工姓名".equals(excelHead.getColum0())) {
                return new ResultUtil<Object>().setErrorMsg("未包含员工姓名列,导入模板错误！");
            }
            if (!"手机号".equals(excelHead.getColum1())) {
                return new ResultUtil<Object>().setErrorMsg("未包含手机号列,导入模板错误！");
            }
            if (!"工号".equals(excelHead.getColum2())) {
                return new ResultUtil<Object>().setErrorMsg("未包含工号列,导入模板错误！");
            }
            if (!"身份证号".equals(excelHead.getColum3())) {
                return new ResultUtil<Object>().setErrorMsg("未包含身份证号列,导入模板错误！");
            }
            if (!"是否可用".equals(excelHead.getColum4())) {
                return new ResultUtil<Object>().setErrorMsg("未包含是否可用列,导入模板错误！");
            }
            if (!"机构编码".equals(excelHead.getColum5())) {
                return new ResultUtil<Object>().setErrorMsg("未包含机构编码列,导入模板错误！");
            }
            if (!"员工角色".equals(excelHead.getColum6())) {
                return new ResultUtil<Object>().setErrorMsg("未包含员工角色列,导入模板错误！");
            }

            excelRows.remove(0);
            Vector<Staff> saveOrUpdateStaffs = new Vector<>();
            List<Account> saveOrUpdateAccounts = new ArrayList<>();
            List<String> errHeand = new ArrayList<>();
            List<String> roleErrHeand = new ArrayList<>();
            ThreadPoolExecutor pool = ThreadPoolUtil.createPool(ThreadPoolType.handleAccount);
            for (ExcelRow temp : excelRows) {
                pool.execute(() -> {
                    if (StrUtil.isBlank(temp.getColum6())) {
                        roleErrHeand.add(temp.getColum0());
                    } else {
                        String theRoleIds = "";
                        String roleName = temp.getColum6();
                        if (StrUtil.isNotBlank(roleName)) {
                            roleName.replaceAll("，", ",");
                            List<String> tempNames = Arrays.asList(StrUtil.split(roleName, ","));
                            String roleIds = "";
                            for (String tempName : tempNames) {
                                tempName = StrUtil.trim(tempName);
                                if (!roleNames.contains(tempName)) {
                                    roleErrHeand.add(temp.getColum0());
                                } else {
                                    RoleStaff tempRole = roles.get(tempName);
                                    roleIds += tempRole.getId() + ",";
                                }
                            }
                            if(StrUtil.isNotBlank(roleIds)){
                                theRoleIds = roleIds.substring(0, roleIds.length() - 1);
                            }
                        }
                        if (StrUtil.isNotBlank(temp.getColum2()) && StrUtil.isNotBlank(temp.getColum1())) {
                            String staffNo = temp.getColum2();
                            Staff oldStaff = staffService.findByStaffNo(staffNo);
                            if (null == oldStaff) {
                                Staff staff = new Staff();
                                staff.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()));
                                String name = temp.getColum0().trim();
                                String phone = temp.getColum1().trim();

                                if (StrUtil.isNotBlank(name)) {
                                    staff.setName(AESUtil.encrypt(name));
                                } else {
                                    staff.setName("");
                                }
                                if (StrUtil.isNotBlank(phone)) {
                                    staff.setPhone(AESUtil.encrypt(phone));
                                } else {
                                    staff.setPhone("");
                                }
                                staff.setStaffNo(staffNo);
                                staff.setRoleIds(theRoleIds);
                                String orgNo = temp.getColum5();//机构编码
                                if (StrUtil.isNotBlank(orgNo)) {
                                    Department department = departmentService.findByDeptCode(orgNo);
                                    if (null != department) {
                                        staff.setDeptNo(department.getId());
                                    } else {
                                        staff.setDeptNo("");
                                    }
                                } else {
                                    staff.setDeptNo("");
                                }

                                if ("Y".equals(temp.getColum4())) {
                                    staff.setStatus(0);
                                } else {
                                    staff.setStatus(-1);
                                }
                                if (StrUtil.isNotBlank(phone)) {
                                    List<Account> accounts = accountService.findByAppidAndPhone(UserContext.getAppid(), AESUtil.encrypt(phone));
                                    if (CollectionUtil.isNotEmpty(accounts)) {
                                        Account account = accountService.get(accounts.get(0).getId());
                                        staff.setAccountId(account.getId());
                                        Integer isStaff = 1;
                                        account.setIsStaff(isStaff);
                                        account.setStaffNo(staff.getStaffNo());
                                        if (StrUtil.isNotBlank(temp.getColum3())) {
                                            account.setIdcardNo(temp.getColum3().trim());
                                        }
                                        saveOrUpdateAccounts.add(account);
                                    } else {
                                        staff.setAccountId("");
                                    }
                                }
                                saveOrUpdateStaffs.add(staff);
                            }
                        } else {
                            errHeand.add(temp.getColum0());
                        }
                    }
                });
            }
            pool.shutdown();
            while (!pool.isTerminated()) {
                try {
                    pool.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (roleErrHeand.size() > 0) {
                return new ResultUtil<Object>().setErrorMsg("excel员工角色错误，请检查员工角色是否填写正确，（注：不能为空）");
            }
            if (CollectionUtil.isNotEmpty(saveOrUpdateStaffs)) {
                List<Staff> distinctByKeys = deduplication(saveOrUpdateStaffs);
                if (distinctByKeys.size() != saveOrUpdateStaffs.size()) {
                    return new ResultUtil<Object>().setErrorMsg("excel中员工号数据重复");
                }
                staffService.saveOrUpdateAll(saveOrUpdateStaffs);
                if (CollectionUtil.isNotEmpty(saveOrUpdateAccounts)) {
                    accountService.saveOrUpdateAll(saveOrUpdateAccounts);
                }
            } else {
                return new ResultUtil<Object>().setErrorMsg("excel中未查询出可导入数据信息");
            }
            //同步到活动平台
            List<Department> departments = departmentService.findAll();
            for (Staff staff:saveOrUpdateStaffs) {
                ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
                modifyStaffVo.setType("Modify");
                activityApiUtil.modifyStaff(modifyStaffVo);
            }
        }
        return new ResultUtil<Object>().setSuccessMsg("导入成功");
    }

    /**
     * 数据去重
     *
     * @param all
     * @return
     */
    public List<Staff> deduplication(List<Staff> all) {
        return all.parallelStream().distinct()
                .filter(distinctByKey(b -> b.getStaffNo()))
                .collect(toList());
    }

    @ResponseBody
    @RequestMapping(value = "/importData")
    @ApiOperation(value = "导入员工信息")
    @SystemLog(description = "导入员工信息")
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file, HttpServletRequest request, HttpSession session) {

        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }

        long startTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
        String message = "";
//        InputStream is = null;
//        int count = 0;
        List<String> errors = new ArrayList<>();
//        List<String> reasons = new ArrayList<>();
//        StringBuffer repeatStaffNo = new StringBuffer();
//        StringBuffer repeatphone = new StringBuffer();
//        List<Staff> staffsList = new ArrayList<>(); //去重存放的list
//        Date date = new Date();
        String appid = UserContext.getAppid();
        String fileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (".csv.xlsx".indexOf(suffix) == -1) {
            return new ResultUtil<Object>().setErrorMsg("文件类型有误, 请上传.csv或者.xlsx格式的文件");
        }
        String filePath = ReadExcelUtil.FilePath(file, request);
//        // excel读取方法
//        ExcelListener excelListener = new ExcelListener();
//        try {
//            is = new BufferedInputStream(new FileInputStream(filePath));
//            EasyExcelFactory.readBySax(is, new Sheet(1, 0, ExcelRow.class), excelListener);
//        } catch (FileNotFoundException e) {
//            return new ResultUtil<Object>().setErrorMsg("文件读取异常！");
//        }
//        List<ExcelRow> excelRows = excelListener.getDatas();
        List<ExcelRow> excelRows = ReadExcelUtil.readExcel(filePath, ExcelRow.class);
//        List<String> deptNos = new ArrayList<>();
        List<Staff> all = new ArrayList<Staff>();
        Set<String> phones = new HashSet<>();
        List<String> phoneList = new ArrayList<>();
        Set<String> staffNos = new HashSet<>();
        List<String> staffNoList = new ArrayList<>();
        if (excelRows != null && excelRows.size() > 0) {
            // 校验excel头部
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Object>().setErrorMsg("没有头列导入模板错误！");
            }
            if (!"姓名".equals(excelHead.getColum0())) {
                return new ResultUtil<Object>().setErrorMsg("未包含员工姓名列,导入模板错误！");
            }
            if (!"手机号".equals(excelHead.getColum1())) {
                return new ResultUtil<Object>().setErrorMsg("未包含手机号列,导入模板错误！");
            }
            if (!"机构编码".equals(excelHead.getColum2())) {
                return new ResultUtil<Object>().setErrorMsg("未包含机构编码列,导入模板错误！");
            }
            if (!"员工号".equals(excelHead.getColum3())) {
                return new ResultUtil<Object>().setErrorMsg("未包含员工号列,导入模板错误！");
            }
            if (!"员工角色".equals(excelHead.getColum4())) {
                return new ResultUtil<Object>().setErrorMsg("未包含员工角色列,导入模板错误！");
            }
        }
        String matrixRoleName = "自助表单用户";
        TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName(matrixRoleName);
        Set<String> roleNames = roleStaffService.getNameMap().keySet();
        Map<String, RoleStaff> roles = roleStaffService.getNameMap();
        Integer selfCustomFormSize = 0;//自助表单用户添加角色
        for (int i = 1; i < excelRows.size(); i++) {
            ExcelRow excelRow = excelRows.get(i);
            Staff s = new Staff();
            s.setAppid(appid);
            if (StrUtil.isNotEmpty(excelRow.getColum0())) {
                s.setName(excelRow.getColum0());
            } else {
                s.setName("");
            }
            s.setPhone(excelRow.getColum1());
            if (StrUtil.isNotEmpty(excelRow.getColum4())) {
                String roleName = excelRow.getColum4();
                roleName = roleName.replaceAll("，", ",");
                List<String> tempNames = Arrays.asList(StrUtil.split(roleName, ","));
                String roleIds = "";
                for (String tempName : tempNames) {
                    tempName = StrUtil.trim(tempName);
                    if (!roleNames.contains(tempName)) {
                        return new ResultUtil<Object>().setErrorMsg(excelRow.getColum2() + "[" + tempName + "]" + "员工角色不存在！");
                    } else {
                        RoleStaff tempRole = roles.get(tempName);
                        if(tempRole != null){
                            roleIds += tempRole.getId() + ",";
                        }
                    }
                    if ("自助表单用户".equals(tempName)) {
                        selfCustomFormSize = selfCustomFormSize +1;
                    }
                }
                if(StrUtil.isNotBlank(roleIds)){
                    String theRoleIds = roleIds.substring(0, roleIds.length() - 1);
                    s.setRoleIds(theRoleIds);
                }

            } else {
                return new ResultUtil<Object>().setErrorMsg(excelRow.getColum2() + "员工角色不能为空！");
            }
            if (StrUtil.isNotEmpty(excelRow.getColum2())) {
                Department d = departmentService.findByDeptCode(excelRow.getColum2());
                if (d == null) {
                    return new ResultUtil<Object>().setErrorMsg(excelRow.getColum2() + "机构不存在！");
                } else {
                    s.setDeptNo(d.getId());
                }
            } else {
                s.setDeptNo("");
            }
            s.setStaffNo(excelRow.getColum3());
            s.setStatus(0);
            s.setCreateTime(new Date());
            s.setUpdateTime(new Date());
            if (StrUtil.isNotBlank(s.getStaffNo())) {
                String staffNo = s.getStaffNo();
                Staff oldStaff = staffService.findByStaffNo(staffNo);
                if (null != oldStaff) {
                    String errMessage = s.getStaffNo() + "的员工号已存在";
                    errors.add(errMessage);
                    continue;
                }
            } else {
                s.setStaffNo("");
            }
            if (StrUtil.isNotBlank(s.getPhone())) {
                List<Staff> sts = staffService.findByPhone(s.getPhone());
                if (CollectionUtil.isNotEmpty(sts)) {
                    String errMessage = s.getPhone() + "的手机号已存在";
                    errors.add(errMessage);
                    continue;
                } else {
                    s.setPhone(AESUtil.encrypt(s.getPhone()));
                }
            } else {
                s.setPhone("");
            }
            if (StrUtil.isNotBlank(s.getName())) {
                if (s.getName().trim().length() > 10) {
                    return new ResultUtil<Object>().setErrorMsg("员工姓名长度不能超过10个长度");
                }
                s.setName(AESUtil.encrypt(s.getName()));
            }
            if (StrUtil.isNotBlank(s.getPhone())) {
                phones.add(excelRow.getColum1().trim());
                phoneList.add(excelRow.getColum1().trim());
            }
            if (StrUtil.isNotBlank(s.getStaffNo())) {
                staffNos.add(excelRow.getColum3().trim());
                staffNoList.add(excelRow.getColum3().trim());
            }
            if (StrUtil.isBlank(s.getPhone()) && StrUtil.isBlank(s.getStaffNo())) {
                return new ResultUtil<Object>().setErrorMsg("员工号和手机号不能都为空，必须有一个不为空");
            }
            all.add(s);
        }
        long stopTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
        long timeSpan = stopTime - startTime;
        //没有重复数据
        if (staffNoList.size() == staffNos.size()) {
            if (phoneList.size() == phones.size()) {
                log.info("staffAll:" + all.size());
                //判断设置的自定义表单角色员工数量是否达到上线
                if ( null != selfCustomFormRole ) {
                    if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                        //查询已经绑定该角色的员工数量
                        RoleStaff roleStaff = roles.get("自助表单用户");
                        if(null != roleStaff){
                            List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                            if(CollectionUtil.isNotEmpty(staffRoles)){
                                int total = selfCustomFormSize +staffRoles.size();
                                if ( total > selfCustomFormRole.getLimitSize()) {
                                    return new ResultUtil<Object>().setErrorMsg("自助表单用户角色最多只允许添加："+selfCustomFormRole.getLimitSize()+ "人，表格中添加的人数为："+ selfCustomFormSize+"人，系统中存在："+ staffRoles.size()+"人，请检查");
                                }
                            }else {
                                if( selfCustomFormSize > selfCustomFormRole.getLimitSize()){
                                    return new ResultUtil<Object>().setErrorMsg("自助表单用户角色最多只允许添加："+selfCustomFormRole.getLimitSize()+ "人，表格中添加的人数为："+ selfCustomFormSize+"人，请检查" );
                                }
                            }
                        }
                    }
                }
                saveBatchOnDuplicateUpdate(all, 10000);
            } else {
                return new ResultUtil<Object>().setErrorMsg("excel中存在重复手机号，请检查！");
            }
        } else {
            return new ResultUtil<Object>().setErrorMsg("excel中存在重复员工号，请检查！");
        }
        if (CollectionUtil.isEmpty(all)) {
            return new ResultUtil<Object>().setErrorMsg("未查询到可导入员工信息");
        }

        //同步到活动平台
        List<Department> departments = departmentService.findAll();
        for (Staff staff:all) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
            modifyStaffVo.setType("Modify");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }

        int successCount = all.size();
        if (CollectionUtil.isEmpty(errors)) {
            message = "导入成功 " + successCount + " 条数据";
        } else {
            message = "导入成功 " + successCount + " 条，失败 " + errors.size() + " 条数据。<br>" +
                    "错误原因 " + errors.toString();
        }
        return new ResultUtil<Object>().setSuccessMsg(message);
    }

    // 私有化导入监听器
//    public class ExcelListener extends AnalysisEventListener<ExcelRow> {
//        private List<ExcelRow> datas = new ArrayList<>();
//
//        @Override
//        public void invoke(ExcelRow excelRow, AnalysisContext analysisContext) {
//            if (excelRow != null) {
//                datas.add(excelRow);
//            }
//        }
//
//        @Override
//        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
//        }
//
//        public List<ExcelRow> getDatas() {
//            return this.datas;
//        }
//    }


    public void saveBatchOnDuplicateUpdate(List<Staff> staffList, int i) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//        Connection conn = DriverManager.getConnection(dbUrl, username, password);
//        conn.setAutoCommit(false);
        //构造预处理statement
//        PreparedStatement pst = conn.prepareStatement("replace into t_staff (id,create_by,create_time,is_deleted,update_by,update_time,account_id,dept_no,`name`,phone,staff_no,`status`,`appid`) values(?,?,?,?,?,?,?,?,?,?,?,?,?)");
//        System.out.println("开始插入数据");
        long startTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
        iStaffService.saveBatchOnDuplicateUpdate(staffList, 10000);
//        int count = 0;
//        for (Staff s : staffList) {
//            count++;
//            pst.setString(1, s.getId());
//            pst.setString(2, s.getCreateBy());
//            pst.setString(3, f.format(new Date()));
//            pst.setBoolean(4, s.getIsDeleted());
//            pst.setString(5, s.getUpdateBy());
//            pst.setString(6, f.format(new Date()));
//            pst.setString(7, s.getAccountId());
//            pst.setString(8, s.getDeptNo());
//            pst.setString(9, s.getName());
//            pst.setString(10,s.getPhone());
//            pst.setString(11,s.getStaffNo());
//            pst.setInt(12,s.getStatus());
//            pst.setString(13,s.getAppid());
//            //将要执行的SQL语句先添加进去，不执行
//            pst.addBatch();
//            //每1000次提交一次
//            if (count % i == 0) {
//                pst.executeBatch();
//                conn.commit();
//                pst.clearBatch();
//            }
//        }
//        if(count % i >0){
//            pst.executeBatch();
//            conn.commit();
//            pst.clearBatch();
//        }
//        pst.close();
//        conn.close();
        long stopTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
        long timeSpan = stopTime - startTime;
        System.out.println("导入时间差为：" + (timeSpan / 1000) + "秒");
    }

    //返回文件地址
//    private String filePath(MultipartFile file, HttpServletRequest request) {
//        String replyPicName = "";
//        String realPath = "";
//        String fileType = "";
//        String message = "";
//        File excelFile = null;
//        InputStream is = null;
//        Date date = new Date();
//        String uploadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(date);
//        try {
//            replyPicName = FilenameUtils.normalize(uploadDate + file.getOriginalFilename());
//            realPath = request.getSession().getServletContext().getRealPath("/excel");
//            is = file.getInputStream();
//            excelFile = new File(realPath, replyPicName);
//            FileUtils.copyInputStreamToFile(is, excelFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return excelFile.getPath();
//    }

    /**
     * 根据String型时间，获取long型时间，单位毫秒
     *
     * @param inVal 时间字符串
     * @return long型时间
     */
    public static long fromDateStringToLong(String inVal) {
        Date date = null;
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
        try {
            date = inputFormat.parse(inVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>(16);
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @RequestMapping(value = "/export")
    @ApiOperation(value = "导出员工信息")
    @SystemLog(description = "导出员工信息")
    public Result<Object> export(@ModelAttribute Staff staff,
                                 @ModelAttribute SearchVo searchVo,
                                 @ModelAttribute PageVo pageVo,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String appid = UserContext.getAppid();
        staff.setAppid(appid);
        List<Staff> list = staffService.findAll(
                new Specification<Staff>() {
                    @Nullable
                    @Override
                    public javax.persistence.criteria.Predicate toPredicate(Root<Staff> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                        Path<Date> createTimeField = root.get("createTime");
                        Path<String> nameField = root.get("name");
                        Path<String> staffNoField = root.get("staffNo");
                        Path<String> phoneField = root.get("phone");
                        Path<String> deptNoField = root.get("deptNo");
                        Path<String> appidField = root.get("appid");
                        Path<String> accountIdField = root.get("accountId");

                        List<javax.persistence.criteria.Predicate> list = new ArrayList<javax.persistence.criteria.Predicate>();

                        list.add(cb.equal(appidField, staff.getAppid()));

                        //创建时间
                        if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                            Date start = DateUtil.parse(searchVo.getStartDate());
                            Date end = DateUtil.parse(searchVo.getEndDate());
                            list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                        }

                        //姓名
                        if (StrUtil.isNotBlank(staff.getName())) {
//                    list.add(cb.equal(nameField ,staff.getName()));
                            list.add(cb.equal(nameField, AESUtil.encrypt(staff.getName().trim())));
                        }

                        //工号
                        if (StrUtil.isNotBlank(staff.getStaffNo())) {
                            list.add(cb.like(staffNoField, "%" + staff.getStaffNo().trim() + "%"));
                        }

                        //手机
                        if (StrUtil.isNotBlank(staff.getPhone())) {
                            list.add(cb.equal(phoneField, AESUtil.encrypt(staff.getPhone().trim())));
                        }

                        //部门号
                        if (StrUtil.isNotBlank(staff.getDeptNo())) {
                            list.add(cb.equal(deptNoField, staff.getDeptNo().trim()));
                        }

                        //绑定账户id
                        if (StrUtil.isNotBlank(staff.getAccountId())) {
                            ActAccount actAccount = actAccountService.findByActAccountId(staff.getAccountId());
                            if (actAccount != null) {
                                list.add(cb.equal(accountIdField, StrUtil.trim(actAccount.getCoreAccountId())));
                            } else {
                                list.add(cb.equal(accountIdField, StrUtil.trim(staff.getAccountId())));
                            }
                        }

                        javax.persistence.criteria.Predicate[] arr = new javax.persistence.criteria.Predicate[list.size()];
                        if (list.size() > 0) {
                            cq.where(list.toArray(arr));
                        }
                        return null;
                    }
                }
        );
        // List<?> row1 = CollUtil.newArrayList("姓名", "手机号", "机构编码", "员工号", "状态","绑定状态","员工角色");
        // List<List<?>> rows = CollUtil.newArrayList();
        List<StaffExport> staffExportList = CollUtil.newArrayList();
        // rows.add(row1);
        for (int i = 0; i < list.size(); i++) {
            StaffExport staffExport = new StaffExport();
            String deptNo = "";
            if (StrUtil.isNotBlank(list.get(i).getDeptNo())) {
                Department ment = departmentService.get(list.get(i).getDeptNo());
                if (ment != null) {
                    deptNo = ment.getDeptCode();
                }
            }
            Staff oldstaff = list.get(i);
            Staff copystaff = new Staff();
            BeanUtils.copyProperties(oldstaff, copystaff);
            if (StrUtil.isNotBlank(copystaff.getName())) {
                // copystaff.setName(AESUtil.decrypt(copystaff.getName()));
                staffExport.setName(AESUtil.decrypt(copystaff.getName()));
            }
//            else {
//                copystaff.setName("");
//            }
            if (StrUtil.isNotBlank(copystaff.getPhone())) {
               //  copystaff.setPhone(AESUtil.decrypt(copystaff.getPhone()));
                staffExport.setPhone(AESUtil.decrypt(copystaff.getPhone()));
            }
//            else {
//                copystaff.setPhone("");
//            }
            List<StaffRole> roles = iStaffRoleService.findByStaffId(oldstaff.getId());
            if(roles.size()>0){
                String roleName = "";
                for(StaffRole roleStaff :roles){
                    roleName += roleStaff.getRoleName()+",";
                }
                if(StrUtil.isNotBlank(roleName)){
                    roleName = roleName.substring(0,roleName.length()-1);
                    // copystaff.setRoleNames(roleName);
                    staffExport.setRoleNames(roleName);
                }
            }
//            else{
//                copystaff.setRoleNames("");
//            }
            staffExport.setDeptNo(deptNo);
            staffExport.setStaffNo(oldstaff.getStaffNo());
            staffExport.setStatus(oldstaff.getStatus() == 0 ? "正常启用" : "禁用");
            staffExport.setBindStatus(StrUtil.isNotBlank(copystaff.getAccountId()) ? "已绑定" : "未绑定");
            staffExportList.add(staffExport);


//            List<?> row = CollUtil.newArrayList(copystaff.getName(), copystaff.getPhone(), deptNo, copystaff.getStaffNo(),
//                    (copystaff.getStatus() == 0 ? "正常启用" : "禁用"),
//                    (StrUtil.isNotBlank(copystaff.getAccountId()) ? "已绑定" : "未绑定"),copystaff.getRoleNames());
//            rows.add(row);
        }
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String path = jarFile.getParentFile().getPath();
        String rootPath = path + File.separator + "static/ytdexports";
        File dir = new File(rootPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String excelFileName = UUID.randomUUID().toString().replace("-", "").toUpperCase() + ".xlsx";
        String fullFileName = rootPath + File.separator + excelFileName;
//        BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
//        // 一次性写出内容，使用默认样式
//        writer.write(rows);
//        // 关闭writer，释放内存
//        writer.close();
        EasyExcel.write(fullFileName,StaffExport.class)
                .inMemory(true) // 注意，此项配置不能少
                .registerWriteHandler(new WaterMarkHandler(WaterExcelUtil.waterRemark()))
                .sheet("sheet1")
                .doWrite(staffExportList);


        File file = new File(fullFileName);
        ServletUtil.write(response, file);
        file.delete();
        return new ResultUtil<Object>().setSuccessMsg("OK");
    }

    @RequestMapping(value = "/untiedPhone/{ids}")
    @ApiOperation(value = "解绑员工绑定状态")
    @SystemLog(description = "解绑员工绑定状态")
    public Result<Object> untiedPhone(@PathVariable String[] ids) {
        List<String> list = new ArrayList<>();
        for (String id : ids) {
            list.add(id);
        }
//        List<Staff> staffs = staffService.listByIds(list);
        List<Staff> staffs = staffService.listByIds(list);
        List<String> accountIds = new ArrayList<>();
        for (Staff s : staffs) {
            if (StrUtil.isNotBlank(s.getAccountId())) {
                accountIds.add(s.getAccountId());
                staffService.removeFromCache(s.getAccountId());
            }
            s.setAccountId("");
        }
        staffService.saveOrUpdateAll(staffs);
        List<Account> accounts = new ArrayList<>();
        //更新Account数据
        if (CollectionUtil.isNotEmpty(accountIds)) {
            for (String accountId : accountIds) {
                Account account = accountService.get(accountId);
                if (null != account) {
                    account.setStaffNo("");
                    account.setIsStaff(0);
                    accounts.add(account);
                }
            }
        }
        accountService.saveOrUpdateAll(accounts);
        //同步到活动平台
       /*
       List<Department> departments = departmentService.findAll();
       for (Staff staff :staffs) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
            modifyStaffVo.setType("Modify");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }*/
        return new ResultUtil<Object>().setSuccessMsg("解绑成功");
    }


    @RequestMapping(value = "/initRole")
    @ApiOperation(value = "初始化员工角色")
    @SystemLog(description = "初始化员工角色")
    public Result<Object> initRole() {
        List<RoleStaff> roles = roleStaffService.findByDefaultRole(true);
        if(roles.size() == 0){
            return new ResultUtil<Object>().setErrorMsg("未设置默认员工角色，请先设置员工角色");
        }
        List<Staff> list = iStaffService.findWithoutRole();
        if(list.size() == 0){
            return new ResultUtil<Object>().setErrorMsg("未找到无角色员工");
        }

        List<StaffRole> staffRoles = new ArrayList<>();
        for (Staff staff : list) {
            for (RoleStaff role : roles) {
                StaffRole staffRole = new StaffRole();
                staffRole.setStaffId(staff.getId());
                staffRole.setRoleId(role.getId());
                staffRoles.add(staffRole);
            }
        }
        staffRoleService.saveOrUpdateAll(staffRoles);
        return new ResultUtil<Object>().setSuccessMsg("初始化成功");
    }

    @RequestMapping(value = "/updateRecommendFlag", method = RequestMethod.POST)
    @ApiOperation(value = "查询业务经理staff")
    public Result<Object> updateRecommendFlag(@RequestBody Staff staff){
        staffService.updateRecommendFlag(staff.getRecommendFlag(),staff.getId());
        return new ResultUtil<Object>().setSuccessMsg("成功");
    }

}
