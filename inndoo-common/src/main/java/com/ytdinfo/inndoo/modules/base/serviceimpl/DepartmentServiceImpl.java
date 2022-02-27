package com.ytdinfo.inndoo.modules.base.serviceimpl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.util.StringUtils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.HibernateProxyTypeAdapter;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.vo.ModifyDepartmentVo;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import com.ytdinfo.inndoo.modules.base.dao.DepartmentDao;
import com.ytdinfo.inndoo.modules.base.entity.Department;
import com.ytdinfo.inndoo.modules.base.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 部门接口实现
 *
 * @author Exrick
 */
@Slf4j
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate<String,List<Department>> DepartmentListTemplate;

    @Override
    public DepartmentDao getRepository() {
        return departmentDao;
    }

    private void cleanRedisCache(){
        String treekey = "department::all";
        redisTemplate.delete(treekey);
        String key = "department::countlevel";
        redisTemplate.delete(key);
    }

    /**
     * 清除缓存数据
     * @param departmentId
     */
    private void cleanRedisCacheById(String departmentId){
        String childkey = "department_allchild::"+departmentId;
        redisTemplate.delete(childkey);
        String staffkey = "department_allstaff::"+departmentId;
        redisTemplate.delete(staffkey);
        String staffNumkey = "department_allstaff_num::"+departmentId;
        redisTemplate.delete(staffNumkey);
    }
    /**
     * 清除缓存数据
     * @param departmentIds
     */
    private void cleanRedisCacheByIds(List<String> departmentIds){
        if(CollectionUtil.isEmpty(departmentIds)){
            return;
        }
        for(String departmentId:departmentIds ){
            cleanRedisCacheById(departmentId);
        }
    }
    /**
     * 清除缓存数据
     * @param departments
     */
    private void cleanRedisCacheByDepartments(List<Department> departments){
        if(CollectionUtil.isEmpty(departments)){
            return;
        }
        for(Department department:departments ){
            cleanRedisCacheById(department.getId());
        }
    }

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    @Override
    public Department get(String id) {
        Optional<Department> entity = getRepository().findById(id);
        if (!entity.isPresent()) {
            return null;
        }
        Department card = entity.get();
        return card;
    }


    /**
     * 保存
     *
     * @param entity
     * @return
     */
    @Override
    public Department save(Department entity) {
        cleanRedisCache();
        Department newDepartment = getRepository().save(entity);
        List<Department> newList = findParentDepartmentById(newDepartment.getId());
        cleanRedisCacheByDepartments(newList);
        return newDepartment;
    }


    /**
     * 修改
     *
     * @param entity
     * @return
     */
    @Override
    public Department update(Department entity) {
        List<Department> oldList = findParentDepartmentById(entity.getId());
        cleanRedisCacheByDepartments(oldList);
        cleanRedisCache();
        Department newDepartment = getRepository().saveAndFlush(entity);
        List<Department> newList = findParentDepartmentById(newDepartment.getId());
        cleanRedisCacheByDepartments(newList);
        return newDepartment;
    }

    /**
     * 删除
     *
     * @param entity
     */
    @Override
    public void delete(Department entity) {
        List<Department> oldList = findParentDepartmentById(entity.getId());
        cleanRedisCacheByDepartments(oldList);
        cleanRedisCache();
        getRepository().delete(entity);
    }

    /**
     * 根据Id删除
     *
     * @param id
     */
    @Override
    public void delete(String id) {
        List<Department> oldList = findParentDepartmentById(id);
        cleanRedisCacheByDepartments(oldList);
        cleanRedisCache();
        getRepository().deleteById(id);
    }

    /**
     * 批量保存与修改
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<Department> saveOrUpdateAll(Iterable<Department> entities) {
        for (Department entity : entities) {
            List<Department> oldList = findParentDepartmentById(entity.getId());
            cleanRedisCacheByDepartments(oldList);
        }
        List<Department> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (Department entity : entities) {
            redisKeys.add("Department::" + entity.getId());
        }
        for(Department department :list){
            List<Department> newList = findParentDepartmentById(department.getId());
            cleanRedisCacheByDepartments(newList);
        }
        redisTemplate.delete(redisKeys);
        cleanRedisCache();
        return list;
    }


    @Override
    public List<Department> findByParentIdOrderBySortOrder(String parentId, Boolean openDataFilter) {

        // 数据权限
        List<String> depIds = securityUtil.getDeparmentIds();
        if (depIds != null && depIds.size() > 0 && openDataFilter) {
            return departmentDao.findByParentIdAndIdInOrderBySortOrder(parentId, depIds);
        }
        return departmentDao.findByParentIdOrderBySortOrder(parentId);
    }

    @Override
    public List<Department> findByParentIdAndStatusOrderBySortOrder(String parentId, Integer status) {

        return departmentDao.findByParentIdAndStatusOrderBySortOrder(parentId, status);
    }

    @Override
    public List<Department> findByTitleLikeOrderBySortOrder(String title, Boolean openDataFilter) {

        // 数据权限
        List<String> depIds = securityUtil.getDeparmentIds();
        if (depIds != null && depIds.size() > 0 && openDataFilter) {
            return departmentDao.findByTitleLikeAndIdInOrderBySortOrder(title, depIds);
        }
        return departmentDao.findByTitleLikeOrderBySortOrder(title);
    }

    @Override
    public List<Department> findByTitleLikeOrDeptCodeLikeOrderBySortOrder(String title, String deptCode, Boolean openDataFilter) {
        // 数据权限
        List<String> depIds = securityUtil.getDeparmentIds();
        if (depIds != null && depIds.size() > 0 && openDataFilter) {
            return departmentDao.findByTitleLikeOrDeptCodeLikeAndIdInOrderBySortOrder(title,deptCode,depIds);
        }
        return departmentDao.findByTitleLikeOrDeptCodeLikeOrderBySortOrder(title,deptCode);
    }


    @Override
    public List<Department> findByDeptCodes(List<String> deptCode) {
        return departmentDao.findByDeptCodes(deptCode);
    }

    @Override
    public Integer countLevel() {
        String key = "department::countlevel";
        String v = redisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(v)){
            return Integer.valueOf(v);
        }
        List<Department> list = departmentDao.findByStatusAndAppid(0, UserContext.getAppid());
        Map<String,Object> map =new HashMap<>();
        map.put("level",0);
        transformToTree(list,map);
        Object object = map.get("level");
        Integer level = 0;
        if(object != null){
            level = (Integer) object;
            redisTemplate.opsForValue().set(key,level.toString());
        }
        return level;
    }

    @Override
    public List<Department> findAllToTree() {
        List<Department> list = new ArrayList<>();
        String key = "department::all";
        String v = redisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(v)){
            list = new Gson().fromJson(v, new TypeToken<List<Department>>(){}.getType());
            if(list.size() >0){
                return list;
            }
        }
        list = departmentDao.findByStatusAndAppid(0, UserContext.getAppid());
        list = transformToTree(list,new HashMap<>());
        Collator collator = Collator.getInstance(Locale.CHINESE);
        list.sort((o1, o2) -> collator.compare(o1.getTitle(), o2.getTitle()));
        if(list.size()>0){
            redisTemplate.opsForValue().set(key,
                    new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(list));
        }
        return list;
    }

    @Override
    public List<Department> findAll() {
        List<Department> departments = departmentDao.findByStatusAndAppid(0, UserContext.getAppid());
        return departments;
    }

    @Override
    public List<Department> findByIdIn(List<String> ids) {
        List<Department> departments = departmentDao.findByIdIn(ids);
        return departments;
    }

    @Override
    public List<Department> findByAppid(String appid) {
        List<Department> departments = departmentDao.findByAppid(appid);
        return departments;
    }

    @Override
    public void deleteAll() {
        getRepository().deleteAll();
        Set<String> keys = RedisUtil.keys("department:" + "*");
        redisTemplate.unlink(keys);
        cleanRedisCache();
        Set<String> childkeys = RedisUtil.keys("department_allchild:" + "*");
        Set<String> staffkeys = RedisUtil.keys("department_allstaff:" + "*");
        redisTemplate.unlink(childkeys);
        redisTemplate.unlink(staffkeys);
    }

    /**
     * 批量删除
     *
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<Department> entities) {
        for(Department entity : entities){
            List<Department> oldList = findParentDepartmentById(entity.getId());
            cleanRedisCacheByDepartments(oldList);
        }
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (Department entity : entities) {
            redisKeys.add("department::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
        cleanRedisCache();
    }

    /**
     * 根据Id批量删除
     *
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String[] ids) {
        for (String id : ids) {
            List<Department> oldList = findParentDepartmentById(id);
            cleanRedisCacheByDepartments(oldList);
        }
        DepartmentDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<Department> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id : ids) {
            redisKeys.add("department::" + id);
        }
        redisTemplate.delete(redisKeys);
        cleanRedisCache();
    }


    /**
     * 根据部门ID 获取该部门包含所有子部门 列表
     * @param pid
     * @return
     */
    @Override
    public List<Department> findAllDepartmentByPId(String pid){
        String departmentkey = "department_allchild::"+pid;
        List<Department> cacheList = DepartmentListTemplate.opsForValue().get(departmentkey);
        if(cacheList != null && cacheList.size()>0){
            return  cacheList;
        }
        List<Department> list = departmentDao.findByStatusAndAppid(0, UserContext.getAppid());
        if(list == null){
            return new ArrayList<>();
        }
        List<Department> rootList = new ArrayList<>();
        for (Department department : list) {
            if (pid.equals(department.getParentId())) {
                rootList.add(department);
            }
        }
        for (Department department : rootList) {
            list.remove(department);
        }
        List<Department> tempDepartment= new ArrayList<>();
        if (rootList.size() > 0) {
            for(Department root: rootList){
                addNextDepartment(root, list ,tempDepartment);
            }
        }
        rootList.addAll(tempDepartment);
        DepartmentListTemplate.opsForValue().set(departmentkey,rootList);
        DepartmentListTemplate.expire(departmentkey,1, TimeUnit.DAYS);
        return rootList;
    }

    public Department addNextDepartment(Department root,List<Department> paramList, List<Department> tempList){
        List<Department> temp = new ArrayList<>();
        for (Department param : paramList) {
            if (StringUtils.equals(root.getId(), param.getParentId())) {
                List<Department> children = root.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                }
                children.add(param);
                temp.add(param);
                root.setChildren(children);
            }
        }
        if(root.getChildren() != null){
            tempList.addAll(root.getChildren());
        }
        for (Department department : temp) {
            paramList.remove(department);
        }
        if (root.getChildren() != null && 0 < root.getChildren().size()) {
            for (int i = 0; i < root.getChildren().size(); i++) {
                addNextDepartment(root.getChildren().get(i), paramList,tempList);
            }
        }
        return root;
    }

    @Override
    public List<Department> transformToTree(List<Department> paramList, Map<String,Object> map) {
        if (paramList == null) {
            return null;
        }
        Object o = null;
        if(map != null){
            o = map.get("level");
        }
        List<Department> rootList = new ArrayList<>();
        for (Department department : paramList) {
            if ("0".equals(department.getParentId())) {
                department.setLevel(1);
                if(o != null){
                    Integer level = (Integer) o;
                    level = 1;
                    map.put("level",level);
                }
                rootList.add(department);
            }
        }
        for (Department department : rootList) {
            paramList.remove(department);
        }
        if (rootList.size() > 0) {
            for (Department tree : rootList) {
                addChildren(tree, paramList,map);
            }
        }
        return rootList;
    }

    @Override
    public Department findNodeByParentid(Department rootNode, String parentid) {
        if (StringUtils.equals(rootNode.getId(), parentid)) {
            return rootNode;
        }
        LinkedList<Department> plist = new LinkedList<>();
        if (CollectionUtil.isNotEmpty(rootNode.getParentList())) {
            plist.addAll(rootNode.getParentList());
        }
        Department childnode = null;
        List<Department> departments = rootNode.getChildren();
        if (departments == null) {
            return childnode;
        }
        for (Department temp : departments) {
            LinkedList<Department> prentlist = new LinkedList<>();
            prentlist.addAll(plist);
            prentlist.add(rootNode);
            temp.setParentList(prentlist);
            childnode = findNodeByParentid(temp, parentid);
            if (childnode != null) {
                return childnode;
            }
        }
        return childnode;
    }

    @Override
    public void tranRow(Department tempNode, LinkedList<LinkedList<String>> row,
                        LinkedList<String> repeatRow) {
        LinkedList<String> pseff = new LinkedList<>();
        if (tempNode != null) {
            LinkedList<Department> plist = new LinkedList<>();
            if (CollectionUtil.isNotEmpty(tempNode.getParentList())) {
                plist.addAll(tempNode.getParentList());
            }
            for (Department pDepartmenty : plist) {
                pseff.add(pDepartmenty.getTitle());
                pseff.add(pDepartmenty.getDeptCode());
            }
            pseff.add(tempNode.getTitle());
            pseff.add(tempNode.getDeptCode());
            if (CollectionUtil.isEmpty(tempNode.getChildren())) {
                if (!repeatRow.contains(tempNode.getDeptCode())) {
                    row.add(pseff);
                    repeatRow.add(tempNode.getDeptCode());
                }
            } else {
                for (Department temp : tempNode.getChildren()) {
                    LinkedList<Department> prentlist = new LinkedList<>();
                    prentlist.addAll(plist);
                    prentlist.add(tempNode);
                    temp.setParentList(prentlist);
                    tranRow(temp, row, repeatRow);
                }
            }
        }
    }

    @Override
    public List<Department> findByTitle(String title) {
        return getRepository().findByTitleAndAppid(title, UserContext.getAppid());
    }

    private Department addChildren(Department root, List<Department> paramList, Map<String,Object> map) {
        List<Department> temp = new ArrayList<>();
        for (Department param : paramList) {
            if (StringUtils.equals(root.getId(), param.getParentId())) {
                List<Department> children = root.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                }
                param.setLevel(root.getLevel()+1);
                if(map != null){
                    Object o = map.get("level");
                    if(o != null){
                        Integer level = (Integer) o;
                        if(param.getLevel() > level){
                            level = param.getLevel();
                        }
                        map.put("level",level);
                    }
                }
                children.add(param);
                temp.add(param);
                root.setChildren(children);
            }
        }
        for (Department department : temp) {
            paramList.remove(department);
        }
        if (root.getChildren() != null && 0 < root.getChildren().size()) {
            for (int i = 0; i < root.getChildren().size(); i++) {
                addChildren(root.getChildren().get(i), paramList,map);
            }
        }
        return root;
    }

    @Override
    public Department findByDeptCode(String deptCode) {
        return departmentDao.findByDeptCode(deptCode);
    }

    @Override
    public List<Department> findByAppidAndParentIdAndStatus(String appid, String parentId, Integer status) {
        return departmentDao.findByAppidAndParentIdAndStatus(appid, parentId, status);
    }

    @Override
    public Department selectById(String id) {
        Department department = get(id);
        if (department != null) {
            LinkedList<Department> parentList = new LinkedList<>();
            initParentList(department, parentList);
            department.setParentList(parentList);
        }
        return department;
    }

    private void initParentList(Department department, LinkedList<Department> parentList) {
        if (!department.getParentId().equals("0")) {
            if(department.getId().equals(department.getParentId())){
                throw new RuntimeException("error department's parentId,departmentId:"+department.getId());
            }
            Department parent = get(department.getParentId());
            department.setParentTitle(parent.getTitle());
            parentList.add(0, parent);
            initParentList(parent, parentList);
        }
    }

    @Override
    public List<Department> findParentDepartmentById(String oldDepartmentId) {
        List<Department> departments = findAll();
        Map<String,Department> map = new HashMap<>();
        for(Department department: departments){
            map.put(department.getId(),department);
        }
        LinkedList<Department> list = new LinkedList<>();
        if(departments.size()==0){
            return list;
        }
        while (map.get(oldDepartmentId) != null){
            Department department = map.get(oldDepartmentId);
            list.add(department);
            oldDepartmentId = department.getParentId();
        }
        if(list.size()>0){
            list.remove(0);
            Collections.reverse(list);
        }
        return list;
    }

    @Override
    public ModifyDepartmentVo getModiifyDepartment(String id,Department newDepartment) {
        Department department =  get(id);
        if(department == null){
            return null;
        }
        ModifyDepartmentVo modifyDepartmentVo = new ModifyDepartmentVo();
        modifyDepartmentVo.setId(id);
        modifyDepartmentVo.setOldTitle(department.getTitle());
        modifyDepartmentVo.setOldDeptCode(department.getDeptCode());
        List<Department> departments = findParentDepartmentById(id);
        LinkedList<Department> departmentLinkedList = ( LinkedList<Department>)departments;
        modifyDepartmentVo.setParentList(departmentLinkedList);
        modifyDepartmentVo.setIsParent(newDepartment.getIsParent());
        modifyDepartmentVo.setParentId(newDepartment.getParentId());
        modifyDepartmentVo.setSortOrder(newDepartment.getSortOrder());
        modifyDepartmentVo.setStatus(newDepartment.getStatus());
        modifyDepartmentVo.setTenantId( UserContext.getTenantId());
        modifyDepartmentVo.setAppid(newDepartment.getAppid());
        modifyDepartmentVo.setTitle(newDepartment.getTitle());
        modifyDepartmentVo.setDeptCode(newDepartment.getDeptCode());
        return modifyDepartmentVo;
    }

    @Override
    public  ModifyDepartmentVo getModiifyDepartment(Department department){
        if(department == null){
            return null;
        }
        ModifyDepartmentVo modifyDepartmentVo = new ModifyDepartmentVo();
        modifyDepartmentVo.setId(department.getId());
        modifyDepartmentVo.setOldTitle(department.getTitle());
        modifyDepartmentVo.setOldDeptCode(department.getDeptCode());
        modifyDepartmentVo.setTitle(department.getTitle());
        modifyDepartmentVo.setDeptCode(department.getDeptCode());
        modifyDepartmentVo.setIsParent(department.getIsParent());
        modifyDepartmentVo.setParentId(department.getParentId());
        modifyDepartmentVo.setSortOrder(department.getSortOrder());
        modifyDepartmentVo.setStatus(department.getStatus());
        modifyDepartmentVo.setTenantId( UserContext.getTenantId());
        modifyDepartmentVo.setAppid(department.getAppid());
        List<Department> departments = findParentDepartmentById(department.getParentId());
        departments.add(department);
        LinkedList<Department> departmentLinkedList = ( LinkedList<Department>)departments;
        modifyDepartmentVo.setParentList(departmentLinkedList);
        return modifyDepartmentVo;
    }

    @Override
    public ModifyDepartmentVo getModiifyDepartment(String id) {
        Department department =  get(id);
        if(department == null){
            return null;
        }
        ModifyDepartmentVo modifyDepartmentVo = new ModifyDepartmentVo();
        modifyDepartmentVo.setId(id);
        modifyDepartmentVo.setOldTitle(department.getTitle());
        modifyDepartmentVo.setOldDeptCode(department.getDeptCode());
        List<Department> departments = findParentDepartmentById(id);
        LinkedList<Department> departmentLinkedList = ( LinkedList<Department>)departments;
        modifyDepartmentVo.setParentList(departmentLinkedList);
        modifyDepartmentVo.setIsParent(department.getIsParent());
        modifyDepartmentVo.setParentId(department.getParentId());
        modifyDepartmentVo.setSortOrder(department.getSortOrder());
        modifyDepartmentVo.setStatus(department.getStatus());
        modifyDepartmentVo.setTenantId( UserContext.getTenantId());
        modifyDepartmentVo.setAppid(department.getAppid());
        modifyDepartmentVo.setTitle(department.getTitle());
        modifyDepartmentVo.setDeptCode(department.getDeptCode());
        return modifyDepartmentVo;
    }


    /**
     * 根据过滤部门id集合 返回树形结构
     * @param filterIds  过滤部门ID集合
     */
    @Override
    public List<Department>filterTree(List<String> filterIds){
        List<Department> tree =  findAllToTree();
        List<Department> result = new ArrayList<>();
        Set<String> filterIdSet = new HashSet<>();
        filterIdSet.addAll(filterIds);
        for(String id :filterIds) {
            List<Department> par = findParentDepartmentById(id);
            for (Department tempid : par) {
                filterIdSet.add(tempid.getId());
            }
        }
        result = filtertoTree(tree,filterIdSet,result,filterIds);
        return result;
    }


    private List<Department> filtertoTree(List<Department> tree, Set<String> filterId ,List<Department> result,List<String> markIds){
        if (CollectionUtil.isEmpty(tree)){
            return new ArrayList<>();
        }
        for (Department item : tree) {
            if (!filterId.contains(item.getId())){
                continue;
            }
            Department node = new Department();
            BeanUtils.copyProperties(item,node);
            node.setChildren(new ArrayList<>());
            if(markIds.contains(item.getId())){
                node.setHasContact(true);
            }
            result.add(node);
            if (!CollectionUtil.isEmpty(item.getChildren())){
                filtertoTree(item.getChildren(), filterId, node.getChildren(),markIds);
            }
        }
        return result;
    }

}