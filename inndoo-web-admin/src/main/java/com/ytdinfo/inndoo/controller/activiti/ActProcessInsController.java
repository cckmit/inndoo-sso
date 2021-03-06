package com.ytdinfo.inndoo.controller.activiti;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.ActivitiConstant;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.config.activiti.ActivitiExtendProperties;
import com.ytdinfo.inndoo.modules.activiti.entity.ActBusiness;
import com.ytdinfo.inndoo.modules.activiti.entity.ActProcess;
import com.ytdinfo.inndoo.modules.activiti.service.ActBusinessService;
import com.ytdinfo.inndoo.modules.activiti.service.ActProcessService;
import com.ytdinfo.inndoo.modules.activiti.vo.ActPage;
import com.ytdinfo.inndoo.modules.activiti.vo.HistoricProcessInsVo;
import com.ytdinfo.inndoo.modules.activiti.vo.ProcessInsVo;
import com.ytdinfo.inndoo.modules.activiti.vo.ProcessNodeVo;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "????????????????????????")
@RequestMapping("/activiti/actProcess")
public class ActProcessInsController {

    @Autowired
    private ActivitiExtendProperties properties;

    @Autowired
    private ActProcessService actProcessService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ActBusinessService actBusinessService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @RequestMapping(value = "/listRunningProcess", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????????????????")
    public Result<Object> listRunningProcess(@RequestParam(required = false) String name,
                                            @RequestParam(required = false) String categoryId,
                                            @RequestParam(required = false) String key,
                                            @ModelAttribute PageVo pageVo){

        ActPage<ProcessInsVo> page = new ActPage<ProcessInsVo>();
        List<ProcessInsVo> list = new ArrayList<>();

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
                .orderByProcessInstanceId().desc();

        if(StrUtil.isNotBlank(name)){
            query.processInstanceNameLike("%"+name+"%");
        }
        if(StrUtil.isNotBlank(categoryId)){
            query.processDefinitionCategory(categoryId);
        }
        if(StrUtil.isNotBlank(key)) {
            query.processDefinitionKey(key);
        }

        page.setTotalElements(query.count());
        int first =  (pageVo.getPageNumber()-1) * pageVo.getPageSize();
        List<ProcessInstance> processInstanceList = query.listPage(first, pageVo.getPageSize());
        processInstanceList.forEach(e -> {
            list.add(new ProcessInsVo(e));
        });
        list.forEach(e -> {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(e.getId());
            for(HistoricIdentityLink hik : identityLinks){
                // ???????????????
                if("starter".equals(hik.getType())&&StrUtil.isNotBlank(hik.getUserId())){
                    e.setApplyer(userService.get(hik.getUserId()).getUsername());
                }
            }
            // ??????????????????
            Task task = taskService.createTaskQuery().processInstanceId(e.getId()).singleResult();
            if(task!=null){
                e.setCurrTaskName(task.getName());
            }
            // ????????????????????????
            ActProcess actProcess = actProcessService.get(e.getProcDefId());
            if(actProcess!=null){
                e.setRouteName(actProcess.getRouteName());
            }
            // ???????????????id
            ActBusiness actBusiness = actBusinessService.get(e.getBusinessKey());
            if(actBusiness!=null){
                e.setTableId(actBusiness.getTableId());
            }
        });
        page.setContent(list);
        return new ResultUtil<Object>().setData(page);
    }

    @RequestMapping(value = "/listFinishedProcess", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????????????????")
    public Result<Object> queryFinishedProcess(@RequestParam(required = false) String name,
                                             @RequestParam(required = false) String categoryId,
                                             @RequestParam(required = false) String key,
                                             @ModelAttribute SearchVo searchVo,
                                             @ModelAttribute PageVo pageVo){

        ActPage<HistoricProcessInsVo> page = new ActPage<HistoricProcessInsVo>();
        List<HistoricProcessInsVo> list = new ArrayList<>();

        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().finished().
                orderByProcessInstanceEndTime().desc();

        if(StrUtil.isNotBlank(name)){
            query.processInstanceNameLike("%"+name+"%");
        }
        if(StrUtil.isNotBlank(categoryId)){
            query.processDefinitionCategory(categoryId);
        }
        if(StrUtil.isNotBlank(key)) {
            query.processDefinitionKey(key);
        }
        if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
            Date start = DateUtil.parse(searchVo.getStartDate());
            Date end = DateUtil.parse(searchVo.getEndDate());
            query.finishedAfter(start);
            query.finishedBefore(DateUtil.endOfDay(end));
        }

        page.setTotalElements(query.count());
        int first =  (pageVo.getPageNumber()-1) * pageVo.getPageSize();
        List<HistoricProcessInstance> processInstanceList = query.listPage(first, pageVo.getPageSize());
        processInstanceList.forEach(e -> {
            list.add(new HistoricProcessInsVo(e));
        });
        list.forEach(e -> {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(e.getId());
            for(HistoricIdentityLink hik : identityLinks){
                // ???????????????
                if("starter".equals(hik.getType())&&StrUtil.isNotBlank(hik.getUserId())){
                    e.setApplyer(userService.get(hik.getUserId()).getUsername());
                }
            }
            // ????????????????????????
            ActProcess actProcess = actProcessService.get(e.getProcDefId());
            if(actProcess!=null){
                e.setRouteName(actProcess.getRouteName());
            }
            // ???????????????id?????????
            ActBusiness actBusiness = actBusinessService.get(e.getBusinessKey());
            if(actBusiness!=null){
                e.setTableId(actBusiness.getTableId());
                String reason = e.getDeleteReason();
                if(reason==null){
                    e.setResult(ActivitiConstant.RESULT_PASS);
                }else if(reason.contains(ActivitiConstant.CANCEL_PRE)){
                    e.setResult(ActivitiConstant.RESULT_CANCEL);
                    if(reason.length()>9){
                        e.setDeleteReason(reason.substring(9));
                    }else{
                        e.setDeleteReason("");
                    }
                }else if(ActivitiConstant.BACKED_FLAG.equals(reason)){
                    e.setResult(ActivitiConstant.RESULT_FAIL);
                    e.setDeleteReason("");
                }else if(reason.contains(ActivitiConstant.DELETE_PRE)){
                    e.setResult(ActivitiConstant.RESULT_DELETED);
                    if(reason.length()>8){
                        e.setDeleteReason(reason.substring(8));
                    }else{
                        e.setDeleteReason("");
                    }
                }else{
                    e.setResult(ActivitiConstant.RESULT_PASS);
                }
            }
        });
        page.setContent(list);
        return new ResultUtil<Object>().setData(page);
    }

    @RequestMapping(value = "/queryFirstNode/{procDefId}", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????id???????????????????????????")
    public Result<ProcessNodeVo> getFirstNode(@ApiParam("????????????id") @PathVariable String procDefId){

        ProcessNodeVo node = actProcessService.getFirstNode(procDefId);
        return new ResultUtil<ProcessNodeVo>().setData(node);
    }

    @RequestMapping(value = "/queryNextNode/{procInstId}", method = RequestMethod.GET)
    @ApiOperation(value = "???????????????????????????????????????")
    public Result<ProcessNodeVo> getNextNode(@ApiParam("????????????id") @PathVariable String procInstId){

        ProcessNodeVo node = actProcessService.getNextNode(procInstId);
        return new ResultUtil<ProcessNodeVo>().setData(node);
    }

    @RequestMapping(value = "/queryNode/{nodeId}", method = RequestMethod.GET)
    @ApiOperation(value = "????????????nodeId???????????????")
    public Result<ProcessNodeVo> getNode(@ApiParam("??????nodeId") @PathVariable String nodeId){

        ProcessNodeVo node = actProcessService.getNode(nodeId);
        return new ResultUtil<ProcessNodeVo>().setData(node);
    }

    @RequestMapping(value = "/queryHighlightImg/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "???????????????????????????")
    public void getHighlightImg(@ApiParam("????????????id") @PathVariable String id,
                                HttpServletResponse response){

        InputStream inputStream = null;
        ProcessInstance pi = null;
        String picName = "";
        // ????????????
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(id).singleResult();
        if (hpi.getEndTime() != null) {
            // ??????????????????????????????
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(hpi.getProcessDefinitionId()).singleResult();
            picName = pd.getDiagramResourceName();
            inputStream = repositoryService.getResourceAsStream(pd.getDeploymentId(), pd.getDiagramResourceName());
        } else {
            pi = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());


            List<String> highLightedActivities = new ArrayList<String>();
            // ??????????????????
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(id).list();
            for (Task task : tasks) {
                highLightedActivities.add(task.getTaskDefinitionKey());
            }

            List<String> highLightedFlows = new ArrayList<String>();
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            inputStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivities, highLightedFlows,
                    properties.getActivityFontName(), properties.getLabelFontName(), properties.getLabelFontName(),null, 1.0);
            picName = pi.getName()+".png";
        }
        try {
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(picName, "UTF-8"));
            byte[] b = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(b, 0, 1024)) != -1) {
                response.getOutputStream().write(b, 0, len);
            }
            response.flushBuffer();
        } catch (IOException e) {
            log.error(e.toString());
            throw new InndooException("????????????????????????");
        }
    }

    @RequestMapping(value = "/updateInsStatus", method = RequestMethod.POST)
    @ApiOperation(value = "???????????????????????????")
    public Result<Object> updateStatus(@ApiParam("????????????id") @RequestParam String id,
                                       @RequestParam Integer status){

        if(ActivitiConstant.PROCESS_STATUS_ACTIVE.equals(status)){
            runtimeService.activateProcessInstanceById(id);
        }else if(ActivitiConstant.PROCESS_STATUS_SUSPEND.equals(status)){
            runtimeService.suspendProcessInstanceById(id);
        }

        return new ResultUtil<Object>().setData("????????????");
    }

    @RequestMapping(value = "/delInsByIds/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "??????id????????????????????????")
    public Result<Object> delInsByIds(@PathVariable String[] ids,
                                      @RequestParam(required = false) String reason){

        if(StrUtil.isBlank(reason)){
            reason = "";
        }
        for(String id : ids){
            // ????????????????????????
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
            ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
            actBusiness.setStatus(ActivitiConstant.STATUS_TO_APPLY);
            actBusiness.setResult(ActivitiConstant.RESULT_TO_SUBMIT);
            actBusinessService.update(actBusiness);
            runtimeService.deleteProcessInstance(id, ActivitiConstant.DELETE_PRE+reason);
        }
        return new ResultUtil<Object>().setData("????????????");
    }

    @RequestMapping(value = "/delHistoricInsByIds/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "??????id????????????????????????")
    public Result<Object> delHistoricInsByIds(@PathVariable String[] ids){

        for(String id : ids){
            historyService.deleteHistoricProcessInstance(id);
        }
        return new ResultUtil<Object>().setData("????????????");
    }

}
