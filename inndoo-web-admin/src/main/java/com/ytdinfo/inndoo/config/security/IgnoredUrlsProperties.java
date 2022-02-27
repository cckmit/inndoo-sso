package com.ytdinfo.inndoo.config.security;

        import cn.hutool.core.util.StrUtil;
        import com.ytdinfo.conf.core.annotation.XxlConf;
        import lombok.Data;
        import org.springframework.context.annotation.Configuration;

        import java.util.ArrayList;
        import java.util.List;

/**
 * @author Exrickx
 */
@Data
@Configuration
public class IgnoredUrlsProperties {

    public List<String> getUrls(){
        List<String> urls = new ArrayList<>();
        if(StrUtil.isNotEmpty(this.ignoredUrl)){
            String[] items = this.ignoredUrl.split(",");
            for (int i = 0; i < items.length; i++) {
                if(StrUtil.isNotEmpty(items[i])){
                    urls.add(items[i]);
                }
            }
        }
        return urls;
    }

    @XxlConf("core.ignored.urls")
    private String ignoredUrl;
}
