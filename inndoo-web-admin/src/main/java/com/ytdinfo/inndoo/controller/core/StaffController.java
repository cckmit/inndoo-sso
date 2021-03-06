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
@Api(description = "????????????????????????")
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
     * ??????????????????
     */
    volatile Integer successInteger = 0;
    /**
     * ??????????????????
     */
    volatile Integer errInteger = 0;

    @Autowired
    private RoleStaffService roleStaffService;

    /**
     * ??????????????????code
     */
    private final static String STAFF_BUSINESS_MANAGER = "STAFF_BUSINESS_MANAGER";

    @ResponseBody
    @RequestMapping(value = "/importPicture")
    @ApiOperation(value = "??????????????????????????????????????????")
    @SystemLog(description = "??????????????????????????????????????????")
    public Result<Map<String, Object>> importPicture(@RequestParam(value = "file", required = true) MultipartFile file,
                                                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("???????????????????????????????????????????????????");
        }
        String fileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (".xls.xlsx".indexOf(suffix) == -1) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("??????????????????, ?????????.xls???.xlsx???????????????");
        }
        String filePath = ReadExcelUtil.FilePath(file, request);
        ExcelReader reader = ExcelUtil.getReader(ResourceUtil.getStream(filePath));
        List<List<Object>> allList = reader.read();
        if (allList.size() <= 1) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("???????????????????????????");
        } else {
            List<Object> excelHead = allList.get(0);
            if (excelHead == null) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("?????????????????????????????????");
            }
            if (!"?????????".equals(excelHead.get(0))) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("??????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.get(1))) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"???????????????".equals(excelHead.get(2))) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("???????????????????????????,?????????????????????");
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
        // ??????????????????
        List<Staff> updateList = new ArrayList<>();
        // ????????????????????????
        Vector<List<String>> rows = new Vector<>();
        List<String> rowhead = new ArrayList<>();
        rowhead.add("?????????");
        rowhead.add("????????????");
        rowhead.add("??????");
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
                rowbody.add("???????????????");
                rowbody.add("??????");
                errorInt = errorInt + 1;
                rows.add(rowbody);
                continue;
            }
            if (duplicateStaffNo.contains(staffNo)) {
                rowbody.add("???????????????");
                rowbody.add("??????");
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
                rowbody.add("???????????????");
                rowbody.add("????????????");
                errorInt = errorInt + 1;
            } else {
                rowbody.add("");
                rowbody.add("??????");
                updateList.add(staff);
                successInt = successInt + 1;
            }
            rows.add(rowbody);
        }
        if (updateList.size() > 0) {
            iStaffService.updateImg(updateList);
            //?????????????????????
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
        // ??????????????????????????????????????????
        writer.write(rows);
        // ??????writer???????????????
        writer.close();
        File filee = new File(fullFileName);
        //   ServletUtil.write(response, filee);
        String contentType = file.getContentType();
        Map<String, Object> map = new HashMap<>(16);
        Result<Object> result = fileService.upload(filee, contentType);
        if (result.isSuccess()) {
            map.put("url", result.getResult().toString());
        } else {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("??????????????????");
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
                String key = cAnchor.getRow1() + "-" + cAnchor.getCol1();// ??????-??????
                //?????????????????????
                String ext = pdata.suggestFileExtension();
                //??????????????????
                String picName = uploadDate + "." + ext;
                String type = pdata.getMimeType();
                byte[] data = pdata.getData();//????????????
                int size = data.length;
                String fKey = CommonUtil.renamePic(picName);
                //??????????????????????????????
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
                String key = cAnchor.getRow1() + "-" + cAnchor.getCol1();// ??????-??????
                //?????????????????????
                String ext = pdata.suggestFileExtension();
                //??????????????????
                String picName = uploadDate + "." + ext;
                String type = pdata.getMimeType();
                byte[] data = pdata.getData();//????????????
                int size = data.length;
                String fKey = CommonUtil.renamePic(picName);
                //??????????????????????????????
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
    @ApiOperation(value = "??????????????????")
    @SystemLog(description = "??????????????????")
    public Result<Map<String, Object>> importtUpdateExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                                          @RequestParam String type,
                                                          HttpServletRequest request, HttpSession session, HttpServletResponse response) throws IOException {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("???????????????????????????????????????????????????");
        }
        //?????????????????????????????????
        successInteger = 0;
        errInteger = 0;
//        String appid = UserContext.getAppid();
//        InputStream is = null;
        String fileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (".csv.xlsx".indexOf(suffix) == -1) {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("??????????????????, ?????????.csv??????.xlsx???????????????");
        }
        // excel????????????
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
            // ??????excel??????
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("?????????????????????????????????");
            }
            if (!"??????".equals(excelHead.getColum0())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("??????????????????,?????????????????????");
            }
            if (!"?????????".equals(excelHead.getColum1())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("?????????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum2())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"?????????".equals(excelHead.getColum3())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("?????????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum4())) {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("????????????????????????,?????????????????????");
            }
            List<String> totalList = new ArrayList<>();
            totalList.add("??????");
            totalList.add("?????????");
            totalList.add("????????????");
            totalList.add("?????????");
            totalList.add("????????????");
            totalList.add("????????????");
            totalList.add("??????");
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
                        //?????????????????????????????????
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
                                        resultList.add(temp.getColum2() + "??????????????????");
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
                                    roleName.replaceAll("???", ",");
                                    String[] des = roleName.split(",|???");
                                    List<String> tempNames = Arrays.asList(des);
                                    String roleIds = "";
                                    boolean checkRule = false;
                                    for (String tempName : tempNames) {
                                        tempName = StrUtil.trim(tempName);
                                        if (!roleNames.contains(tempName)) {
                                            checkRule = true;
                                            resultList.add(tempName + " ??????????????????");
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
                                                resultList.add("?????????" + phone + "???????????????????????????????????????????????????????????????");
                                                resultList.add("");
                                                errInteger = errInteger + 1;
                                                handlerows.add(resultList);
                                                continue;
                                            } else {
                                                if (!staff.getId().equals(sts.get(0).getId())) {
                                                    resultList.add("?????????" + phone + "???????????????????????????????????????????????????????????????");
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
                                    resultList.add("?????????" + staffNo + "????????????????????????");
                                    resultList.add("");
                                    errInteger = errInteger + 1;
                                }
                            } else {
                                resultList.add("?????????????????????");
                                resultList.add("");
                                errInteger = errInteger + 1;
                            }
                            handlerows.add(resultList);
                        } else {
                            //?????????????????????????????????
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
                                        resultList.add(temp.getColum2() + "??????????????????");
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
                                    roleName.replaceAll("???", ",");
                                    String[] des = roleName.split(",|???");
                                    List<String> tempNames = Arrays.asList(des);
                                    String roleIds = "";
                                    boolean checkRule =false;
                                    for (String tempName : tempNames) {
                                        tempName = StrUtil.trim(tempName);
                                        if (!roleNames.contains(tempName)) {
                                            resultList.add(tempName + " ??????????????????");
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
                                        resultList.add("?????????" + phone + "???????????????????????????????????????????????????????????????");
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
                                                resultList.add("?????????" + staffNo + "???????????????????????????????????????????????????????????????");
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
                                        resultList.add("?????????" + phone + "????????????????????????");
                                        resultList.add("");
                                        errInteger = errInteger + 1;
                                    }
                                } else {
                                    resultList.add("?????????" + phone + "????????????????????????");
                                    resultList.add("");
                                    errInteger = errInteger + 1;
                                }
                                handlerows.add(resultList);
                            } else {
                                resultList.add("???????????????");
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

        //??????????????????
        if (staffNoList.size() == staffNos.size()) {
            if (phoneList.size() == phones.size()) {
                if (CollectionUtil.isNotEmpty(updateStaffs)) {
                    saveBatchOnDuplicateUpdate(updateStaffs, 10000);
                }
            } else {
                return new ResultUtil<Map<String, Object>>().setErrorMsg("excel???????????????????????????????????????");
            }
        } else {
            return new ResultUtil<Map<String, Object>>().setErrorMsg("excel???????????????????????????????????????");
        }
        //?????????????????????
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
        // ??????????????????????????????????????????
        writer.write(rows);
        // ??????writer???????????????
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
            return new ResultUtil<Map<String, Object>>().setErrorMsg("??????????????????");
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
    @ApiOperation(value = "????????????")
    @SystemLog(description = "????????????")
    @Override
    public Result<Staff> create(@ModelAttribute Staff entity) {
        String phone = entity.getPhone();
        Staff staff = staffService.findByStaffNo(entity.getStaffNo());
        if (staff != null) {
            return new ResultUtil<Staff>().setErrorMsg("?????????????????????");
        }
        if (StrUtil.isNotEmpty(entity.getPhone())) {
            entity.setPhone(AESUtil.encrypt(entity.getPhone()));
        }
        if (StrUtil.isNotBlank(entity.getPhone())) {
            List<Staff> staff1 = staffService.findByPhone(phone);
            if (staff1 != null && staff1.size() > 0) {
                return new ResultUtil<Staff>().setErrorMsg("????????????????????????");
            }
        }
        if (StrUtil.isNotEmpty(entity.getName())) {
            if (entity.getName().trim().length() > 10) {
                return new ResultUtil<Staff>().setErrorMsg("??????????????????????????????10?????????");
            }
            entity.setName(AESUtil.encrypt(entity.getName()));
        }
        String roleIds = entity.getRoleIds();
        if (StrUtil.isNotBlank(roleIds)) {
            Boolean check = false;
            List<RoleStaff> roleStaffs = roleStaffService.findByName("??????????????????");
            if (CollectionUtil.isNotEmpty(roleStaffs)) {
                String matrixRoleName = "??????????????????";
                TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName(matrixRoleName);
                Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                //??????????????????????????????????????????????????????????????????
                if ( null != selfCustomFormRole ) {
                    if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                        //??????????????????????????????????????????
                        RoleStaff roleStaff = roles.get("??????????????????");
                        if(null != roleStaff){
                            List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                            if (CollectionUtil.isNotEmpty(staffRoles)){
                                if( staffRoles.size() >= selfCustomFormRole.getLimitSize()){
                                    return new ResultUtil<Staff>().setErrorMsg("????????????????????????????????????????????????"+selfCustomFormRole.getLimitSize()+ "????????????????????????"+ staffRoles.size()+"???????????????" );
                                }
                            }
                        }
                    }
                }
            }
            //??????????????????????????????????????????????????????????????????
            List<RoleStaff> businessRoleStaffs = roleStaffService.findByName("????????????");
            if(CollectionUtil.isNotEmpty(businessRoleStaffs) && roleIds.contains(businessRoleStaffs.get(0).getId())){
                TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName("????????????");
                Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                if ( null != selfCustomFormRole ) {
                    if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                        //??????????????????????????????????????????
                        RoleStaff roleStaff = roles.get("????????????");
                        if(null != roleStaff){
                            List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                            if (CollectionUtil.isNotEmpty(staffRoles)){
                                if( staffRoles.size() >= selfCustomFormRole.getLimitSize()){
                                    return new ResultUtil<Staff>().setErrorMsg("??????????????????????????????????????????"+selfCustomFormRole.getLimitSize()+ "????????????????????????"+ staffRoles.size()+"???????????????" );
                                }
                            }
                        }
                    }
                }
            }

        }
        Staff e = getService().save(entity);
        //?????????????????????
        List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(e.getId(),departments);
        modifyStaffVo.setType("add");
        activityApiUtil.modifyStaff(modifyStaffVo);


        return new ResultUtil<Staff>().setData(e);
    }

    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(value = "????????????")
    @Override
    public Result<Staff> update(@ModelAttribute Staff entity) {
        String phone = entity.getPhone();
        Staff staff = staffService.findByStaffNo(entity.getStaffNo());
        if (null != staff) {
            if (!entity.getId().equals(staff.getId())) {
                if (staff != null) {
                    return new ResultUtil<Staff>().setErrorMsg("?????????????????????");
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
                        return new ResultUtil<Staff>().setErrorMsg("????????????????????????");
                    }
                }
            }
        }
        if (StrUtil.isNotEmpty(entity.getName())) {
            if (entity.getName().trim().length() > 10) {
                return new ResultUtil<Staff>().setErrorMsg("??????????????????????????????10?????????");
            }
            entity.setName(AESUtil.encrypt(entity.getName()));
        }
        String roleIds = entity.getRoleIds();
        if (StrUtil.isNotBlank(roleIds)) {
            Boolean check = false;
            List<RoleStaff> roleStaffs = roleStaffService.findByName("??????????????????");
            if (CollectionUtil.isNotEmpty(roleStaffs)) {
                if (roleIds.contains(roleStaffs.get(0).getId())) {
                    StaffRole staffRole = iStaffRoleService.findRoleByRoleIdAndStaffId(roleStaffs.get(0).getId(),entity.getId());
                    if ( null != staffRole ) {
                        check = true;
                    }
                }
                if (check) {
                    String matrixRoleName = "??????????????????";
                    TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName(matrixRoleName);
                    Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                    //??????????????????????????????????????????????????????????????????
                    if ( null != selfCustomFormRole ) {
                        if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                            //??????????????????????????????????????????
                            RoleStaff roleStaff = roles.get("??????????????????");
                            if(null != roleStaff){
                                List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                                if (CollectionUtil.isNotEmpty(staffRoles)){
                                    if( staffRoles.size() >= selfCustomFormRole.getLimitSize()){
                                        return new ResultUtil<Staff>().setErrorMsg("????????????????????????????????????????????????"+selfCustomFormRole.getLimitSize()+ "????????????????????????"+ staffRoles.size()+"???????????????" );
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //??????????????????????????????????????????????????????????????????
            List<RoleStaff> businessRoleStaffs = roleStaffService.findByName("????????????");
            if (CollectionUtil.isNotEmpty(businessRoleStaffs) && roleIds.contains(businessRoleStaffs.get(0).getId())) {
                StaffRole staffRole = iStaffRoleService.findRoleByRoleIdAndStaffId(businessRoleStaffs.get(0).getId(), entity.getId());
                if (null == staffRole) {
                    TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName("????????????");
                    Map<String, RoleStaff> roles = roleStaffService.getNameMap();
                    if (null != selfCustomFormRole) {
                        if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0) {
                            //??????????????????????????????????????????
                            RoleStaff roleStaff = roles.get("????????????");
                            if (null != roleStaff) {
                                List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                                if (CollectionUtil.isNotEmpty(staffRoles)) {
                                    if (staffRoles.size() >= selfCustomFormRole.getLimitSize()) {
                                        return new ResultUtil<Staff>().setErrorMsg("??????????????????????????????????????????" + selfCustomFormRole.getLimitSize() + "????????????????????????" + staffRoles.size() + "???????????????");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Staff e = getService().update(entity);
        //?????????????????????
        List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
        modifyStaffVo.setType("Modify");
        activityApiUtil.modifyStaff(modifyStaffVo);
        return new ResultUtil<Staff>().setData(e);
    }

    @RequestMapping(value = "/disable/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "??????????????????")
    @SystemLog(description = "??????????????????")
    public Result<Object> disable(@ApiParam("id") @PathVariable String id) {

        Staff staff = staffService.get(id);
        if (staff == null) {
            return new ResultUtil<Object>().setErrorMsg("??????id????????????????????????");
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
        //?????????????????????
       /* List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
        modifyStaffVo.setType("NOLOG");
        activityApiUtil.modifyStaff(modifyStaffVo);*/
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/enable/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "??????????????????")
    @SystemLog(description = "??????????????????")
    public Result<Object> enable(@ApiParam("id") @PathVariable String id) {

        Staff staff = staffService.get(id);
        if (staff == null) {
            return new ResultUtil<Object>().setErrorMsg("??????id????????????????????????");
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
        //?????????????????????
        /*List<Department> departments = departmentService.findAll();
        ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
        modifyStaffVo.setType("NOLOG");
        activityApiUtil.modifyStaff(modifyStaffVo);*/
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????")
    @SystemLog(description = "?????????????????????")
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
        //????????????id
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
    @ApiOperation(value = "??????id????????????")
    @Override
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids) {
        for (String id : ids) {
            Staff staff = getService().get(id);
            if (StrUtil.isNotBlank(staff.getAccountId())) {
                return new ResultUtil<Object>().setErrorMsg(staff.getName() + "????????????????????????");
            }
        }
        getService().delete(ids);
        //?????????????????????
        List<Department> departments = departmentService.findAll();
        for (String id:ids) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(id,departments);
            modifyStaffVo.setType("delete");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????????????????");
    }

    @ResponseBody
    @RequestMapping(value = "/importData1")
    @ApiOperation(value = "1.0??????????????????")
    @SystemLog(description = "1.0??????????????????")
    public Result<Object> importExcel1(@RequestParam(value = "file", required = true) MultipartFile file,
                                       HttpServletRequest request, HttpSession session) {
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("???????????????????????????????????????????????????");
        }
        /*1.???Excel, ??????????????????
          2.?????????????????????????????????????????????????????????????????????????????????????????????????????????
          2.????????????
         */
        if (file == null) {
            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
        }
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            return new ResultUtil<Object>().setErrorMsg("?????????xlsx???xls??????");
        }
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (!StringUtils.equalsAnyIgnoreCase(suffix, ".xlsx") && !StringUtils.equalsAnyIgnoreCase(suffix, ".xls")) {
            return new ResultUtil<Object>().setErrorMsg("?????????xlsx???xls??????");
        }
        // ??????????????????
        String filePath = ReadExcelUtil.FilePath(file, request);
//        // excel????????????
//        StaffController.ExcelListener excelListener = new StaffController.ExcelListener();
//        try {
//            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
//            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
//        } catch (FileNotFoundException e) {
//            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
//        }
//        List<ExcelRow> excelRows = excelListener.getDatas();

        Set<String> roleNames = roleStaffService.getNameMap().keySet();
        Map<String, RoleStaff> roles = roleStaffService.getNameMap();

        Vector<List<String>> rows = new Vector<>();

        List<ExcelRow> excelRows = ReadExcelUtil.readExcel(filePath, ExcelRow.class);
        if (excelRows != null && excelRows.size() > 0) {
            // ??????excel??????
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum0())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"?????????".equals(excelHead.getColum1())) {
                return new ResultUtil<Object>().setErrorMsg("?????????????????????,?????????????????????");
            }
            if (!"??????".equals(excelHead.getColum2())) {
                return new ResultUtil<Object>().setErrorMsg("??????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum3())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum4())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum5())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum6())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
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
                            roleName.replaceAll("???", ",");
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
                                String orgNo = temp.getColum5();//????????????
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
                return new ResultUtil<Object>().setErrorMsg("excel???????????????????????????????????????????????????????????????????????????????????????");
            }
            if (CollectionUtil.isNotEmpty(saveOrUpdateStaffs)) {
                List<Staff> distinctByKeys = deduplication(saveOrUpdateStaffs);
                if (distinctByKeys.size() != saveOrUpdateStaffs.size()) {
                    return new ResultUtil<Object>().setErrorMsg("excel????????????????????????");
                }
                staffService.saveOrUpdateAll(saveOrUpdateStaffs);
                if (CollectionUtil.isNotEmpty(saveOrUpdateAccounts)) {
                    accountService.saveOrUpdateAll(saveOrUpdateAccounts);
                }
            } else {
                return new ResultUtil<Object>().setErrorMsg("excel????????????????????????????????????");
            }
            //?????????????????????
            List<Department> departments = departmentService.findAll();
            for (Staff staff:saveOrUpdateStaffs) {
                ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
                modifyStaffVo.setType("Modify");
                activityApiUtil.modifyStaff(modifyStaffVo);
            }
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    /**
     * ????????????
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
    @ApiOperation(value = "??????????????????")
    @SystemLog(description = "??????????????????")
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file, HttpServletRequest request, HttpSession session) {

        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("???????????????????????????????????????????????????");
        }

        long startTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
        String message = "";
//        InputStream is = null;
//        int count = 0;
        List<String> errors = new ArrayList<>();
//        List<String> reasons = new ArrayList<>();
//        StringBuffer repeatStaffNo = new StringBuffer();
//        StringBuffer repeatphone = new StringBuffer();
//        List<Staff> staffsList = new ArrayList<>(); //???????????????list
//        Date date = new Date();
        String appid = UserContext.getAppid();
        String fileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        if (".csv.xlsx".indexOf(suffix) == -1) {
            return new ResultUtil<Object>().setErrorMsg("??????????????????, ?????????.csv??????.xlsx???????????????");
        }
        String filePath = ReadExcelUtil.FilePath(file, request);
//        // excel????????????
//        ExcelListener excelListener = new ExcelListener();
//        try {
//            is = new BufferedInputStream(new FileInputStream(filePath));
//            EasyExcelFactory.readBySax(is, new Sheet(1, 0, ExcelRow.class), excelListener);
//        } catch (FileNotFoundException e) {
//            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
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
            // ??????excel??????
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????");
            }
            if (!"??????".equals(excelHead.getColum0())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"?????????".equals(excelHead.getColum1())) {
                return new ResultUtil<Object>().setErrorMsg("?????????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum2())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
            }
            if (!"?????????".equals(excelHead.getColum3())) {
                return new ResultUtil<Object>().setErrorMsg("?????????????????????,?????????????????????");
            }
            if (!"????????????".equals(excelHead.getColum4())) {
                return new ResultUtil<Object>().setErrorMsg("????????????????????????,?????????????????????");
            }
        }
        String matrixRoleName = "??????????????????";
        TenantStaffRoleLimitVo selfCustomFormRole = matrixApiUtil.getTenantStaffRoleLimitByRoleName(matrixRoleName);
        Set<String> roleNames = roleStaffService.getNameMap().keySet();
        Map<String, RoleStaff> roles = roleStaffService.getNameMap();
        Integer selfCustomFormSize = 0;//??????????????????????????????
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
                roleName = roleName.replaceAll("???", ",");
                List<String> tempNames = Arrays.asList(StrUtil.split(roleName, ","));
                String roleIds = "";
                for (String tempName : tempNames) {
                    tempName = StrUtil.trim(tempName);
                    if (!roleNames.contains(tempName)) {
                        return new ResultUtil<Object>().setErrorMsg(excelRow.getColum2() + "[" + tempName + "]" + "????????????????????????");
                    } else {
                        RoleStaff tempRole = roles.get(tempName);
                        if(tempRole != null){
                            roleIds += tempRole.getId() + ",";
                        }
                    }
                    if ("??????????????????".equals(tempName)) {
                        selfCustomFormSize = selfCustomFormSize +1;
                    }
                }
                if(StrUtil.isNotBlank(roleIds)){
                    String theRoleIds = roleIds.substring(0, roleIds.length() - 1);
                    s.setRoleIds(theRoleIds);
                }

            } else {
                return new ResultUtil<Object>().setErrorMsg(excelRow.getColum2() + "???????????????????????????");
            }
            if (StrUtil.isNotEmpty(excelRow.getColum2())) {
                Department d = departmentService.findByDeptCode(excelRow.getColum2());
                if (d == null) {
                    return new ResultUtil<Object>().setErrorMsg(excelRow.getColum2() + "??????????????????");
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
                    String errMessage = s.getStaffNo() + "?????????????????????";
                    errors.add(errMessage);
                    continue;
                }
            } else {
                s.setStaffNo("");
            }
            if (StrUtil.isNotBlank(s.getPhone())) {
                List<Staff> sts = staffService.findByPhone(s.getPhone());
                if (CollectionUtil.isNotEmpty(sts)) {
                    String errMessage = s.getPhone() + "?????????????????????";
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
                    return new ResultUtil<Object>().setErrorMsg("??????????????????????????????10?????????");
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
                return new ResultUtil<Object>().setErrorMsg("???????????????????????????????????????????????????????????????");
            }
            all.add(s);
        }
        long stopTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
        long timeSpan = stopTime - startTime;
        //??????????????????
        if (staffNoList.size() == staffNos.size()) {
            if (phoneList.size() == phones.size()) {
                log.info("staffAll:" + all.size());
                //??????????????????????????????????????????????????????????????????
                if ( null != selfCustomFormRole ) {
                    if (!selfCustomFormRole.getEnableLimit() && null != selfCustomFormRole.getLimitSize() && selfCustomFormRole.getLimitSize() > 0 ) {
                        //??????????????????????????????????????????
                        RoleStaff roleStaff = roles.get("??????????????????");
                        if(null != roleStaff){
                            List<StaffRole> staffRoles = iStaffRoleService.findRoleByRoleId(roleStaff.getId());
                            if(CollectionUtil.isNotEmpty(staffRoles)){
                                int total = selfCustomFormSize +staffRoles.size();
                                if ( total > selfCustomFormRole.getLimitSize()) {
                                    return new ResultUtil<Object>().setErrorMsg("????????????????????????????????????????????????"+selfCustomFormRole.getLimitSize()+ "????????????????????????????????????"+ selfCustomFormSize+"????????????????????????"+ staffRoles.size()+"???????????????");
                                }
                            }else {
                                if( selfCustomFormSize > selfCustomFormRole.getLimitSize()){
                                    return new ResultUtil<Object>().setErrorMsg("????????????????????????????????????????????????"+selfCustomFormRole.getLimitSize()+ "????????????????????????????????????"+ selfCustomFormSize+"???????????????" );
                                }
                            }
                        }
                    }
                }
                saveBatchOnDuplicateUpdate(all, 10000);
            } else {
                return new ResultUtil<Object>().setErrorMsg("excel???????????????????????????????????????");
            }
        } else {
            return new ResultUtil<Object>().setErrorMsg("excel???????????????????????????????????????");
        }
        if (CollectionUtil.isEmpty(all)) {
            return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????");
        }

        //?????????????????????
        List<Department> departments = departmentService.findAll();
        for (Staff staff:all) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
            modifyStaffVo.setType("Modify");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }

        int successCount = all.size();
        if (CollectionUtil.isEmpty(errors)) {
            message = "???????????? " + successCount + " ?????????";
        } else {
            message = "???????????? " + successCount + " ???????????? " + errors.size() + " ????????????<br>" +
                    "???????????? " + errors.toString();
        }
        return new ResultUtil<Object>().setSuccessMsg(message);
    }

    // ????????????????????????
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
        //???????????????statement
//        PreparedStatement pst = conn.prepareStatement("replace into t_staff (id,create_by,create_time,is_deleted,update_by,update_time,account_id,dept_no,`name`,phone,staff_no,`status`,`appid`) values(?,?,?,?,?,?,?,?,?,?,?,?,?)");
//        System.out.println("??????????????????");
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
//            //???????????????SQL?????????????????????????????????
//            pst.addBatch();
//            //???1000???????????????
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
        System.out.println("?????????????????????" + (timeSpan / 1000) + "???");
    }

    //??????????????????
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
     * ??????String??????????????????long????????????????????????
     *
     * @param inVal ???????????????
     * @return long?????????
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
    @ApiOperation(value = "??????????????????")
    @SystemLog(description = "??????????????????")
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

                        //????????????
                        if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                            Date start = DateUtil.parse(searchVo.getStartDate());
                            Date end = DateUtil.parse(searchVo.getEndDate());
                            list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                        }

                        //??????
                        if (StrUtil.isNotBlank(staff.getName())) {
//                    list.add(cb.equal(nameField ,staff.getName()));
                            list.add(cb.equal(nameField, AESUtil.encrypt(staff.getName().trim())));
                        }

                        //??????
                        if (StrUtil.isNotBlank(staff.getStaffNo())) {
                            list.add(cb.like(staffNoField, "%" + staff.getStaffNo().trim() + "%"));
                        }

                        //??????
                        if (StrUtil.isNotBlank(staff.getPhone())) {
                            list.add(cb.equal(phoneField, AESUtil.encrypt(staff.getPhone().trim())));
                        }

                        //?????????
                        if (StrUtil.isNotBlank(staff.getDeptNo())) {
                            list.add(cb.equal(deptNoField, staff.getDeptNo().trim()));
                        }

                        //????????????id
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
        // List<?> row1 = CollUtil.newArrayList("??????", "?????????", "????????????", "?????????", "??????","????????????","????????????");
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
            staffExport.setStatus(oldstaff.getStatus() == 0 ? "????????????" : "??????");
            staffExport.setBindStatus(StrUtil.isNotBlank(copystaff.getAccountId()) ? "?????????" : "?????????");
            staffExportList.add(staffExport);


//            List<?> row = CollUtil.newArrayList(copystaff.getName(), copystaff.getPhone(), deptNo, copystaff.getStaffNo(),
//                    (copystaff.getStatus() == 0 ? "????????????" : "??????"),
//                    (StrUtil.isNotBlank(copystaff.getAccountId()) ? "?????????" : "?????????"),copystaff.getRoleNames());
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
//        // ??????????????????????????????????????????
//        writer.write(rows);
//        // ??????writer???????????????
//        writer.close();
        EasyExcel.write(fullFileName,StaffExport.class)
                .inMemory(true) // ??????????????????????????????
                .registerWriteHandler(new WaterMarkHandler(WaterExcelUtil.waterRemark()))
                .sheet("sheet1")
                .doWrite(staffExportList);


        File file = new File(fullFileName);
        ServletUtil.write(response, file);
        file.delete();
        return new ResultUtil<Object>().setSuccessMsg("OK");
    }

    @RequestMapping(value = "/untiedPhone/{ids}")
    @ApiOperation(value = "????????????????????????")
    @SystemLog(description = "????????????????????????")
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
        //??????Account??????
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
        //?????????????????????
       /*
       List<Department> departments = departmentService.findAll();
       for (Staff staff :staffs) {
            ModifyStaffVo modifyStaffVo = staffServiceImpl.getModifyStaffVo(staff.getId(),departments);
            modifyStaffVo.setType("Modify");
            activityApiUtil.modifyStaff(modifyStaffVo);
        }*/
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }


    @RequestMapping(value = "/initRole")
    @ApiOperation(value = "?????????????????????")
    @SystemLog(description = "?????????????????????")
    public Result<Object> initRole() {
        List<RoleStaff> roles = roleStaffService.findByDefaultRole(true);
        if(roles.size() == 0){
            return new ResultUtil<Object>().setErrorMsg("??????????????????????????????????????????????????????");
        }
        List<Staff> list = iStaffService.findWithoutRole();
        if(list.size() == 0){
            return new ResultUtil<Object>().setErrorMsg("????????????????????????");
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
        return new ResultUtil<Object>().setSuccessMsg("???????????????");
    }

    @RequestMapping(value = "/updateRecommendFlag", method = RequestMethod.POST)
    @ApiOperation(value = "??????????????????staff")
    public Result<Object> updateRecommendFlag(@RequestBody Staff staff){
        staffService.updateRecommendFlag(staff.getRecommendFlag(),staff.getId());
        return new ResultUtil<Object>().setSuccessMsg("??????");
    }

}
