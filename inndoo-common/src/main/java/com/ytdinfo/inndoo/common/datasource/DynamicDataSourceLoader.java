package com.ytdinfo.inndoo.common.datasource;

import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.context.TenantContextHolder;
import com.ytdinfo.inndoo.common.context.WxComponentContextHolder;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.common.utils.BulkheadContainter;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.vo.Tenant;
import com.ytdinfo.inndoo.common.vo.WxopenComponentInfo;
import com.alibaba.druid.pool.DruidDataSource;
import com.ytdinfo.inndoo.modules.core.entity.ActivityDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;

/**
 * Created by timmy on 2019/5/9.
 */
@Service
public class DynamicDataSourceLoader {

    @Autowired
    private ApplicationContext ctx;

    /**
     * Recourse By Name注解，获取在DSConfig中初始化的Bean
     */
    @Resource
    private DynamicDataSource dynamicDataSource;

    @Autowired
    private MatrixApiUtil apiUtil;

    @XxlConf("core.spring.datasource.username")
    private String username;
    @XxlConf("core.spring.datasource.password")
    private String password;

    private String secret = "rPMYoxxUmZVnGh3n";
    public void init() {
        // System.out.println("进入init方法");
        List<Tenant> tenantList = apiUtil.getTenantList();
        for (int i = 0; i < tenantList.size(); i++) {
            Tenant tenant = tenantList.get(i);
            initByTenant(tenant, false);
            BulkheadContainter.init(tenant.getId());
        }
        dynamicDataSource.setTargetDataSources(dynamicDataSource.dataSourceMap);
        dynamicDataSource.afterPropertiesSet();
    }

    public void initByTenant(Tenant tenant, Boolean updateDataSource) {
        String dataSourceId = tenant.getActivityDataSourceId();
        ActivityDataSource activityDataSource = apiUtil.getActivityDataSource(dataSourceId);
        String databaseUrl = activityDataSource.getCoreDatabaseUrl();
        if(StrUtil.isEmpty(databaseUrl)){
            return;
        }
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();
        DruidDataSource primaryDataSource = (DruidDataSource) dynamicDataSource.dataSourceMap.get("primary");
        //动态注册bean. bean的名字为第一个参数
        String dataSourceName = "dataSource-" + tenant.getId();

        DruidDataSource dataSource = (DruidDataSource) dynamicDataSource.dataSourceMap.get(tenant.getId());
        if (dataSource == null) {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
            defaultListableBeanFactory.registerBeanDefinition(dataSourceName, beanDefinitionBuilder.getBeanDefinition());
            //获取动态注册的bean.
            dataSource = ctx.getBean(dataSourceName, DruidDataSource.class);
        }
        if(databaseUrl.contains("?")){
            databaseUrl = StrUtil.subPre(databaseUrl,databaseUrl.indexOf("?"));
        }
        databaseUrl = databaseUrl + "?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2b8&useLegacyDatetimeCode=false&rewriteBatchedStatements=true&autoReconnect=true&allowMultiQueries=true";
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(AESUtil.decrypt(username, secret));
        dataSource.setPassword(AESUtil.decrypt(password,secret));
        dataSource.setDriverClassName(primaryDataSource.getDriverClassName());
        dataSource.setInitialSize(primaryDataSource.getInitialSize());
        dataSource.setMaxActive(primaryDataSource.getMaxActive());
        dataSource.setMinIdle(primaryDataSource.getMinIdle());
        dataSource.setMaxWait(primaryDataSource.getMaxWait());
        dataSource.setTimeBetweenEvictionRunsMillis(primaryDataSource.getTimeBetweenEvictionRunsMillis());
        dataSource.setMinEvictableIdleTimeMillis(primaryDataSource.getMinEvictableIdleTimeMillis());
        dataSource.setValidationQuery(primaryDataSource.getValidationQuery());
        dataSource.setTestWhileIdle(primaryDataSource.isTestWhileIdle());
        dataSource.setTestOnBorrow(primaryDataSource.isTestOnBorrow());
        dataSource.setPoolPreparedStatements(primaryDataSource.isPoolPreparedStatements());
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(primaryDataSource.getMaxPoolPreparedStatementPerConnectionSize());
        //设为true代表removeAbandonedTimeoutMillis时间到即强制回收
        dataSource.setRemoveAbandoned(false);
        dataSource.setRemoveAbandonedTimeoutMillis(1000*60*5);
        dataSource.setLogAbandoned(false);
        dynamicDataSource.dataSourceMap.put(tenant.getId(), dataSource);
        if (!DynamicDataSourceContextHolder.dataSourceIds.contains(tenant.getId())) {
            DynamicDataSourceContextHolder.dataSourceIds.add(tenant.getId());
            //增加TenantContext，用于获取登录用户租户信息
            TenantContextHolder.add(tenant);
        }
        if(StrUtil.isNotEmpty(tenant.getWxopenComponentId())){
            WxopenComponentInfo componentInfo = apiUtil.getComponentInfo(tenant.getWxopenComponentId());
            WxComponentContextHolder.add(componentInfo);
        }
        if (updateDataSource) {
            dynamicDataSource.setTargetDataSources(dynamicDataSource.dataSourceMap);
            dynamicDataSource.afterPropertiesSet();
        }
    }

    public void initByTenant(Tenant tenant) {
        initByTenant(tenant, true);
    }

    public void deleteByTenant(Tenant tenant) {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();
        //动态注册bean. bean的名字为第一个参数
        String dataSourceName = "dataSource-" + tenant.getId();
        if (defaultListableBeanFactory.containsBeanDefinition(dataSourceName)) {
            defaultListableBeanFactory.removeBeanDefinition(dataSourceName);
        }
        dynamicDataSource.dataSourceMap.remove(tenant.getId());
        DynamicDataSourceContextHolder.dataSourceIds.remove(tenant.getId());

        TenantContextHolder.remove(tenant.getId());
        WxComponentContextHolder.remove(tenant.getWxopenComponentId());
        dynamicDataSource.setTargetDataSources(dynamicDataSource.dataSourceMap);
        dynamicDataSource.afterPropertiesSet();
    }
}