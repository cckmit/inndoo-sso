package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.config.IWxMQConfig;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.modules.base.entity.WxAuthorizer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author yaochangning
 */
@Service
@Scope("prototype")
public class WxAuthorizerConsumer extends BaseConsumer {
    @Override
    public void onMessage(MQMessage mqMessage) {
        String action = mqMessage.getMessageProperties().getHeaders().get("action").toString();
        WxAuthorizer u = JSONUtil.toBean((JSONObject) mqMessage.getContent(), WxAuthorizer.class);
        if ("add".equals(action)) {
            String packageName = "com.ytdinfo.inndoo.config";
            Set<Class<?>> configClasses = ClassUtil.scanPackageBySuper(packageName, IWxMQConfig.class);
            for (Class<?> configClass : configClasses) {
                IWxMQConfig wxMQConfig = (IWxMQConfig) SpringContextUtil.getBean(configClass);
                wxMQConfig.dynamicInit(u.getAppid());
            }
        }
    }
}
