package com.ytdinfo.inndoo.config.xxlsso;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 2019/6/23
 * @create xuewenlong
 */
@Data
@Configuration
@Component
//@ConfigurationProperties(prefix = "xxl-sso")
public class XxlSsoIgnorePaths {

//    private List<String> paths = new ArrayList<>();

    public List<String> getPaths(){
        List<String> urls = new ArrayList<>();
        if(StrUtil.isNotEmpty(this.ignorePaths)){
            String[] items = this.ignorePaths.split(",");
            for (int i = 0; i < items.length; i++) {
                if(StrUtil.isNotEmpty(items[i])){
                    urls.add(items[i]);
                }
            }
        }
        return urls;
    }
    @XxlConf("inndoo-sso.xxl-sso.paths")
    private String ignorePaths;
}
