package com.ytdinfo.inndoo.common.datasource;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;

/**
 * Created by timmy on 2019/5/9.
 */
@Configuration
public class DSConfig {

//    @Bean(name = "primaryDataSource")
//    @Qualifier("primaryDataSource")
//    @ConfigurationProperties(prefix = "spring.datasource.primary")
//    public DataSource primaryDataSource() {
//        return DruidDataSourceBuilder.create().build();
//    }

    @Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dynamicDataSource(@Qualifier("primaryDataSource") DataSource dataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        //保存我们有的数据源，方便后面动态增加
        dynamicDataSource.dataSourceMap = new HashMap<>();
        dynamicDataSource.dataSourceMap.put("primary", dataSource);
        //父类的方法
        dynamicDataSource.setTargetDataSources(dynamicDataSource.dataSourceMap);
        //父类的方法
        dynamicDataSource.setDefaultTargetDataSource(dataSource);
        return dynamicDataSource;
    }
}