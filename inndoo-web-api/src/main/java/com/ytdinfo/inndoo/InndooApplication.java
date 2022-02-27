package com.ytdinfo.inndoo;

import com.ytdinfo.inndoo.base.BaseRepositoryFactoryBean;
import com.ytdinfo.inndoo.common.listener.ApplicationEventListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Exrickx
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
// 启用JPA审计
@EnableJpaAuditing
// 启用缓存
@EnableCaching
// 启用异步
@EnableAsync
// 启用自带定时任务
@EnableScheduling
//加载自定义Repository
@EnableJpaRepositories(basePackages = {"com.ytdinfo.inndoo"},repositoryFactoryBeanClass = BaseRepositoryFactoryBean.class)
public class InndooApplication {

    @Primary
    @Bean
    public TaskExecutor primaryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        return executor;
    }

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(InndooApplication.class);
        springApplication.addListeners(new ApplicationEventListener());
        springApplication.run(args);
    }
}
