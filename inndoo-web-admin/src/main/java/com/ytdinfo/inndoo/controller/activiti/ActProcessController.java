package com.ytdinfo.inndoo.controller.activiti;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ytdinfo.inndoo.common.constant.ActivitiConstant;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.activiti.entity.ActCategory;
import com.ytdinfo.inndoo.modules.activiti.entity.ActModel;
import com.ytdinfo.inndoo.modules.activiti.entity.ActNode;
import com.ytdinfo.inndoo.modules.activiti.entity.ActProcess;
import com.ytdinfo.inndoo.modules.activiti.service.*;
import com.ytdinfo.inndoo.modules.activiti.vo.ProcessNodeVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "????????????????????????")
@RequestMapping("/activiti/actProcess")
public class ActProcessController {

    @Autowired
    private ActModelService actModelService;

    @Autowired
    private ActProcessService actProcessService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ActNodeService actNodeService;

    @Autowired
    private ActCategoryService actCategoryService;

    @Autowired
    private ActBusinessService actBusinessService;


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????????????????")
    public Result<Page<ActProcess>> listByCondition(@ApiParam("????????????????????????") @RequestParam(required = false) Boolean showLatest,
                                                   @ModelAttribute ActProcess actProcess,
                                                   @ModelAttribute SearchVo searchVo,
                                                   @ModelAttribute PageVo pageVo){

        Page<ActProcess> page = actProcessService.findByCondition(showLatest, actProcess, searchVo, PageUtil.initPage(pageVo));
        page.getContent().forEach(e -> {
            if(StrUtil.isNotBlank(e.getCategoryId())){
                ActCategory category = actCategoryService.get(e.getCategoryId());
                if(category!=null){
                    e.setCategoryTitle(category.getTitle());
                }
            }
        });
        return new ResultUtil<Page<ActProcess>>().setData(page);
    }

    @RequestMapping(value = "/listByKey/{key}", method = RequestMethod.GET)
    @ApiOperation(value = "??????key??????????????????")
    public Result<ActProcess> listByCondition(@PathVariable String key){

        ActProcess actProcess = actProcessService.findByProcessKeyAndLatest(key, true);
        return new ResultUtil<ActProcess>().setData(actProcess);
    }

    @RequestMapping(value = "/updateInfo", method = RequestMethod.POST)
    @ApiOperation(value = "???????????????????????????")
    public Result<Object> updateInfo(@ModelAttribute ActProcess actProcess){

        ProcessDefinition pd = repositoryService.getProcessDefinition(actProcess.getId());
        if(pd==null){
            return new ResultUtil<Object>().setErrorMsg("?????????????????????");
        }
        if(StrUtil.isNotBlank(actProcess.getCategoryId())){
            repositoryService.setProcessDefinitionCategory(actProcess.getId(), actProcess.getCategoryId());
            repositoryService.setDeploymentCategory(pd.getDeploymentId(), actProcess.getCategoryId());
        }
        actProcessService.update(actProcess);
        return new ResultUtil<Object>().setData("????????????");
    }

    @RequestMapping(value = "/updateStatus", method = RequestMethod.POST)
    @ApiOperation(value = "???????????????????????????")
    public Result<Object> updateStatus(@ApiParam("????????????id") @RequestParam String id,
                                       @RequestParam Integer status){

        if(ActivitiConstant.PROCESS_STATUS_ACTIVE.equals(status)){
            repositoryService.activateProcessDefinitionById(id, true, new Date());
        }else if(ActivitiConstant.PROCESS_STATUS_SUSPEND.equals(status)){
            repositoryService.suspendProcessDefinitionById(id, true, new Date());
        }

        ActProcess actProcess = actProcessService.get(id);
        actProcess.setStatus(status);
        actProcessService.update(actProcess);
        return new ResultUtil<Object>().setData("????????????");
    }

