package com.ytdinfo.inndoo.common.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Created by timmy on 2019/5/9.
 */
@Component
@Slf4j
public class ApplicationRunnerImpl implements ApplicationRunner{

    @Autowired
    private DynamicDataSourceLoader dynamicDataSourceLoader;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        //从数据库初始化datasource
        //dynamicDataSourceLoader.init();
    }
}