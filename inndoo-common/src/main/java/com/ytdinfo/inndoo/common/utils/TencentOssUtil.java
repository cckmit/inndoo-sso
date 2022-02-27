package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.Copy;
import com.qcloud.cos.transfer.TransferManager;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.utils.ao.*;
import com.ytdinfo.inndoo.modules.base.vo.OssSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

/**
 * @author Exrickx
 */
@Component
@Slf4j
public class TencentOssUtil extends AbstractUploader{

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SettingUtil settingUtil;

    @Override
    public OssSetting getOssSetting(){
        OssSetting os = super.getOssSetting();
        if(os==null){
            throw new InndooException("您还未配置腾讯云COS");
        }
        return os;
    }

    /**
     * 文件路径上传
     * @param filePath
     * @param key
     * @return
     */
    public String tencentUpload(String filePath, String key) {

        OssSetting os = getOssSetting();

        COSCredentials cred = new BasicCOSCredentials(os.getAccessKey(), os.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(os.getBucketRegion()));
        COSClient cosClient = new COSClient(cred, clientConfig);

        String tenantId = UserContext.getTenantId();
        String qcloudFilePath = key;
        if(StrUtil.isNotEmpty(tenantId) && !StrUtil.startWith(key, tenantId)){
            qcloudFilePath = tenantId + "/" + key;
        }

        PutObjectRequest putObjectRequest = new PutObjectRequest(os.getBucket(), qcloudFilePath, new File(filePath));
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        cosClient.shutdown();
        return os.getHttp() + os.getEndpoint() + "/" + qcloudFilePath;
    }

    /**
     * 文件流上传
     * @param file
     * @param inputStream
     * @param key
     * @return
     */
    public String tencentInputStreamUpload(MultipartFile file, InputStream inputStream, String key) {
        String tenantId = UserContext.getTenantId();
        String qcloudFilePath = key;
        if(StrUtil.isNotEmpty(tenantId) && !StrUtil.startWith(key, tenantId)){
            qcloudFilePath = tenantId + "/" + key;
        }
        OssSetting os = getOssSetting();
        COSCredentials cred = new BasicCOSCredentials(os.getAccessKey(), os.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(os.getBucketRegion()));
        COSClient cosClient = new COSClient(cred, clientConfig);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());

        PutObjectRequest putObjectRequest = new PutObjectRequest(os.getBucket(), qcloudFilePath, inputStream, objectMetadata);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        cosClient.shutdown();
        return os.getHttp() + os.getEndpoint() + "/" + qcloudFilePath;
    }

    /**
     * 文件流上传
     * @param inputStream
     * @param key
     * @return
     */
    public String tencentInputStreamUpload(InputStream inputStream, String key,String contentType,int size) {
        String tenantId = "matrix";
        String qcloudFilePath = key;
        if(StrUtil.isNotEmpty(tenantId) && !StrUtil.startWith(key, tenantId)){
            qcloudFilePath = tenantId + "/" + key;
        }
        OssSetting os = getOssSetting();
        COSCredentials cred = new BasicCOSCredentials(os.getAccessKey(), os.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(os.getBucketRegion()));
        COSClient cosClient = new COSClient(cred, clientConfig);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(size);
        objectMetadata.setContentType(contentType);

        PutObjectRequest putObjectRequest = new PutObjectRequest(os.getBucket(), qcloudFilePath, inputStream, objectMetadata);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        cosClient.shutdown();
        return os.getHttp() + os.getEndpoint() + "/" + qcloudFilePath;
    }

    /**
     * 重命名
     * @param fromKey
     * @param toKey
     */
    public String renameFile(String fromKey, String toKey){

        OssSetting os = getOssSetting();
        copyFile(fromKey, toKey);
        deleteFile(fromKey);
        return os.getHttp() + os.getEndpoint() + "/" + toKey;
    }

    /**
     * 复制文件
     * @param fromKey
     */
    public String copyFile(String fromKey, String toKey){

        OssSetting os = getOssSetting();

        COSCredentials cred = new BasicCOSCredentials(os.getAccessKey(), os.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(os.getBucketRegion()));
        COSClient cosClient = new COSClient(cred, clientConfig);

        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(os.getBucket(), fromKey, os.getBucket(), toKey);

        TransferManager transferManager = new TransferManager(cosClient);
        try {
            Copy copy = transferManager.copy(copyObjectRequest, cosClient, null);
            CopyResult copyResult = copy.waitForCopyResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InndooException("复制文件失败");
        }
        transferManager.shutdownNow();
        cosClient.shutdown();
        return os.getHttp() + os.getEndpoint() + "/" + toKey;
    }

    /**
     * 删除文件
     * @param key
     */
    public void deleteFile(String key){

        OssSetting os = getOssSetting();

        COSCredentials cred = new BasicCOSCredentials(os.getAccessKey(), os.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(os.getBucketRegion()));
        COSClient cosClient = new COSClient(cred, clientConfig);

        cosClient.deleteObject(os.getBucket(), key);
        cosClient.shutdown();
    }
}
