package com.ytdinfo.inndoo.common.utils.ao;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.modules.base.vo.OssSetting;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractUploader {

    @XxlConf("matrix.inndoo.upload.serviceprovider")
    private String serviceProvider;

    @XxlConf("matrix.inndoo.upload.accesskey")
    private String accessKey;

    @XxlConf("matrix.inndoo.upload.secretkey")
    private String secretKey;


    @XxlConf("matrix.inndoo.upload.protocol")
    private String protocol;

    /**
     * 指定要上传到的存储桶
     */
    @XxlConf("matrix.inndoo.upload.bucket")
    private String bucket;

    @XxlConf("matrix.inndoo.upload.bucketregion")
    private String bucketRegion;

    @XxlConf("matrix.inndoo.upload.endpoint")
    private String endPoint;

    public OssSetting getOssSetting(){
        if(StrUtil.isBlank(accessKey)||StrUtil.isBlank(secretKey)||StrUtil.isBlank(bucketRegion)||StrUtil.isBlank(endPoint)){
            return null;
        }
        if(!"http".equals(protocol)&&!"https".equals(protocol)){
            return null;
        }
        OssSetting oss = new OssSetting();
        oss.setAccessKey(accessKey);
        oss.setSecretKey(secretKey);
        oss.setBucket(bucket);
        oss.setBucketRegion(bucketRegion);
        oss.setEndpoint(endPoint);
        oss.setHttp(protocol+"://");
        return oss;
    }
}
