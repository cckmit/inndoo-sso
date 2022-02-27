package com.ytdinfo.inndoo.base.mybatis;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;

import java.util.List;

/**
 * 自定义sql注入器，用来扩展MySQLPlus BaseMapper方法
 * @author timmy
 * @date 2019/10/18
 */
public class InndooSqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
        List<AbstractMethod> methodList=super.getMethodList(mapperClass);
        methodList.add(new InsertIgnore());
        methodList.add(new InsertOnDuplicateUpdate());
        return methodList;
    }
}