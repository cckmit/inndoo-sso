package com.ytdinfo.inndoo.controller.base.manage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.metadata.Sheet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.utils.HibernateProxyTypeAdapter;
import com.ytdinfo.inndoo.common.utils.ReadExcelUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.utils.excel.WaterExcelUtil;
import com.ytdinfo.inndoo.common.utils.excel.WaterMarkHandler;
import com.ytdinfo.inndoo.common.vo.DepartmentVo;
import com.ytdinfo.inndoo.common.vo.ExcelRow;
import com.ytdinfo.inndoo.common.vo.ModifyDepartmentVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.activiti.service.ActNodeService;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.entity.DepartmentHeader;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.service.DepartmentHeaderService;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import com.ytdinfo.inndoo.modules.base.service.RoleDepartmentService;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import com.ytdinfo.inndoo.modules.core.entity.export.DepartMentExport;
import com.ytdinfo.inndoo.modules.core.entity.export.StaffExport;
import com.ytdinfo.inndoo.modules.core.service.StaffService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "机构管理接口")
@RequestMapping("/base/department")
@CacheConfig(cacheNames = "department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleDepartmentService roleDepartmentService;

    @Autowired
    private DepartmentHeaderService departmentHeaderService;

    @Autowired
    private ActNodeService actNodeService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private StaffService staffService;

    @Autowired
    private ActivityApiUtil activityApiUtil;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取全部数据")
    public Result<List<Department>> listAll() {
    	List<Department> list = new ArrayList<>();
        String key = "department::all";
        String v = redisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(v)){
            list = new Gson().fromJson(v, new TypeToken<List<Department>>(){}.getType());
            return new ResultUtil<List<Department>>().setData(list);
        }
        list =departmentService.findAllToTree();
        Collator collator = Collator.getInstance(Locale.CHINESE);
        list.sort((o1, o2) -> collator.compare(o1.getTitle(), o2.getTitle()));
        redisTemplate.opsForValue().set(key,
                new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(list));
        return new ResultUtil<List<Department>>().setData(list);
    }
    
    @RequestMapping(value = "/listByParentId/{parentId}", method = RequestMethod.GET)
    @ApiOperation(value = "通过parentId获取")
    @SystemLog(description = "查看机构列表")
    public Result<List<Department>> getByParentId(@PathVariable String parentId,
                                                  @ApiParam("是否开始数据权限过滤") @RequestParam(required = false, defaultValue = "true") Boolean openDataFilter){

        List<Department> list = new ArrayList<>();
        User u = securityUtil.getCurrUser();
        String key = "department::"+parentId+":"+u.getId()+"_"+openDataFilter;
        String v = redisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(v)){
            list = new Gson().fromJson(v, new TypeToken<List<Department>>(){}.getType());
            return new ResultUtil<List<Department>>().setData(list);
        }
        list = departmentService.findByParentIdOrderBySortOrder(parentId, openDataFilter);
        list = setInfo(list);
        redisTemplate.opsForValue().set(key,
                new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(list));
        return new ResultUtil<List<Department>>().setData(list);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ApiOperation(value = "添加")
    @SystemLog(description = "添加机构")
    public Result<Object> create(@ModelAttribute Department department){

        String name=department.getParentTitle();
        String code=department.getDeptCode();
        String parentid=department.getParentId();
    	Department a = departmentService.findByDeptCode(department.getDeptCode());
    	if(a != null) {
    		return new ResultUtil<Object>().setErrorMsg("该机构编码已存在！");
    	}
        Department d = departmentService.save(department);
        // 如果不是添加的一级 判断设置上级为父节点标识
        if(!CommonConstant.PARENT_ID.equals(department.getParentId())){
            Department parent = departmentService.get(department.getParentId());
            if(parent.getIsParent()==null||!parent.getIsParent()){
                parent.setIsParent(true);
                departmentService.update(parent);
            }
        }
        // 更新缓存
        Set<String> keys = RedisUtil.keys("department::*");
        redisTemplate.delete(keys);
        String key = "department::all";
        redisTemplate.delete(key);

        //推到活动平台--新增时
        ModifyDepartmentVo modifyDepartmentVo = departmentService.getModiifyDepartment(d.getId());
        modifyDepartmentVo.setType("add");
        activityApiUtil.modiifyDepartment(modifyDepartmentVo);
        return new ResultUtil<Object>().setSuccessMsg("添加成功");
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ApiOperation(value = "编辑")
    @SystemLog(description = "编辑机构")
    public Result<Object> update(@ModelAttribute Department department,
                               @RequestParam(required = false) String[] mainHeader,
                               @RequestParam(required = false) String[] viceHeader){

    	Department a = departmentService.findByDeptCode(department.getDeptCode());
    	if(a != null && !a.getId().equals(department.getId())) {
    		return new ResultUtil<Object>().setErrorMsg("该机构编码已存在！");
    	}
        if(StringUtils.isNotEmpty(department.getParentId()) && StringUtils.equals(department.getParentId(),department.getId())  ){
            return new ResultUtil<Object>().setErrorMsg("机构的上级不能是本机构！");
        }


        Department d = departmentService.update(department);
        // 先删除原数据
        departmentHeaderService.deleteByDepartmentId(department.getId());
        for(String id:mainHeader){
            DepartmentHeader dh = new DepartmentHeader();
            dh.setUserId(id);
            dh.setDepartmentId(d.getId());
            dh.setType(CommonConstant.HEADER_TYPE_MAIN);
            departmentHeaderService.save(dh);
        }
        for(String id:viceHeader){
            DepartmentHeader dh = new DepartmentHeader();
            dh.setUserId(id);
            dh.setDepartmentId(d.getId());
            dh.setType(CommonConstant.HEADER_TYPE_VICE);
            departmentHeaderService.save(dh);

        }
        ModifyDepartmentVo old = departmentService.getModiifyDepartment(department.getId(),department);
        //if(!(a.getTitle().equals(department.getTitle()))||!(a.getDeptCode().equals(department.getDeptCode()))|| !(a.getParentId().equals(department.getParentId()))) {
            old.setType("Modify");
            activityApiUtil.modiifyDepartment(old);
        //}
        // 手动删除所有机构缓存
        Set<String> keys = RedisUtil.keys("department:" + "*");
        redisTemplate.unlink(keys);
        String key = "department::all";
        redisTemplate.unlink(key);

        // 删除所有用户缓存
        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
        redisTemplate.unlink(keysUser);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
        return new ResultUtil<Object>().setSuccessMsg("编辑成功");
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "批量通过id删除")
    @SystemLog(description = "删除机构")
    public Result<Object> batchDelete(@PathVariable String[] ids){

        for(String id:ids){
            List<User> list = userService.findByDepartmentId(id);
            if(list!=null&&list.size()>0){
                return new ResultUtil<Object>().setErrorMsg("机构删除失败，删除机构下用户信息后才能进行删除操作");
            }
            List<Staff> liststaff = staffService.findByDeptNo(id);
            if(liststaff != null && liststaff.size() > 0) {
            	return new ResultUtil<Object>().setErrorMsg("机构删除失败，删除机构下员工信息后才能进行删除操作");
            }
            //查询是否存在子机构
            List<Department> childrenlist = departmentService.findByParentIdAndStatusOrderBySortOrder(id, CommonConstant.STATUS_NORMAL);
            if(childrenlist != null && childrenlist.size() > 0) {
            	return new ResultUtil<Object>().setErrorMsg("存在子机构，先从子机构开始删除");
            }
        }
        for(String id:ids){
            ModifyDepartmentVo modifyDepartmentVo = departmentService.getModiifyDepartment(id);
            departmentService.delete(id);
            // 删除关联数据权限
            roleDepartmentService.deleteByDepartmentId(id);
            // 删除关联机构负责人
            departmentHeaderService.deleteByDepartmentId(id);
            // 删除流程关联节点
            actNodeService.deleteByRelateId(id);
            //推到活动平台--删除时
            modifyDepartmentVo.setType("delete");
            activityApiUtil.modiifyDepartment(modifyDepartmentVo);

        }
        // 删除所有用户缓存
        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
        redisTemplate.delete(keysUser);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
        // 手动删除所有部门缓存
        Set<String> keys = RedisUtil.keys("department:" + "*");
        redisTemplate.unlink(keys);
        String key = "department::all";
        redisTemplate.unlink(key);
        // 删除数据权限缓存
        Set<String> keysUserRole = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userRole.getPrefixKey());
        redisTemplate.unlink(keysUserRole);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userRole.getPrefixKey());
        return new ResultUtil<Object>().setSuccessMsg("批量通过id删除数据成功");
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ApiOperation(value = "机构名和机构编号模糊搜索")
    @SystemLog(description = "机构名和机构编号模糊搜索")
    public Result<List<Department>> searchByTitle(@RequestParam String searchKey,
                                                  @ApiParam("是否开始数据权限过滤") @RequestParam(required = false, defaultValue = "true") Boolean openDataFilter){
        List<Department> list = departmentService.findByTitleLikeOrDeptCodeLikeOrderBySortOrder("%"+searchKey+"%", "%"+searchKey+"%",openDataFilter);
        list = setInfo(list);
        return new ResultUtil<List<Department>>().setData(list);
    }

    public List<Department> setInfo(List<Department> list){
        // lambda表达式
        list.forEach(item -> {
            if(!CommonConstant.PARENT_ID.equals(item.getParentId())){
                Department parent = departmentService.get(item.getParentId());
                item.setParentTitle(parent.getTitle());
            }else{
                item.setParentTitle("一级机构");
            }
            // 设置负责人
            item.setMainHeader(departmentHeaderService.findHeaderByDepartmentId(item.getId(), CommonConstant.HEADER_TYPE_MAIN));
            item.setViceHeader(departmentHeaderService.findHeaderByDepartmentId(item.getId(), CommonConstant.HEADER_TYPE_VICE));
        });
        return list;
    }


    @RequestMapping(value = "/countLevel", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "获取当前账号下机构层级")
    public Result<Integer> countLevel() {
        Integer count =  departmentService.countLevel();
        return new ResultUtil<Integer>().setData(count);
    }


    @RequestMapping(value = "/export")
    @ApiOperation(value = "导出机构信息")
    public void export( String[] ids ,Boolean strict ,HttpServletRequest request,HttpServletResponse response) throws IllegalAccessException, InstantiationException {
        String appid =  UserContext.getAppid();
    	List<String> deptIds= Arrays.asList(ids);
        List<Department> allList = departmentService.findByAppid(appid);
        allList = departmentService.transformToTree(allList,new HashMap<>());
        LinkedList<LinkedList<String>> row =new LinkedList<>();
        if(strict){
            LinkedList<String> repartRow =new LinkedList<>();
            for(String id:ids){
                for(Department department:allList){
                    Department tempNode = departmentService.findNodeByParentid(department,id);
                    if(tempNode!=null){
                        departmentService.tranRow(tempNode,row,repartRow);
                    }
                }
            }
        }else{
//            List<Department> list = departmentService.findByIdIn(deptIds);
//            list = setInfo(list);
//            for(Department temp:list){
//                LinkedList<String> columns =new LinkedList<>();
//                columns.add(temp.getTitle());
//                columns.add(temp.getDeptCode());
//                row.add(columns);
//            }


            LinkedList<String> repartRow =new LinkedList<>();
            for(String id:ids){
                for(Department department:allList){
                    Department tempNode = departmentService.findNodeByParentid(department,id);
                    if(tempNode!=null){
                        LinkedList<Department> plist =tempNode.getParentList();
                        plist.add(tempNode);
                        LinkedList<String> columns =new LinkedList<>();
                        for(Department temp:plist){
                            columns.add(temp.getTitle());
                            columns.add(temp.getDeptCode());
                        }
                        row.add(columns);
                    }
                }
            }

        }
//        for(Map<String,Object> pMap:exportDepartmetnList){
//            if(pMap!=null){
//                if(pMap.containsKey("pdept")){
//                    LinkedList<Department> pdept = (LinkedList<Department>)pMap.get("pdept");
//                }
//            }
//        }

        String[] depts = new String[]{};
        CollUtil.newArrayList(depts);
//    	List<?> row1 = CollUtil.newArrayList("一级机构名称", "一级机构编码", "二级机构名称", "二级机构编码", "三级机构名称", "三级机构编码", "四级机构名称", "四级机构编码", "五级机构名称", "五级机构编码","六级机构名称", "六级机构编码");
//        List<List<?>> rows = CollUtil.newArrayList();
//        rows.add(row1);

        List<DepartMentExport> departMentExportList = CollUtil.newArrayList();

//        for (Department department : list) {
//        	DepartmentVo vo = new DepartmentVo();
//        	//为一级机构时,直接往exportList塞一笔数据
//        	if(CommonConstant.PARENT_ID.equals(department.getParentId())) {
//        		vo.setId(department.getId());
//        		vo.setTitle(department.getTitle());
//        		vo.setDeptCode(department.getDeptCode());
//        		exportList.add(vo);
//        	}else {
//        		if(exportList.size() > 0) {
//        			DepartmentVo dev = exportList.get(exportList.size()-1);
//        			if(dev.getId().equals(department.getParentId())) {
//        				DepartmentVo cdvo = new DepartmentVo();
//        				cdvo.setId(department.getId());
//                		cdvo.setTitle(department.getTitle());
//                		cdvo.setDeptCode(department.getDeptCode());
//        				//exportList.get(exportList.size()-1).setChildren(cdvo);
//        			}
//        		}else {
//        			vo.setId(department.getId());
//        			vo.setTitle(department.getTitle());
//            		vo.setDeptCode(department.getDeptCode());
//            		exportList.add(vo);
//        		}
//        	}
//		}
        Field[] fields = DepartMentExport.class.getDeclaredFields();
        for (int i = 0; i < row.size(); i++) {
            LinkedList<String> tempRow= row.get(i);
            // String[] strings = tempRow.toArray(new String[tempRow.size()]);
            // List<?> temp = CollUtil.newArrayList(strings);
           //  rows.add(temp);
            DepartMentExport departMentExport = new DepartMentExport();
            for (int j = 0; j < tempRow.size(); j++) {
                Field field =  fields[j];
                field.setAccessible(true);
                field.set(departMentExport,tempRow.get(j));
            }
            departMentExportList.add(departMentExport);




        }
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String path = jarFile.getParentFile().getPath();
        String rootPath = path + File.separator + "static/ytdexports";
        File dir = new File(rootPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String excelFileName = "机构信息导出"+ new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
        String fullFileName = rootPath + File.separator + excelFileName;
        //BigExcelWriter writer = ExcelUtil.getBigWriter(fullFileName);
        // 一次性写出内容，使用默认样式
        // writer.write(rows);
        // 关闭writer，释放内存
        //writer.close();

        EasyExcel.write(fullFileName, DepartMentExport.class)
                .inMemory(true) // 注意，此项配置不能少
                .registerWriteHandler(new WaterMarkHandler(WaterExcelUtil.waterRemark()))
                .sheet("sheet1")
                .doWrite(departMentExportList);
        response.setContentType("multipart/form-data");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename="+ URLUtil.encode(excelFileName, StringUtil.UTF8) );
        File file = new File(fullFileName);
        ServletUtil.write(response, file);
        file.delete();
    }
    
    @RequestMapping(value = "/loadDepartmentById/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "通过id获取")
    @SystemLog(description = "查看机构列表")
    public Result<List<Department>> getById(@PathVariable String id){
        List<Department> list = new ArrayList<>();
        Department department = departmentService.get(id);
        list.add(department);
        return new ResultUtil<List<Department>>().setData(list);
    }

    @ResponseBody
    @RequestMapping(value = "/importData")
    @ApiOperation(value = "导入机构信息")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                      HttpServletRequest request, HttpSession session
                                     ) throws SQLException {
        if(file == null){
            return new ResultUtil<Object>().setErrorMsg("未能读取到文件");
        }
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        String fileName=file.getOriginalFilename();
        if(StringUtils.isBlank(fileName) ){
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        String suffix = StringUtils.substring(fileName,StringUtils.lastIndexOf(fileName,"."));
        if(!StringUtils.equalsAnyIgnoreCase(suffix,".xlsx") &&  !StringUtils.equalsAnyIgnoreCase(suffix,".xls") ){
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        // 导入文件地址
        String filePath = ReadExcelUtil.FilePath(file, request);
        ReadExcelUtil.ExcelListener<ExcelRow> excelListener = new ReadExcelUtil.ExcelListener<ExcelRow>();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            return new ResultUtil<Object>().setErrorMsg("文件读取异常！");
        }
        List<ExcelRow> excelRows = excelListener.getDatas();
        if(excelRows !=null && excelRows.size() >0){
            // 校验excel头部
            ExcelRow excelHead = excelRows.get(0);
            if(excelHead == null ){
                return new ResultUtil<Object>().setErrorMsg("导入模板错误！");
            }
            excelRows.remove(0);
            ArrayList<Department> insertDepartment = new ArrayList<>();
            Set<Department> mdPhoneList = new LinkedHashSet<Department>();
            Map<String,Department> set =new HashMap<>();
            List<ModifyDepartmentVo> modifyDepartmentVoList = new ArrayList<>();
            List<ModifyDepartmentVo> addDepartmentVoList = new ArrayList<>();

            Map<String,String> verifyDept = new HashMap<>();
            Map<String,String> deptLevel = new HashMap<>();
            for(ExcelRow temp :excelRows){
                String[] keys = temp.getColums();
                int tempDepts = keys.length/2;
                List<Department> rowDepartments= new ArrayList<>();
                for(int  i = 0 ;i<tempDepts;i++){
                    Department department =new Department();
                    String  title = keys[2*i];
                    String  deptCode = keys[2*i+1];
                    if(StringUtils.isNotBlank(title) || StringUtils.isNotBlank(deptCode)){
                        if(verifyDept.get(deptCode) != null && !StrUtil.equals(verifyDept.get(deptCode) ,title)){
                            return new ResultUtil<Object>().setErrorMsg("机构编码: "+deptCode+" 存在多个机构名称，请检查后重新导入");
                        }else{
                            verifyDept.put(deptCode,title);
                        }
                        if(deptLevel.get(deptCode) != null && !StrUtil.equals(deptLevel.get(deptCode) ,""+i)){
                            return new ResultUtil<Object>().setErrorMsg("机构编码: "+deptCode+" 处于多个层级，请检查后重新导入");
                        }else{
                            deptLevel.put(deptCode,i+"");
                        }

                    	 if(StringUtils.isBlank(deptCode)) {
                    		 deptCode = title;
                    	 }
                    	//先查询当前机构编码是否存在
                        Department d1 = departmentService.findByDeptCode(deptCode);
                        if(d1 != null) {
                        	department.setSortOrder(d1.getSortOrder());
                        	department.setIsParent(d1.getIsParent());
                        	department.setParentId(d1.getParentId());
                        	department.setId(d1.getId());
                        	department.setTitle(title);
                        	department.setDeptCode(d1.getDeptCode());
                        	department.setUpdateTime(new Date());
                        	if(rowDepartments.size()>0){
                        		Department parentDepart= rowDepartments.get(rowDepartments.size()-1);
                        		if(parentDepart !=null){
                        			department.setParentId(parentDepart.getId());
                        			parentDepart.setIsParent(true);
                        		}
                        		//先看已存在的机构编码是否一级机构
                        		if(CommonConstant.PARENT_ID.equals(d1.getParentId())) {
                        			if(parentDepart !=null){
                        				d1.setIsParent(false);
                            			d1.setParentId(parentDepart.getId());
                            			departmentService.update(d1);
                            		}
                        		}else {
                        			//不是同一个上级机构的时候，查出当前父级机构并改变他的是否为父级的关系
                        			if(!d1.getParentId().equals(parentDepart.getId())) {
                        				Department d2 = departmentService.get(d1.getParentId());
                        				d2.setIsParent(false);
                        				departmentService.update(d2);
                        			}
                        		}
                        	}
                        	else {
                        		d1.setTitle(title);
                        		//departmentService.update(department);
                        	}

                           /* if (!(d1.getParentId().equals(department.getParentId())) || !(d1.getTitle().equals(department.getTitle()))) {
                                ModifyDepartmentVo modifyDepartmentVo = departmentService.getModiifyDepartment(d1.getId(),department);
                                modifyDepartmentVoList.add(modifyDepartmentVo);
                            }*/

                        }else {
                        	department.setSortOrder(new BigDecimal(0));
                        	department.setParentId("0");
                        	department.setTitle(title);
                        	if(StringUtils.isNotBlank(deptCode)) {
                        		department.setDeptCode(deptCode);
                        	}else {
                        		department.setDeptCode(title);
                        	}
                        	if(rowDepartments.size()>0){
                        		Department parentDepart= rowDepartments.get(rowDepartments.size()-1);
                        		/*//查询该机构编码是否存在
                        		Department d = departmentService.findByDeptCode(parentDepart.getDeptCode());
                        		if(d != null) {
                        			department.setId(d.getId());
                        		}*/
                        		if(parentDepart !=null){
                        			department.setParentId(parentDepart.getId());
                        			parentDepart.setIsParent(true);
                        		}
                        	}

                           /* //新增时--推到时活动平台
                            ModifyDepartmentVo modifyDepartmentVo = departmentService.getModiifyDepartment(department);
                            addDepartmentVoList.add(modifyDepartmentVo);*/
                        }
                        rowDepartments.add(department);
                    }
                }
                for (Department department: rowDepartments){
                    String key = department.getTitle()+department.getDeptCode()/*+department.getParentId()*/;
                    if(set.get(key) != null) {
                    	Department d = set.get(key);
                    	List<Department> list = new ArrayList<Department>(set.values());
                    	for (Department department2 : list) {
							if(d.getId().equals(department2.getParentId())) {
								String key2 = department2.getTitle()+department2.getDeptCode()/*+department.getParentId()*/;
								set.get(key2).setParentId(department.getId());
							}
						}
                    	set.remove(key);
                    }
                    set.put(key,department);
                }
                insertDepartment.addAll(rowDepartments);
            }
            Collection<Department> departments=  set.values();
            //departmentService.deleteAll();
            Set<String> keys = RedisUtil.keys("department::"+"*");
            redisTemplate.delete(keys);
//            User u = securityUtil.getCurrUser();
//            String key = "department::"+0+":"+u.getId()+"_"+openDataFilter;
//            String v = redisTemplate.opsForValue().get(key);

            for (Department department : departments) {
            	departmentService.save(department);

                ModifyDepartmentVo modifyDepartmentVo = departmentService.getModiifyDepartment(department);
                modifyDepartmentVo.setType("Modify");
                activityApiUtil.modiifyDepartment(modifyDepartmentVo);
			}

           /* for(ModifyDepartmentVo modifyDepartmentVo:modifyDepartmentVoList){
                modifyDepartmentVo.setType("Modify");
                activityApiUtil.modiifyDepartment(modifyDepartmentVo);
            }
            for(ModifyDepartmentVo modifyDepartmentVo:addDepartmentVoList){
                modifyDepartmentVo.setType("add");
                activityApiUtil.modiifyDepartment(modifyDepartmentVo);
            }*/
            List<String> redisKeys = new ArrayList<>();
            for (Department entity:departments){
                redisKeys.add("Department::" + entity.getId());
            }
            redisTemplate.delete(redisKeys);
            String key = "department::all";
            redisTemplate.delete(key);
        }
        return new ResultUtil<Object>().setSuccessMsg("导入成功");
    }



}
