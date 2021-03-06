package com.ytdinfo.inndoo.modules.activiti.serviceimpl;

import com.ytdinfo.inndoo.common.constant.ActivitiConstant;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.activiti.dao.ActProcessDao;
import com.ytdinfo.inndoo.modules.activiti.entity.ActBusiness;
import com.ytdinfo.inndoo.modules.activiti.entity.ActProcess;
import com.ytdinfo.inndoo.modules.activiti.service.ActNodeService;
import com.ytdinfo.inndoo.modules.activiti.service.ActProcessService;
import com.ytdinfo.inndoo.modules.activiti.utils.MessageUtil;
import com.ytdinfo.inndoo.modules.activiti.vo.ProcessNodeVo;
import com.ytdinfo.inndoo.modules.base.entity.DepartmentHeader;
import com.ytdinfo.inndoo.modules.base.entity.Role;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.service.DepartmentHeaderService;
import com.ytdinfo.inndoo.modules.base.service.UserRoleService;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.util.*;

/**
 * ????????????????????????
 * @author Exrick
 */
@Slf4j
@Service
public class ActProcessServiceImpl implements ActProcessService {

    @Autowired
    private ActProcessDao actProcessDao;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ActNodeService actNodeService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentHeaderService departmentHeaderService;

    @Autowired
    private MessageUtil messageUtil;

    @Override
    public ActProcessDao getRepository() {
        return actProcessDao;
    }

