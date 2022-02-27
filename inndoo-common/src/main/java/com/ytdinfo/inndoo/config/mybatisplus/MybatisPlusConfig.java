package com.ytdinfo.inndoo.config.mybatisplus;

import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.ytdinfo.inndoo.base.mybatis.InndooSqlInjector;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Exrickx
 */
@Configuration
@MapperScan("com.ytdinfo.inndoo.modules.*.*.mapper")
public class MybatisPlusConfig {

    @Bean
    public DefaultSqlInjector getSqlInjector(){
        return new InndooSqlInjector();
    }
    /**
     * 分页插件，自动识别数据库类型
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }
}
