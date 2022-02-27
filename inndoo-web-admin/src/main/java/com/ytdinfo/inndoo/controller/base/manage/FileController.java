package com.ytdinfo.inndoo.controller.base.manage;

import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.base.entity.File;
import com.ytdinfo.inndoo.modules.base.service.FileService;
import com.ytdinfo.inndoo.modules.base.vo.OssSetting;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;


/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "文件管理管理接口")
@RequestMapping("/base/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private QiniuUtil qiniuUtil;

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private TencentOssUtil tencentOssUtil;

    @Autowired
    private FileUtil fileUtil;

    @PersistenceContext
    private EntityManager entityManager;

    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    public Result<Page<File>> listByCondition(@ModelAttribute File file,
                                          @ModelAttribute SearchVo searchVo,
                                          @ModelAttribute PageVo pageVo){

        Page<File> page = fileService.findByCondition(file, searchVo, PageUtil.initPage(pageVo));
        return new ResultUtil<Page<File>>().setData(page);
    }

    @RequestMapping(value = "/copy", method = RequestMethod.POST)
    @ApiOperation(value = "文件复制")
    public Result<Object> copy(@RequestParam String id,
                               @RequestParam String key) {

        File file = fileService.get(id);
        String toKey = "副本_" + key;
        String newUrl = "";
        if(file.getLocation()==null){
            return new ResultUtil<Object>().setErrorMsg("存储位置未知");
        }
        if(CommonConstant.OSS_QINIU.equals(file.getLocation())){
            newUrl = qiniuUtil.copyFile(key, toKey);
        }else if(CommonConstant.OSS_ALI.equals(file.getLocation())){
            newUrl = aliOssUtil.copyFile(key, toKey);
        }else if(CommonConstant.OSS_TENCENT.equals(file.getLocation())){
            newUrl = tencentOssUtil.copyFile(key, toKey);
        }else if(CommonConstant.OSS_LOCAL.equals(file.getLocation())){
            newUrl = fileUtil.copyFile(file.getUrl(), toKey);
        }
        File newFile = new File();
        newFile.setName(file.getName());
        newFile.setFKey(toKey);
        newFile.setSize(file.getSize());
        newFile.setType(file.getType());
        newFile.setLocation(file.getLocation());
        newFile.setUrl(newUrl);
        fileService.save(newFile);
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/rename", method = RequestMethod.POST)
    @ApiOperation(value = "文件重命名")
    public Result<Object> upload(@RequestParam String id,
                                 @RequestParam String key,
                                 @RequestParam String newKey,
                                 @RequestParam String newName) {

        File file = fileService.get(id);
        if(file.getLocation()==null){
            return new ResultUtil<Object>().setErrorMsg("存储位置未知");
        }
        String newUrl = "";
        if(!key.equals(newKey)){
            if(CommonConstant.OSS_QINIU.equals(file.getLocation())){
                newUrl = qiniuUtil.renameFile(key, newKey);
            }else if(CommonConstant.OSS_ALI.equals(file.getLocation())){
                newUrl = aliOssUtil.renameFile(key, newKey);
            }else if(CommonConstant.OSS_TENCENT.equals(file.getLocation())){
                newUrl = tencentOssUtil.renameFile(key, newKey);
            }else if(CommonConstant.OSS_LOCAL.equals(file.getLocation())){
                newUrl = fileUtil.renameFile(file.getUrl(), newKey);
            }
        }
        file.setName(newName);
        file.setFKey(newKey);
        if(!key.equals(newKey)) {
            file.setUrl(newUrl);
        }
        fileService.update(file);
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/delete/{ids}", method = RequestMethod.DELETE)
    @ApiOperation(value = "文件删除")
    public Result<Object> delete(@PathVariable String[] ids) {

        for(String id : ids){
            File file = fileService.get(id);
            if(file.getLocation()==null){
                return new ResultUtil<Object>().setErrorMsg("存储位置未知");
            }
            if(CommonConstant.OSS_QINIU.equals(file.getLocation())){
                qiniuUtil.deleteFile(file.getFKey());
            }else if(CommonConstant.OSS_ALI.equals(file.getLocation())){
                aliOssUtil.deleteFile(file.getFKey());
            }else if(CommonConstant.OSS_TENCENT.equals(file.getLocation())){
                tencentOssUtil.deleteFile(file.getFKey());
            }else if(CommonConstant.OSS_LOCAL.equals(file.getLocation())){
                fileUtil.deleteFile(file.getUrl());
            }
            fileService.delete(id);
        }
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/view/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "本地存储预览文件")
    @CrossOrigin
    public void view(@PathVariable String id, HttpServletResponse response) throws IOException {

        File file = fileService.get(id);
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(file.getFKey(), "UTF-8"));
        response.addHeader("Content-Length", file.getSize().toString());
        response.setContentType("application/octet-stream;charset=UTF-8");
        fileUtil.view(file.getUrl(), response);
    }
}
