package com.ytdinfo.inndoo.modules.base.serviceimpl;

import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.constant.SettingConstant;
import com.ytdinfo.inndoo.common.limit.RedisRaterLimiter;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.base.dao.FileDao;
import com.ytdinfo.inndoo.modules.base.entity.File;
import com.ytdinfo.inndoo.modules.base.service.FileService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.inndoo.modules.base.vo.OssSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 文件管理接口实现
 * @author Exrick
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private RedisRaterLimiter redisRaterLimiter;

    @Autowired
    private QiniuUtil qiniuUtil;

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private TencentOssUtil tencentOssUtil;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private IpInfoUtil ipInfoUtil;

    @XxlConf("matrix.inndoo.upload.serviceprovider")
    private String serviceProvider;

    @Autowired
    private FileDao fileDao;


    @Override
    public FileDao getRepository() {
        return fileDao;
    }

    @Override
    public Page<File> findByCondition(File file, SearchVo searchVo, Pageable pageable) {

        return fileDao.findAll(new Specification<File>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<File> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                Path<String> nameField = root.get("name");
                Path<String> fKeyField = root.get("fKey");
                Path<String> typeField = root.get("type");
                Path<Integer> locationField = root.get("location");
                Path<Date> createTimeField=root.get("createTime");

                List<Predicate> list = new ArrayList<Predicate>();

                //模糊搜素
                if(StrUtil.isNotBlank(file.getName())){
                    list.add(cb.like(nameField,'%'+file.getName()+'%'));
                }
                if(StrUtil.isNotBlank(file.getFKey())){
                    list.add(cb.like(fKeyField,'%'+file.getFKey()+'%'));
                }
                if(StrUtil.isNotBlank(file.getType())){
                    list.add(cb.like(typeField,'%'+file.getType()+'%'));
                }

                if(file.getLocation()!=null){
                    list.add(cb.equal(locationField, file.getLocation()));
                }

                //创建时间
                if(StrUtil.isNotBlank(searchVo.getStartDate())&&StrUtil.isNotBlank(searchVo.getEndDate())){
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
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
    public String uploadFileInputStream(InputStream inputStream,String fKey,String contentType,int size ){

        String result = "";
        String used = serviceProvider;
        if(StrUtil.isBlank(used)){
            //return new ResultUtil<Object>().setErrorMsg(501, "您还未配置OSS服务");
            return result;
        }
        // 上传至第三方云服务或服务器
        if(used.equals(SettingConstant.QINIU_OSS)){
            result = qiniuUtil.qiniuInputStreamUpload(inputStream, fKey);
        }else if(used.equals(SettingConstant.ALI_OSS)){
            result = aliOssUtil.aliInputStreamUpload(inputStream, fKey);
        }else if(used.equals(SettingConstant.TENCENT_OSS)){
            result = tencentOssUtil.tencentInputStreamUpload( inputStream, fKey,contentType,size);
        }
        return result;
    }

    @Override
    public Result<Object> upload(java.io.File file,String contentType) {
        String used = serviceProvider;
        String result = "";
        String extName = cn.hutool.core.io.FileUtil.extName(file);
        String whiteList = XxlConfClient.get("matrix.uploadfile.whitelist");
        if(StrUtil.isEmpty(whiteList)){
            whiteList = CommonConstant.UPLOAD_WHITELIST;
        }
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg(500,"文件头与文件类型不一致，请检查文件");
        }
        if(!StrUtil.containsAnyIgnoreCase(whiteList,"." + extName)){
            return new ResultUtil<Object>().setErrorMsg(403, "非法的文件");
        }
        String fKey = CommonUtil.renamePic(file.getAbsolutePath());
        File f = new File();
        try {
            String filePath = file.getAbsolutePath();
            // 上传至第三方云服务或服务器
            if(used.equals(SettingConstant.QINIU_OSS)){
                result = qiniuUtil.qiniuUpload(filePath, fKey);
                f.setLocation(CommonConstant.OSS_QINIU);
            }else if(used.equals(SettingConstant.ALI_OSS)){
                result = aliOssUtil.aliUpload(filePath, fKey);
                f.setLocation(CommonConstant.OSS_ALI);
            }else if(used.equals(SettingConstant.TENCENT_OSS)){
                result = tencentOssUtil.tencentUpload( filePath, fKey);
                f.setLocation(CommonConstant.OSS_TENCENT);
            }else if(used.equals(SettingConstant.LOCAL_OSS)){
                result = fileUtil.upload(file, fKey);
                f.setLocation(CommonConstant.OSS_LOCAL);
            }
            // 保存数据信息至数据库
            f.setName(file.getName());
            f.setSize(file.length());
            f.setType(contentType);
            f.setFKey(fKey);
            f.setUrl(result);
            save(f);
        } catch (Exception e) {
            log.error(e.toString());
            return new ResultUtil<Object>().setErrorMsg(e.toString());
        }
        return new ResultUtil<Object>().setData(result);
    }
}