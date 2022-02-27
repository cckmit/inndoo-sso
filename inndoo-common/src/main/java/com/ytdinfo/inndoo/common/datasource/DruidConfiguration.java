package com.ytdinfo.inndoo.common.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by timmy on 2019/7/15.
 */
@Configuration
public class DruidConfiguration {
    @XxlConf("core.spring.datasource.url")
    private String dbUrl;
    @XxlConf("core.spring.datasource.username")
    private String username;
    @XxlConf("core.spring.datasource.password")
    private String password;
    @XxlConf("core.spring.datasource.driverclassname")
    private String driverClassName;
    @XxlConf("core.spring.datasource.initialsize")
    private int initialSize;
    @XxlConf("core.spring.datasource.minidle")
    private int minIdle;
    @XxlConf("core.spring.datasource.maxactive")
    private int maxActive;
    @XxlConf("core.spring.datasource.maxwait")
    private int maxWait;
    @XxlConf("core.spring.datasource.timebetweenevictionrunsmillis")
    private int timeBetweenEvictionRunsMillis;
    @XxlConf("core.spring.datasource.minevictableidletimemillis")
    private int minEvictableIdleTimeMillis;
    @XxlConf("core.spring.datasource.validationquery")
    private String validationQuery;
    @XxlConf("core.spring.datasource.testwhileidle")
    private boolean testWhileIdle;
    @XxlConf("core.spring.datasource.testonborrow")
    private boolean testOnBorrow;
    @XxlConf("core.spring.datasource.testonreturn")
    private boolean testOnReturn;
    @XxlConf("core.spring.datasource.poolpreparedstatements")
    private boolean poolPreparedStatements;
    @XxlConf("core.spring.datasource.maxpoolpreparedstatementperconnectionsize")
    private int maxPoolPreparedStatementPerConnectionSize;
    @XxlConf("core.spring.datasource.filters")
    private String filters;
    @XxlConf("core.spring.datasource.connectionproperties")
    private String connectionProperties;

    private String secret = "rPMYoxxUmZVnGh3n";

    @Bean(name = "primaryDataSource")
    @Qualifier("primaryDataSource")
    public DataSource dataSource() {
        DruidDataSource datasource = new DruidDataSource();
        datasource.setUrl(AESUtil.decrypt(this.dbUrl,secret));
        datasource.setUsername(AESUtil.decrypt(username,secret));
        datasource.setPassword(AESUtil.decrypt(password,secret));
        datasource.setDriverClassName(driverClassName);
        //configuration
        datasource.setInitialSize(initialSize);
        datasource.setMinIdle(minIdle);
        datasource.setMaxActive(maxActive);
        datasource.setMaxWait(maxWait);
        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        datasource.setValidationQuery(validationQuery);
        datasource.setTestWhileIdle(testWhileIdle);
        datasource.setTestOnBorrow(testOnBorrow);
        datasource.setTestOnReturn(testOnReturn);
        datasource.setPoolPreparedStatements(poolPreparedStatements);
        datasource.setMaxPoolPreparedStatementPerConnectionSize(maxPoolPreparedStatementPerConnectionSize);
        try {
            datasource.setFilters(filters);
        } catch (SQLException e) {
            System.err.println("druid configuration initialization filter: " + e);
        }
        datasource.setConnectionProperties(connectionProperties);
        return datasource;
    }
}