    @Override
    public Page<ActProcess> findByCondition(Boolean showLatest, ActProcess actProcess, SearchVo searchVo, Pageable pageable) {

        return actProcessDao.findAll(new Specification<ActProcess>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<ActProcess> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                Path<String> nameField = root.get("name");
                Path<String> processKeyField = root.get("processKey");
                Path<String> categoryIdField = root.get("categoryId");
                Path<Integer> statusField = root.get("status");
                Path<Date> createTimeField = root.get("createTime");
                Path<Boolean> latestField = root.get("latest");

                List<Predicate> list = new ArrayList<Predicate>();

                // ????????????
                if(StrUtil.isNotBlank(actProcess.getName())){
                    list.add(cb.like(nameField,'%'+actProcess.getName()+'%'));
                }
                if(StrUtil.isNotBlank(actProcess.getProcessKey())){
                    list.add(cb.like(processKeyField,'%'+actProcess.getProcessKey()+'%'));
                }

                // ??????
                if(StrUtil.isNotBlank(actProcess.getCategoryId())){
                    list.add(cb.equal(categoryIdField, actProcess.getCategoryId()));
                }

                // ??????
                if(actProcess.getStatus()!=null){
                    list.add(cb.equal(statusField, actProcess.getStatus()));
                }
                // ????????????
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                // ???????????????
                if(showLatest!=null&&showLatest){
                    list.add(cb.equal(latestField, true));
                }

                Predicate[] arr = new Predicate[list.size()];
                if(list.size() > 0){
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    @Override
    public ActProcess findByProcessKeyAndLatest(String processKey, Boolean latest) {

        List<ActProcess> list = actProcessDao.findByProcessKeyAndLatest(processKey, latest);
        if(list!=null&&list.size()>0){
            return list.get(0);
        }
        return null;
    }

    @Override
    public void setAllOldByProcessKey(String processKey) {

        List<ActProcess> list = actProcessDao.findByProcessKey(processKey);
        if(list==null||list.size()==0){
            return;
        }
        list.forEach(item -> {
            item.setLatest(false);
        });
        actProcessDao.saveAll(list);
    }

    @Override
    public void setLatestByProcessKey(String processKey) {

        ActProcess actProcess = actProcessDao.findTopByProcessKeyOrderByVersionDesc(processKey);
        if(actProcess==null){
            return;
        }
        actProcess.setLatest(true);
        actProcessDao.save(actProcess);
    }

    @Override
    public List<ActProcess> findByCategoryId(String categoryId) {

        return actProcessDao.findByCategoryId(categoryId);
    }

    @Override
    public String startProcess(ActBusiness actBusiness) {

        String userId = securityUtil.getCurrUser().getId();
        // ??????????????????
        identityService.setAuthenticatedUserId(userId);
        // ???????????? ?????????????????????id??????
        Map<String, Object> paramId = new HashMap<>(16);
        paramId.put("tableId", actBusiness.getTableId());
        ProcessInstance pi = runtimeService.startProcessInstanceById(actBusiness.getProcDefId(), actBusiness.getId(), paramId);
        // ????????????????????????
        runtimeService.setProcessInstanceName(pi.getId(), actBusiness.getTitle());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        for(Task task : tasks){
            // ???????????????????????????
            for(String assignee : actBusiness.getAssignees()){
                taskService.addCandidateUser(task.getId(), assignee);
                // ???????????????
                messageUtil.sendActMessage(assignee, ActivitiConstant.MESSAGE_TODO_CONTENT, actBusiness.getSendMessage(),
                        actBusiness.getSendSms(), actBusiness.getSendEmail());
            }
            // ?????????????????????
            taskService.setPriority(task.getId(), actBusiness.getPriority());
        }
        return pi.getId();
    }

    @Override
    public ProcessNodeVo getFirstNode(String procDefId) {

        BpmnModel bpmnModel = repositoryService.getBpmnModel(procDefId);

        ProcessNodeVo node = new ProcessNodeVo();

        List<Process> processes = bpmnModel.getProcesses();
        Collection<FlowElement> elements = processes.get(0).getFlowElements();
        // ??????????????????
        StartEvent startEvent = null;
        for (FlowElement element : elements) {
            if (element instanceof StartEvent) {
                startEvent = (StartEvent) element;
                break;
            }
        }
        FlowElement e = null;
        // ??????????????????????????????
        SequenceFlow sequenceFlow = startEvent.getOutgoingFlows().get(0);
        for (FlowElement element : elements) {
            if(element.getId().equals(sequenceFlow.getTargetRef())){
                if(element instanceof UserTask){
                    e = element;
                    node.setType(ActivitiConstant.NODE_TYPE_TASK);
                    break;
                }else{
                    throw new InndooException("???????????????????????????????????????????????????????????????");
                }
            }
        }
        node.setTitle(e.getName());
        // ??????????????????
        List<User> users = actNodeService.findUserByNodeId(e.getId());
        // ???????????????????????????
        List<Role> roles = actNodeService.findRoleByNodeId(e.getId());
        for(Role r : roles){
            List<User> userList = userRoleService.findUserByRoleId(r.getId());
            users.addAll(userList);
        }
        // ???????????????????????????
        List<String> departmentIds = actNodeService.findDepartmentIdsByNodeId(e.getId());
        List<DepartmentHeader> departments = departmentHeaderService.findByDepartmentIdIn(departmentIds);
        for (DepartmentHeader d : departments){
            User user = userService.get(d.getUserId());
            users.add(user);
        }
        node.setUsers(removeDuplicate(users));
        return node;
    }

    @Override
    public ProcessNodeVo getNextNode(String procInstId) {

        ProcessNodeVo node = new ProcessNodeVo();

        // ??????????????????id
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        String currActId = pi.getActivityId();
        ProcessDefinitionEntity dfe = (ProcessDefinitionEntity) ((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(pi.getProcessDefinitionId());
        // ??????????????????
        List<ActivityImpl> activitiList = dfe.getActivities();
        // ???????????????????????????????????????????????????????????????????????????
        for(ActivityImpl activityImpl : activitiList){
            if (activityImpl.getId().equals(currActId)) {
                // ?????????????????????
                List<PvmTransition> pvmTransitions = activityImpl.getOutgoingTransitions();

                PvmActivity pvmActivity = pvmTransitions.get(0).getDestination();
                String type = pvmActivity.getProperty("type").toString();
                if("userTask".equals(type)){
                    // ??????????????????
                    node.setType(ActivitiConstant.NODE_TYPE_TASK);
                    node.setTitle(pvmActivity.getProperty("name").toString());
                    // ??????????????????
                    List<User> users = actNodeService.findUserByNodeId(pvmActivity.getId());
                    // ???????????????????????????
                    List<Role> roles = actNodeService.findRoleByNodeId(pvmActivity.getId());
                    for(Role r : roles){
                        List<User> userList = userRoleService.findUserByRoleId(r.getId());
                        users.addAll(userList);
                    }
                    // ???????????????????????????
                    List<String> departmentIds = actNodeService.findDepartmentIdsByNodeId(pvmActivity.getId());
                    List<DepartmentHeader> departments = departmentHeaderService.findByDepartmentIdIn(departmentIds);
                    for (DepartmentHeader d : departments){
                        User user = userService.get(d.getUserId());
                        users.add(user);
                    }
                    node.setUsers(removeDuplicate(users));
                }else if("endEvent".equals(type)){
                    // ??????
                    node.setType(ActivitiConstant.NODE_TYPE_END);
                }else{
                    throw new InndooException("??????????????????????????????????????????");
                }
                break;
            }
        }

        return node;
    }

    @Override
    public ProcessNodeVo getNode(String nodeId) {

        ProcessNodeVo node = new ProcessNodeVo();
        // ??????????????????
        List<User> users = actNodeService.findUserByNodeId(nodeId);
        // ???????????????????????????
        List<Role> roles = actNodeService.findRoleByNodeId(nodeId);
        for(Role r : roles){
            List<User> userList = userRoleService.findUserByRoleId(r.getId());
            users.addAll(userList);
        }
        // ???????????????????????????
        List<String> departmentIds = actNodeService.findDepartmentIdsByNodeId(nodeId);
        List<DepartmentHeader> departments = departmentHeaderService.findByDepartmentIdIn(departmentIds);
        for (DepartmentHeader d : departments){
            User user = userService.get(d.getUserId());
            users.add(user);
        }
        node.setUsers(removeDuplicate(users));
        return node;
    }

    /**
     * ??????
     * @param list
     * @return
     */
    private List<User> removeDuplicate(List<User> list) {

        LinkedHashSet<User> set = new LinkedHashSet<User>(list.size());
        set.addAll(list);
        list.clear();
        list.addAll(set);
        return list;
    }
}