    @RequestMapping(value = "/export", method = RequestMethod.GET)
    @ApiOperation(value = "????????????????????????")
    public void exportResource(@ApiParam("????????????id") @RequestParam String id,
                               @RequestParam Integer type,
                               HttpServletResponse response){

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(id).singleResult();

        String resourceName = "";
        if (ActivitiConstant.RESOURCE_TYPE_XML.equals(type)) {
            resourceName = pd.getResourceName();
        } else if (ActivitiConstant.RESOURCE_TYPE_IMAGE.equals(type)) {
            resourceName = pd.getDiagramResourceName();
        }
        InputStream inputStream = repositoryService.getResourceAsStream(pd.getDeploymentId(),
                resourceName);

        try {
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(resourceName, "UTF-8"));
            byte[] b = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(b, 0, 1024)) != -1) {
                response.getOutputStream().write(b, 0, len);
            }
            response.flushBuffer();
        } catch (IOException e) {
            log.error(e.toString());
            throw new InndooException("??????????????????????????????");
        }
    }

    @RequestMapping(value = "/convertToModel/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????")
    public Result<Object> convertToModel(@ApiParam("????????????id") @PathVariable String id){

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(id).singleResult();
        InputStream bpmnStream = repositoryService.getResourceAsStream(pd.getDeploymentId(), pd.getResourceName());
        ActProcess actProcess = actProcessService.get(id);

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            InputStreamReader in = new InputStreamReader(bpmnStream, "UTF-8");
            XMLStreamReader xtr = xif.createXMLStreamReader(in);
            BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
            BpmnJsonConverter converter = new BpmnJsonConverter();

            ObjectNode modelNode = converter.convertToJson(bpmnModel);
            Model modelData = repositoryService.newModel();
            modelData.setKey(pd.getKey());
            modelData.setName(pd.getResourceName());

            ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, actProcess.getName());
            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, modelData.getVersion());
            modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, actProcess.getDescription());
            modelData.setMetaInfo(modelObjectNode.toString());

            repositoryService.saveModel(modelData);
            repositoryService.addModelEditorSource(modelData.getId(), modelNode.toString().getBytes("utf-8"));

            // ??????????????????????????????
            ActModel actModel = new ActModel();
            actModel.setId(modelData.getId());
            actModel.setName(modelData.getName());
            actModel.setModelKey(modelData.getKey());
            actModel.setDescription(actProcess.getDescription());
            actModel.setVersion(modelData.getVersion());
            actModelService.save(actModel);
        }catch (Exception e){
            log.error(e.toString());
            return new ResultUtil<Object>().setErrorMsg("???????????????????????????");
        }
        return new ResultUtil<Object>().setData("????????????");
    }

    @RequestMapping(value = "/queryProcessNode/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????id??????????????????")
    public Result<List<ProcessNodeVo>> queryProcessNode(@ApiParam("????????????id") @PathVariable String id){

        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);

        List<ProcessNodeVo> list = new ArrayList<>();

        List<Process> processes = bpmnModel.getProcesses();
        if(processes==null||processes.size()==0){
            return new ResultUtil<List<ProcessNodeVo>>().setData(null);
        }
        for(Process process : processes){
            Collection<FlowElement> elements = process.getFlowElements();
            for(FlowElement element : elements){
                ProcessNodeVo node = new ProcessNodeVo();
                node.setId(element.getId());
                node.setTitle(element.getName());
                if(element instanceof StartEvent){
                    // ????????????
                    node.setType(ActivitiConstant.NODE_TYPE_START);
                }else if(element instanceof UserTask){
                    // ????????????
                    node.setType(ActivitiConstant.NODE_TYPE_TASK);
                    // ??????????????????
                    node.setUsers(actNodeService.findUserByNodeId(element.getId()));
                    // ??????????????????
                    node.setRoles(actNodeService.findRoleByNodeId(element.getId()));
                    // ??????????????????
                    node.setDepartments(actNodeService.findDepartmentByNodeId(element.getId()));
                }else if(element instanceof EndEvent){
                    // ??????
                    node.setType(ActivitiConstant.NODE_TYPE_END);
                }else{
                    // ???????????????????????????
                    continue;
                }
                list.add(node);
            }
        }
        return new ResultUtil<List<ProcessNodeVo>>().setData(list);
    }

    @RequestMapping(value = "/updateNodeUser", method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????")
    public Result<Object> updateNodeUser(@RequestParam String nodeId,
                                       @RequestParam(required = false) String[] userIds,
                                       @RequestParam(required = false) String[] roleIds,
                                       @RequestParam(required = false) String[] departmentIds){

        // ?????????????????????
        actNodeService.deleteByNodeId(nodeId);
        // ???????????????
        for(String userId : userIds){
            ActNode actNode = new ActNode();
            actNode.setNodeId(nodeId);
            actNode.setRelateId(userId);
            actNode.setType(ActivitiConstant.NODE_USER);
            actNodeService.save(actNode);
        }
        // ???????????????
        for(String roleId : roleIds){
            ActNode actNode = new ActNode();
            actNode.setNodeId(nodeId);
            actNode.setRelateId(roleId);
            actNode.setType(ActivitiConstant.NODE_ROLE);
            actNodeService.save(actNode);
        }
        // ???????????????
        for(String departmentId : departmentIds){
            ActNode actNode = new ActNode();
            actNode.setNodeId(nodeId);
            actNode.setRelateId(departmentId);
            actNode.setType(ActivitiConstant.NODE_DEPARTMENT);
            actNodeService.save(actNode);
        }
        return new ResultUtil<Object>().setSuccessMsg("????????????");
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "??????id????????????")
    public Result<Object> batchDelete(@PathVariable String[] ids){

        for(String id : ids){
            if(CollectionUtil.isNotEmpty(actBusinessService.findByProcDefId(id))){
                return new ResultUtil<Object>().setErrorMsg("?????????????????????????????????????????????");
            }
            ActProcess actProcess = actProcessService.get(id);
            // ?????????????????????????????? ??????????????????
            if(actProcess.getVersion()==1){
                deleteNodeUsers(id);
            }
            // ????????????
            repositoryService.deleteDeployment(actProcess.getDeploymentId(), true);
            actProcessService.delete(id);
            // ??????????????????
            actProcessService.setLatestByProcessKey(actProcess.getProcessKey());
        }
        return new ResultUtil<Object>().setData("????????????");
    }

    public void deleteNodeUsers(String processId){

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processId);
        List<Process> processes = bpmnModel.getProcesses();
        for(Process process : processes){
            Collection<FlowElement> elements = process.getFlowElements();
            for(FlowElement element : elements) {
                actNodeService.deleteByNodeId(element.getId());
            }
        }
    }
}
