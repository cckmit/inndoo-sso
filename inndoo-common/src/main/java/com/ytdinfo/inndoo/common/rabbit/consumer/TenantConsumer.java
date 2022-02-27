package com.ytdinfo.inndoo.common.rabbit.consumer;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceLoader;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.config.ITenantMQConfig;
import com.ytdinfo.inndoo.common.utils.BulkheadContainter;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.common.vo.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author yaochangning
 */
@Service
@Scope("prototype")
public class TenantConsumer extends BaseConsumer {
    @Autowired
    DynamicDataSourceLoader dynamicDataSourceLoader;

    @Override
    public void onMessage(MQMessage mqMessage) {

        String action = mqMessage.getMessageProperties().getHeaders().get("action").toString();
        Tenant u = new Tenant();
        if (!"setDefault".equals(action)) {
            u = JSONUtil.toBean((JSONObject) mqMessage.getContent(), Tenant.class);
        }

        if ("delete".equals(action)) {
            dynamicDataSourceLoader.deleteByTenant(u);
        }
        if ("put".equals(action)) {
            dynamicDataSourceLoader.initByTenant(u);
            BulkheadContainter.init(u.getId());
        }
        if ("add".equals(action)) {
            dynamicDataSourceLoader.initByTenant(u);
            initMQByTenant(u.getId());
            BulkheadContainter.init(u.getId());
        }
        if ("setDefault".equals(action)) {
            dynamicDataSourceLoader.init();
        }

    }

    private void initMQByTenant(String tenantId){
        String packageName = "com.ytdinfo.inndoo.config";
        Set<Class<?>> configClasses = ClassUtil.scanPackageBySuper(packageName, ITenantMQConfig.class);
        for (Class<?> configClass : configClasses) {
            ITenantMQConfig tenantMQConfig = (ITenantMQConfig) SpringContextUtil.getBean(configClass);
            tenantMQConfig.dynamicInit(tenantId);
        }
    }


}
