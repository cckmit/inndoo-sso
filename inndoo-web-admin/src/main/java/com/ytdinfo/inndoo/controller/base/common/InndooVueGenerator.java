package com.ytdinfo.inndoo.controller.base.common;

import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.enums.RedisKeyStoreType;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.config.security.permission.MySecurityMetadataSource;
import com.ytdinfo.inndoo.generator.bean.Field;
import com.ytdinfo.inndoo.modules.base.entity.Permission;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.entity.RolePermission;
import com.ytdinfo.inndoo.modules.base.service.PermissionService;
import com.ytdinfo.inndoo.modules.base.service.RolePermissionService;
import com.ytdinfo.inndoo.modules.base.service.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.FileResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author nikou
 */
@Slf4j
@RestController
@Api(description = "Vue代码生成")
@RequestMapping(value = "/base/generate")
public class InndooVueGenerator {

    @Autowired
    private PermissionService permissionService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RoleService roleService;
    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private MySecurityMetadataSource mySecurityMetadataSource;

    @RequestMapping(value = "/table/{vueName}/{rowNum}", method = RequestMethod.POST)
    @ApiOperation(value = "增删改表格生成")
    public Result<Object> generateTable(@PathVariable String vueName,
                                        @PathVariable Integer rowNum,
                                        @RequestBody List<Field> fields) throws IOException {

        String result = generate("table.btl", vueName, rowNum, fields);

        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/tree/{vueName}/{rowNum}", method = RequestMethod.POST)
    @ApiOperation(value = "树形结构生成")
    public Result<Object> generateTree(@PathVariable String vueName,
                                       @PathVariable Integer rowNum,
                                       @RequestBody List<Field> fields) throws IOException {

        String result = generate("tree.btl", vueName, rowNum, fields);
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/getEntityData/{path}", method = RequestMethod.GET)
    @ApiOperation(value = "通过实体类生成Vue代码Json数据")
    public Result<Object> getEntityData(@PathVariable String path) {

        String result = "";
        try {
            result = gengerateEntityData(path);
        } catch (Exception e) {
            return new ResultUtil<Object>().setErrorMsg("实体类文件不存在");
        }
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/generateMenu", method = RequestMethod.GET)
    @ApiOperation(value = "通过实体类生成Vue代码Json数据")
    @CacheEvict(cacheNames = "permission",key = "'allList'")
    public Result<Object> generateMenu(@RequestParam String path, @RequestParam String parentMenuName, @RequestParam String menuName) {

        String className = "";
        try {
            Class c = Class.forName(path);
            className = c.getSimpleName();
        } catch (Exception e) {
            return new ResultUtil<>().setErrorMsg("实体类文件不存在");
        }
        List<Permission> permissionList = permissionService.findByTitle(parentMenuName);
        if (permissionList.size() == 0) {
            return new ResultUtil<>().setErrorMsg("找不到父级菜单");
        }
        Permission permissionParent = permissionList.get(0);
        Permission permission = new Permission();
        permission.setName(className.toLowerCase());
        permission.setTitle(menuName);
        permission.setPath(className.toLowerCase());
        permission.setLevel(2);
        permission.setType(CommonConstant.PERMISSION_PAGE);
        permission.setComponent(permissionParent.getPath() + "/" + className.toLowerCase() + "/" + className);
        permission.setParentId(permissionParent.getId());
        permission.setSortOrder(new BigDecimal(0));
        permission.setStatus(CommonConstant.STATUS_NORMAL);
        permission.setIcon("ios-alarm");
        List<String> permissions = new ArrayList<>();
        Permission u = permissionService.save(permission);
        permissions.add(u.getId());

        permissions.add(createChildMenu(className, u.getId(), "/query/*", "view", "获取" + menuName));
        permissions.add(createChildMenu(className, u.getId(), "/list", "view", menuName + "所有清单"));
        permissions.add(createChildMenu(className, u.getId(), "/listByPage*", "view", menuName + "分页清单"));
        permissions.add(createChildMenu(className, u.getId(), "/listByCondition*", "view", "查询" + menuName));
        permissions.add(createChildMenu(className, u.getId(), "/update", "edit", "更新" + menuName));
        permissions.add(createChildMenu(className, u.getId(), "/create", "add", "创建" + menuName));
        permissions.add(createChildMenu(className, u.getId(), "/delete/*", "delete", "删除" + menuName));
        permissions.add(createChildMenu(className, u.getId(), "/batch_delete/*", "delete", "批量删除" + menuName));

        Role roleAdmin = roleService.findByName("ROLE_ADMIN");
        //分配新权限
        for(String permId : permissions){
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleAdmin.getId());
            rolePermission.setPermissionId(permId);
            rolePermissionService.save(rolePermission);
        }
        //手动批量删除缓存
        mySecurityMetadataSource.loadResourceDefine(true);
        Set<String> keysUser = RedisUtil.membersFromKeyStore(RedisKeyStoreType.user.getPrefixKey());
        redisTemplate.delete(keysUser);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.user.getPrefixKey());
        Set<String> keysUserRole = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userRole.getPrefixKey());
        redisTemplate.delete(keysUserRole);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userRole.getPrefixKey());
        Set<String> keysUserPerm = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userPermission.getPrefixKey());
        redisTemplate.delete(keysUserPerm);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userPermission.getPrefixKey());
        Set<String> keysUserMenu = RedisUtil.membersFromKeyStore(RedisKeyStoreType.userMenuList.getPrefixKey());
        redisTemplate.delete(keysUserMenu);
        RedisUtil.clearKeyFromStore(RedisKeyStoreType.userMenuList.getPrefixKey());
        //手动批量删除缓存
        redisTemplate.delete("permission::allList");
        return new ResultUtil<>().setData("OK");
    }

    private String createChildMenu(String className, String parentId, String path, String buttonType, String menuName) {
        Permission permission = new Permission();
        permission.setName("");
        permission.setTitle(menuName);
        permission.setPath("/" + className.toLowerCase() + path);
        permission.setLevel(3);
        permission.setType(CommonConstant.PERMISSION_OPERATION);
        permission.setComponent("");
        permission.setParentId(parentId);
        permission.setSortOrder(new BigDecimal(0));
        permission.setStatus(CommonConstant.STATUS_NORMAL);
        permission.setIcon("");
        permission.setButtonType(buttonType);
        Permission p = permissionService.save(permission);
        return p.getId();
    }

    public String generate(String template, String vueName, Integer rowNum, List<Field> fields) throws IOException {

        // 模板路径
        String root = System.getProperty("user.dir") + "/inndoo-common/src/main/java/com/ytdinfo/inndoo/generator/vue";
        FileResourceLoader resourceLoader = new FileResourceLoader(root, "utf-8");
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);

        Template tableTemplate = gt.getTemplate(template);
        // 排序
        Collections.sort(fields, Comparator.comparing(Field::getSortOrder));
        // 绑定变量
        tableTemplate.binding("vueName", vueName);
        tableTemplate.binding("fields", fields);
        // 判断有无upload和日期范围搜索
        Boolean upload = false;
        for (Field f : fields) {
            if ("upload".equals(f.getType())) {
                upload = true;
            }
        }
        tableTemplate.binding("upload", upload);
        if ("table.btl".equals(template)) {
            // 判断有无upload和日期范围搜索
            Boolean daterangeSearch = false;
            for (Field f : fields) {
                if (f.getSearchable() && "daterange".equals(f.getSearchType())) {
                    daterangeSearch = true;
                }
            }
            tableTemplate.binding("daterangeSearch", daterangeSearch);
            // 统计搜索栏个数 判断是否隐藏搜索栏
            Boolean hideSearch = false;
            List<Field> firstTwo = new ArrayList<>();
            List<Field> rest = new ArrayList<>();
            Integer count = 0;
            for (Field f : fields) {
                if (f.getSearchable()) {
                    count++;
                    if (count <= 2) {
                        firstTwo.add(f);
                    } else {
                        rest.add(f);
                    }
                }
            }
            if (count >= 4) {
                hideSearch = true;
                tableTemplate.binding("firstTwo", firstTwo);
                tableTemplate.binding("rest", rest);
            }
            tableTemplate.binding("searchSize", count);
            tableTemplate.binding("hideSearch", hideSearch);
            // 获取默认排序字段
            String defaultSort = "", defaultSortType = "";
            for (Field f : fields) {
                if (f.getDefaultSort()) {
                    defaultSort = f.getField();
                    defaultSortType = f.getDefaultSortType();
                    break;
                }
            }
            tableTemplate.binding("defaultSort", defaultSort);
            tableTemplate.binding("defaultSortType", defaultSortType);
        }
        // 一行几列
        tableTemplate.binding("rowNum", rowNum);
        if (rowNum == 1) {
            tableTemplate.binding("modalWidth", 500);
            tableTemplate.binding("width", "100%");
            tableTemplate.binding("editWidth", "100%");
            tableTemplate.binding("itemWidth", "");
            tableTemplate.binding("span", "9");
        } else if (rowNum == 2) {
            tableTemplate.binding("modalWidth", 770);
            tableTemplate.binding("width", "250px");
            tableTemplate.binding("editWidth", "250px");
            tableTemplate.binding("itemWidth", "350px");
            tableTemplate.binding("span", "17");
        } else if (rowNum == 3) {
            tableTemplate.binding("modalWidth", 980);
            tableTemplate.binding("width", "200px");
            tableTemplate.binding("editWidth", "200px");
            tableTemplate.binding("itemWidth", "300px");
            tableTemplate.binding("span", "17");
        } else if (rowNum == 4) {
            tableTemplate.binding("modalWidth", 1130);
            tableTemplate.binding("width", "160px");
            tableTemplate.binding("editWidth", "160px");
            tableTemplate.binding("itemWidth", "260px");
            tableTemplate.binding("span", "17");
        } else {
            throw new InndooException("rowNum仅支持数字1-4");
        }
        // 生成代码
        String result = tableTemplate.render();
        System.out.println(result);
        return result;
    }

    public String gengerateEntityData(String path) throws Exception {

        Class c = Class.forName(path);

        Object obj = c.newInstance();

        String start = "{\n" +
                "    \"data\": [";
        String end = "\n    ]\n" +
                "}";
        String field_all = "";
        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        for (int j = 0; j < fields.length; j++) {
            java.lang.reflect.Field field = fields[j];
            field.setAccessible(true);
            // 字段名
            String fieldName = field.getName();
            String fieldType = field.getType().getName();
            // 白名单
            if ("serialVersionUID".equals(fieldName) || "actBusinessId".equals(fieldName) || "applyUser".equals(fieldName)
                    || "routeName".equals(fieldName) || "procInstId".equals(fieldName) || "applyTime".equals(fieldName)
                    || "status".equals(fieldName) || "result".equals(fieldName)) {
                continue;
            }

            // 获得字段注解
            ApiModelProperty myFieldAnnotation = field.getAnnotation(ApiModelProperty.class);
            String fieldName_CN = fieldName;
            if (myFieldAnnotation != null) {
                fieldName_CN = myFieldAnnotation.value();
            }
            fieldName_CN = (fieldName_CN == null || fieldName_CN == "") ? fieldName : fieldName_CN;

            String type = "text";
            String searchType = "text";
            // 日期字段特殊处理,其他一律按 字符串界面处理
            if (fieldType == "java.lang.Date" || fieldType == "java.util.Date" || fieldType == "Date") {
                type = "date";
                searchType = "daterange";
            }
            String field_json = "\n        {\n" +
                    "            \"sortOrder\": " + j + ",\n" +
                    "            \"field\": \"" + fieldName + "\",\n" +
                    "            \"name\": \"" + fieldName_CN + "\",\n" +
                    "            \"level\": \"2\",\n" +
                    "            \"tableShow\": true,\n" +
                    "            \"editable\": true,\n" +
                    "            \"type\": \"" + type + "\",\n" +
                    "            \"searchType\": \"" + searchType + "\",\n" +
                    "            \"searchLevel\": \"2\",\n" +
                    "            \"validate\": false,\n" +
                    "            \"searchable\": true,\n" +
                    "            \"sortable\": false,\n" +
                    "            \"defaultSort\": false,\n" +
                    "            \"defaultSortType\": \"desc\"\n" +
                    "        }";
            String splitChar = field_all == "" ? "" : ",";
            field_all = field_all + splitChar + field_json;
        }
        String json = start + field_all + end;
        return json;
    }
}
