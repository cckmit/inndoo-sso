package com.ytdinfo.inndoo.controller.activiti;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.ActivitiConstant;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.utils.SecurityUtil;
import com.ytdinfo.inndoo.common.utils.SnowFlakeUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.activiti.entity.ActBusiness;
import com.ytdinfo.inndoo.modules.activiti.entity.ActProcess;
import com.ytdinfo.inndoo.modules.activiti.service.ActBusinessService;
import com.ytdinfo.inndoo.modules.activiti.service.ActProcessService;
import com.ytdinfo.inndoo.modules.activiti.service.mybatis.IHistoryIdentityService;
import com.ytdinfo.inndoo.modules.activiti.utils.MessageUtil;
import com.ytdinfo.inndoo.modules.activiti.vo.ActPage;
import com.ytdinfo.inndoo.modules.activiti.vo.Assignee;
import com.ytdinfo.inndoo.modules.activiti.vo.HistoricTaskVo;
import com.ytdinfo.inndoo.modules.activiti.vo.TaskVo;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "??????????????????")
@RequestMapping("/activiti/actTask")
public class ActTaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private ActProcessService actProcessService;

    @Autowired
    private ActBusinessService actBusinessService;

    @Autowired
    private UserService userService;

    @Autowired
    private IHistoryIdentityService iHistoryIdentityService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private MessageUtil messageUtil;

    @RequestMapping(value = "/todoList", method = RequestMethod.GET)
    @ApiOperation(value = "????????????")
    public Result<Object> todoList(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) String categoryId,
                                   @RequestParam(required = false) Integer priority,
                                   @ModelAttribute SearchVo searchVo,
                                   @ModelAttribute PageVo pageVo){

        ActPage<TaskVo> page = new ActPage<TaskVo>();
        List<TaskVo> list = new ArrayList<>();

        String userId = securityUtil.getCurrUser().getId();
        TaskQuery query = taskService.createTaskQuery().taskCandidateOrAssigned(userId);

        // ???????????????
        if("createTime".equals(pageVo.getSort())&&"asc".equals(pageVo.getOrder())){
            query.orderByTaskCreateTime().asc();
        }else if("priority".equals(pageVo.getSort())&&"asc".equals(pageVo.getOrder())){
            query.orderByTaskPriority().asc();
        }else if("priority".equals(pageVo.getSort())&&"desc".equals(pageVo.getOrder())){
            query.orderByTaskPriority().desc();
        }else{
            query.orderByTaskCreateTime().desc();
        }
        if(StrUtil.isNotBlank(name)){
            query.taskNameLike("%"+name+"%");
        }
        if(StrUtil.isNotBlank(categoryId)){
            query.taskCategory(categoryId);
        }
        if(priority!=null){
            query.taskPriority(priority);
        }
        if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
            Date start = DateUtil.parse(searchVo.getStartDate());
            Date end = DateUtil.parse(searchVo.getEndDate());
            query.taskCreatedAfter(start);
            query.taskCreatedBefore(DateUtil.endOfDay(end));
        }

        page.setTotalElements(query.count());
        int first =  (pageVo.getPageNumber()-1) * pageVo.getPageSize();
        List<Task> taskList = query.listPage(first, pageVo.getPageSize());

        // ??????vo
        taskList.forEach(e -> {
            TaskVo tv = new TaskVo(e);

            // ???????????????
            if(StrUtil.isNotBlank(tv.getOwner())){
                tv.setOwner(userService.get(tv.getOwner()).getUsername());
            }
            List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(tv.getProcInstId());
            for(IdentityLink ik : identityLinks){
                // ???????????????
                if("starter".equals(ik.getType())&&StrUtil.isNotBlank(ik.getUserId())){
                    tv.setApplyer(userService.get(ik.getUserId()).getUsername());
                }
            }
            // ??????????????????
            ActProcess actProcess = actProcessService.get(tv.getProcDefId());
            if(actProcess!=null){
                tv.setProcessName(actProcess.getName());
                tv.setRouteName(actProcess.getRouteName());
            }
            // ????????????key
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(tv.getProcInstId()).singleResult();
            tv.setBusinessKey(pi.getBusinessKey());
            ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
            if(actBusiness!=null){
                tv.setTableId(actBusiness.getTableId());
            }

            list.add(tv);
        });
        page.setContent(list);
        return new ResultUtil<Object>().setData(page);
    }

    @RequestMapping(value = "/doneList", method = RequestMethod.GET)
    @ApiOperation(value = "????????????")
    public Result<Object> doneList(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) String categoryId,
                                   @RequestParam(required = false) Integer priority,
                                   @ModelAttribute SearchVo searchVo,
                                   @ModelAttribute PageVo pageVo){

        ActPage<HistoricTaskVo> page = new ActPage<HistoricTaskVo>();
        List<HistoricTaskVo> list = new ArrayList<>();

        String userId = securityUtil.getCurrUser().getId();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().or().taskCandidateUser(userId).
                taskAssignee(userId).endOr().finished();

        // ???????????????
        if("createTime".equals(pageVo.getSort())&&"asc".equals(pageVo.getOrder())){
            query.orderByHistoricTaskInstanceEndTime().asc();
        }else if("priority".equals(pageVo.getSort())&&"asc".equals(pageVo.getOrder())){
            query.orderByTaskPriority().asc();
        }else if("priority".equals(pageVo.getSort())&&"desc".equals(pageVo.getOrder())){
            query.orderByTaskPriority().desc();
        }else if("duration".equals(pageVo.getSort())&&"asc".equals(pageVo.getOrder())){
            query.orderByHistoricTaskInstanceDuration().asc();
        }else if("duration".equals(pageVo.getSort())&&"desc".equals(pageVo.getOrder())){
            query.orderByHistoricTaskInstanceDuration().desc();
        }else{
            query.orderByHistoricTaskInstanceEndTime().desc();
        }
        if(StrUtil.isNotBlank(name)){
            query.taskNameLike("%"+name+"%");
        }
        if(StrUtil.isNotBlank(categoryId)){
            query.taskCategory(categoryId);
        }
        if(priority!=null){
            query.taskPriority(priority);
        }
        if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
            Date start = DateUtil.parse(searchVo.getStartDate());
            Date end = DateUtil.parse(searchVo.getEndDate());
            query.taskCompletedAfter(start);
            query.taskCompletedBefore(DateUtil.endOfDay(end));
        }

        page.setTotalElements((long) query.list().size());
        int first =  (pageVo.getPageNumber()-1) * pageVo.getPageSize();
        List<HistoricTaskInstance> taskList = query.listPage(first, pageVo.getPageSize());
        // ??????vo
        taskList.forEach(e -> {
            HistoricTaskVo htv = new HistoricTaskVo(e);
            // ???????????????
            if(StrUtil.isNotBlank(htv.getOwner())){
                htv.setOwner(userService.get(htv.getOwner()).getUsername());
            }
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(htv.getProcInstId());
            for(HistoricIdentityLink hik : identityLinks){
                // ???????????????
                if("starter".equals(hik.getType())&&StrUtil.isNotBlank(hik.getUserId())){
                    htv.setApplyer(userService.get(hik.getUserId()).getUsername());
                }
            }
            // ??????????????????
            List<Comment> comments = taskService.getTaskComments(htv.getId(), "comment");
            if(comments!=null&&comments.size()>0){
                htv.setComment(comments.get(0).getFullMessage());
            }
            // ??????????????????
            ActProcess actProcess = actProcessService.get(htv.getProcDefId());
            if(actProcess!=null){
                htv.setProcessName(actProcess.getName());
                htv.setRouteName(actProcess.getRouteName());
            }
            // ????????????key
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(htv.getProcInstId()).singleResult();
            htv.setBusinessKey(hpi.getBusinessKey());
            ActBusiness actBusiness = actBusinessService.get(hpi.getBusinessKey());
            if(actBusiness!=null){
                htv.setTableId(actBusiness.getTableId());
            }

            list.add(htv);
        });
        page.setContent(list);
        return new ResultUtil<Object>().setData(page);
    }

    @RequestMapping(value = "/historicFlow/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????")
    public Result<Object> historicFlow(@ApiParam("????????????id") @PathVariable String id){

        List<HistoricTaskVo> list = new ArrayList<>();

        List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(id).orderByHistoricTaskInstanceEndTime().asc().list();

        // ??????vo
        taskList.forEach(e -> {
            HistoricTaskVo htv = new HistoricTaskVo(e);
            List<Assignee> assignees = new ArrayList<>();
            // ????????????????????????????????????????????????
            if(StrUtil.isNotBlank(htv.getAssignee())){
                String username = userService.get(htv.getAssignee()).getUsername();
                htv.setAssignee(username);
                assignees.add(new Assignee(username, true));
            }
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(e.getId());
            StringBuilder candidateBuilder = new StringBuilder();
            // ????????????????????????id
            String userId = iHistoryIdentityService.findUserIdByTypeAndTaskId(ActivitiConstant.EXECUTOR_TYPE, e.getId());
            for(HistoricIdentityLink hik : identityLinks){
                // ??????????????????????????????????????????????????????
                if("candidate".equals(hik.getType())&&StrUtil.isNotBlank(hik.getUserId())){
                    String username = userService.get(hik.getUserId()).getUsername();
                    Assignee assignee = new Assignee(username, false);
                    if(StrUtil.isNotBlank(userId)&&userId.equals(hik.getUserId())){
                        assignee.setIsExecutor(true);
                        username+="(????????????)";
                    }
                    if(StrUtil.isBlank(candidateBuilder.toString())){
                        candidateBuilder.append(username);
                    }else{
                        candidateBuilder.append("???" + username);
                    }
                    assignees.add(assignee);
                }
            }
            if(StrUtil.isBlank(htv.getAssignee())&&StrUtil.isNotBlank(candidateBuilder.toString())){
                htv.setAssignee(candidateBuilder.toString());
                htv.setAssignees(assignees);
            }
            // ??????????????????
            List<Comment> comments = taskService.getTaskComments(htv.getId(), "comment");
            if(comments!=null&&comments.size()>0){
                htv.setComment(comments.get(0).getFullMessage());
            }
            list.add(htv);
        });
        return new ResultUtil<Object>().setData(list);
    }

    @RequestMapping(value = "/pass", method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????")
    public Result<Object> pass(@ApiParam("??????id") @RequestParam String id,
                               @ApiParam("????????????id") @RequestParam String procInstId,
                               @ApiParam("?????????????????????") @RequestParam(required = false) String[] assignees,
                               @ApiParam("?????????") @RequestParam(required = false) Integer priority,
                               @ApiParam("????????????") @RequestParam(required = false) String comment,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail){

        if(StrUtil.isBlank(comment)){
            comment = "";
        }
        taskService.addComment(id, procInstId, comment);
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        Task task = taskService.createTaskQuery().taskId(id).singleResult();
        if(StrUtil.isNotBlank(task.getOwner())&&!("RESOLVED").equals(task.getDelegationState().toString())){
            // ???????????????????????? ???resolve
            taskService.resolveTask(id);
        }
        taskService.complete(id);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
        // ?????????????????????
        if(tasks!=null&&tasks.size()>0){
            for(Task t : tasks){
                for(String assignee : assignees){
                    taskService.addCandidateUser(t.getId(), assignee);
                    // ???????????????
                    messageUtil.sendActMessage(assignee, ActivitiConstant.MESSAGE_TODO_CONTENT, sendMessage, sendSms, sendEmail);
                }
                if(priority!=null){
                    taskService.setPriority(t.getId(), priority);
                }
            }
        } else {
            ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
            actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
            actBusiness.setResult(ActivitiConstant.RESULT_PASS);
            actBusinessService.update(actBusiness);
            // ???????????????
            messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_PASS_CONTENT, sendMessage, sendSms, sendEmail);
        }
        // ????????????????????????
        iHistoryIdentityService.insert(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()),
                ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, procInstId);
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/passAll/{ids}", method = RequestMethod.POST)
    @ApiOperation(value = "????????????")
    public Result<Object> passAll(@ApiParam("??????ids") @PathVariable String[] ids,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail){

        for(String id:ids){
            Task task = taskService.createTaskQuery().taskId(id).singleResult();
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            if(StrUtil.isNotBlank(task.getOwner())&&!("RESOLVED").equals(task.getDelegationState().toString())){
                // ???????????????????????? ???resolve
                taskService.resolveTask(id);
            }
            taskService.complete(id);
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            // ?????????????????????
            if(tasks!=null&&tasks.size()>0){
                for(Task t : tasks){
                    List<User> users = actProcessService.getNode(t.getTaskDefinitionKey()).getUsers();
                    for(User user : users){
                        taskService.addCandidateUser(t.getId(), user.getId());
                        // ???????????????
                        messageUtil.sendActMessage(user.getId(), ActivitiConstant.MESSAGE_TODO_CONTENT, sendMessage, sendSms, sendEmail);
                    }
                    taskService.setPriority(t.getId(), task.getPriority());
                }
            } else {
                ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
                actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
                actBusiness.setResult(ActivitiConstant.RESULT_PASS);
                actBusinessService.update(actBusiness);
                // ???????????????
                messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_PASS_CONTENT, sendMessage, sendSms, sendEmail);
            }
            // ????????????????????????
            iHistoryIdentityService.insert(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()),
                    ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, pi.getId());
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/listBackByProcInstId/{procInstId}", method = RequestMethod.GET)
    @ApiOperation(value = "????????????????????????")
    public Result<Object> listBackByProcInstId(@PathVariable String procInstId){

        List<HistoricTaskVo> list = new ArrayList<>();
        List<HistoricTaskInstance> taskInstanceList = historyService.createHistoricTaskInstanceQuery().processInstanceId(procInstId)
                .finished().list();

        taskInstanceList.forEach(e -> {
            HistoricTaskVo htv = new HistoricTaskVo(e);
            list.add(htv);
        });

        // ??????
        LinkedHashSet<String> set = new LinkedHashSet<String>(list.size());
        List<HistoricTaskVo> newList = new ArrayList<>();
        list.forEach(e->{
            if(set.add(e.getName())){
                newList.add(e);
            }
        });

        return new ResultUtil<Object>().setData(newList);
    }

    @RequestMapping(value = "/backToTask", method = RequestMethod.POST)
    @ApiOperation(value = "?????????????????????????????????????????????")
    public Result<Object> backToTask(@ApiParam("??????id") @RequestParam String id,
                                     @ApiParam("??????????????????key") @RequestParam String backTaskKey,
                                     @ApiParam("????????????id") @RequestParam String procInstId,
                                     @ApiParam("????????????id") @RequestParam String procDefId,
                                     @ApiParam("??????????????????") @RequestParam(required = false) String[] assignees,
                                     @ApiParam("?????????") @RequestParam(required = false) Integer priority,
                                     @ApiParam("????????????") @RequestParam(required = false) String comment,
                                     @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                     @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                     @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail){


        if(StrUtil.isBlank(comment)){
            comment = "";
        }
        taskService.addComment(id, procInstId, comment);
        // ??????????????????
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(procDefId);
        // ?????????????????????Activity
        ActivityImpl hisActivity = definition.findActivity(backTaskKey);
        // ????????????
        managementService.executeCommand(new JumpTask(procInstId, hisActivity.getId()));
        // ??????????????????????????????
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
        if(tasks!=null&&tasks.size()>0){
            tasks.forEach(e->{
                for(String assignee:assignees){
                    taskService.addCandidateUser(e.getId(), assignee);
                    // ???????????????
                    messageUtil.sendActMessage(assignee, ActivitiConstant.MESSAGE_TODO_CONTENT, sendMessage, sendSms, sendEmail);
                }
                if(priority!=null){
                    taskService.setPriority(e.getId(), priority);
                }
            });
        }
        // ????????????????????????
        iHistoryIdentityService.insert(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()),
                ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, procInstId);
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    public class JumpTask implements Command<ExecutionEntity> {

        private String procInstId;
        private String activityId;

        public JumpTask(String procInstId, String activityId) {
            this.procInstId = procInstId;
            this.activityId = activityId;
        }

        @Override
        public ExecutionEntity execute(CommandContext commandContext) {

            ExecutionEntity executionEntity = commandContext.getExecutionEntityManager().findExecutionById(procInstId);
            executionEntity.destroyScope("backed");
            ProcessDefinitionImpl processDefinition = executionEntity.getProcessDefinition();
            ActivityImpl activity = processDefinition.findActivity(activityId);
            executionEntity.executeActivity(activity);

            return executionEntity;
        }
    }

    @RequestMapping(value = "/back", method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????????????????")
    public Result<Object> back(@ApiParam("??????id") @RequestParam String id,
                               @ApiParam("????????????id") @RequestParam String procInstId,
                               @ApiParam("????????????") @RequestParam(required = false) String comment,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail){


        if(StrUtil.isBlank(comment)){
            comment = "";
        }
        taskService.addComment(id, procInstId, comment);
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        // ??????????????????
        runtimeService.deleteProcessInstance(procInstId, "backed");
        ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
        actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
        actBusiness.setResult(ActivitiConstant.RESULT_FAIL);
        actBusinessService.update(actBusiness);
        // ???????????????
        messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_BACK_CONTENT, sendMessage, sendSms, sendEmail);
        // ????????????????????????
        iHistoryIdentityService.insert(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()),
                ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, procInstId);
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/backAll/{procInstIds}", method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????")
    public Result<Object> backAll(@ApiParam("????????????ids") @PathVariable String[] procInstIds,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail){

        for(String procInstId:procInstIds){
            // ????????????????????????
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
            tasks.forEach(t->{
                iHistoryIdentityService.insert(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()),
                        ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), t.getId(), procInstId);
            });
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
            // ??????????????????
            runtimeService.deleteProcessInstance(procInstId, ActivitiConstant.BACKED_FLAG);
            ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
            actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
            actBusiness.setResult(ActivitiConstant.RESULT_FAIL);
            actBusinessService.update(actBusiness);
            // ???????????????
            messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_BACK_CONTENT, sendMessage, sendSms, sendEmail);
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/delegate", method = RequestMethod.POST)
    @ApiOperation(value = "??????????????????")
    public Result<Object> delegate(@ApiParam("??????id") @RequestParam String id,
                                   @ApiParam("????????????id") @RequestParam String userId,
                                   @ApiParam("????????????id") @RequestParam String procInstId,
                                   @ApiParam("????????????") @RequestParam(required = false) String comment,
                                   @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                   @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                   @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail){

        if(StrUtil.isBlank(comment)){
            comment = "";
        }
        taskService.addComment(id, procInstId, comment);
        taskService.delegateTask(id, userId);
        taskService.setOwner(id, securityUtil.getCurrUser().getId());
        // ???????????????
        messageUtil.sendActMessage(userId, ActivitiConstant.MESSAGE_DELEGATE_CONTENT, sendMessage, sendSms, sendEmail);
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "????????????")
    public Result<Object> delete(@ApiParam("??????id") @PathVariable String[] ids,
                                 @ApiParam("??????") @RequestParam(required = false) String reason){

        if(StrUtil.isBlank(reason)){
            reason = "";
        }
        for(String id : ids){
            taskService.deleteTask(id, reason);
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/deleteHistoric/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "??????????????????")
    public Result<Object> deleteHistoric(@ApiParam("??????id") @PathVariable String[] ids){

        for(String id : ids){
            historyService.deleteHistoricTaskInstance(id);
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }
}
