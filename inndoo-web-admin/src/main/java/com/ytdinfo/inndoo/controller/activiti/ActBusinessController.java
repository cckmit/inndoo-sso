package com.ytdinfo.inndoo.controller.activiti;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.common.constant.ActivitiConstant;
import com.ytdinfo.inndoo.common.utils.PageUtil;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.activiti.entity.ActBusiness;
import com.ytdinfo.inndoo.modules.activiti.entity.ActProcess;
import com.ytdinfo.inndoo.modules.activiti.service.ActBusinessService;
import com.ytdinfo.inndoo.modules.activiti.service.ActProcessService;
import com.ytdinfo.inndoo.modules.activiti.service.mybatis.IActService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "业务申请管理接口")
@RequestMapping("/activiti/actBusiness")
public class ActBusinessController {

    @Autowired
    private ActBusinessService actBusinessService;

    @Autowired
    private IActService iActService;

    @Autowired
    private ActProcessService actProcessService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RuntimeService runtimeService;

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取申请列表")
    public Result<Page<ActBusiness>> listByCondition(@ModelAttribute ActBusiness actBusiness,
                                                    @ModelAttribute SearchVo searchVo,
                                                    @ModelAttribute PageVo pageVo){

        Page<ActBusiness> page = actBusinessService.findByCondition(actBusiness, searchVo, PageUtil.initPage(pageVo));
        page.getContent().forEach(e -> {
            if(StrUtil.isNotBlank(e.getProcDefId())){
                ActProcess actProcess = actProcessService.get(e.getProcDefId());
                e.setRouteName(actProcess.getRouteName());
                e.setProcessName(actProcess.getName());
            }
            if(ActivitiConstant.STATUS_DEALING.equals(e.getStatus())){
                // 关联当前任务
                Task task = taskService.createTaskQuery().processInstanceId(e.getProcInstId()).singleResult();
                if(task!=null){
                    e.setCurrTaskName(task.getName());
                }
            }
        });
        return new ResultUtil<Page<ActBusiness>>().setData(page);
    }

    @RequestMapping(value = "/apply", method = RequestMethod.POST)
    @ApiOperation(value = "提交申请 启动流程")
    public Result<Object> apply(@ModelAttribute ActBusiness act){

        ActBusiness actBusiness = actBusinessService.get(act.getId());
        act.setTableId(actBusiness.getTableId());
        String processInstanceId = actProcessService.startProcess(act);
        actBusiness.setProcInstId(processInstanceId);
        actBusiness.setStatus(ActivitiConstant.STATUS_DEALING);
        actBusiness.setResult(ActivitiConstant.RESULT_DEALING);
        actBusiness.setApplyTime(new Date());
        actBusinessService.update(actBusiness);
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/start", method = RequestMethod.POST)
    @ApiOperation(value = "流程选择组件启动流程")
    public Result<Object> start(@ModelAttribute ActBusiness act){

        ActBusiness actBusiness = actBusinessService.get(act.getId());
        act.setTableId(actBusiness.getTableId());
        String processInstanceId = actProcessService.startProcess(act);
        actBusiness.setProcDefId(act.getProcDefId());
        actBusiness.setTitle(act.getTitle());
        actBusiness.setProcInstId(processInstanceId);
        actBusiness.setStatus(ActivitiConstant.STATUS_DEALING);
        actBusiness.setResult(ActivitiConstant.RESULT_DEALING);
        actBusiness.setApplyTime(new Date());
        actBusinessService.update(actBusiness);
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    @ApiOperation(value = "撤回申请")
    public Result<Object> cancel(@RequestParam String id,
                                 @RequestParam String procInstId,
                                 @RequestParam(required = false) String reason){

        if(StrUtil.isBlank(reason)){
            reason = "";
        }
        runtimeService.deleteProcessInstance(procInstId, "canceled-"+reason);
        ActBusiness actBusiness = actBusinessService.get(id);
        actBusiness.setStatus(ActivitiConstant.STATUS_TO_APPLY);
        actBusiness.setResult(ActivitiConstant.RESULT_TO_SUBMIT);
        actBusinessService.update(actBusiness);
        return new ResultUtil<Object>().setSuccessMsg("操作成功");
    }

    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "通过id删除草稿状态申请")
    public Result<Object> batchDelete(@PathVariable String[] ids){

        for(String id : ids){
            ActBusiness actBusiness = actBusinessService.get(id);
            if(!ActivitiConstant.STATUS_TO_APPLY.equals(actBusiness.getStatus())){
                return new ResultUtil<Object>().setErrorMsg("删除失败, 仅能删除草稿状态的申请");
            }
            // 删除关联业务表
            ActProcess actProcess = actProcessService.get(actBusiness.getProcDefId());
            iActService.deleteBusiness(actProcess.getBusinessTable(), actBusiness.getTableId());
            actBusinessService.delete(id);
        }
        return new ResultUtil<Object>().setSuccessMsg("删除成功");
    }
}
