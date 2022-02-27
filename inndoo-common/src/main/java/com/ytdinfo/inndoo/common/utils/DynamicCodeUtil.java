package com.ytdinfo.inndoo.common.utils;

import groovy.lang.GroovyClassLoader;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class DynamicCodeUtil {

    private static GroovyClassLoader groovyClassLoader = null;

    // 缓存本地动态代码版本
    private static ConcurrentMap<String, String> LOCAL_VERSION = new ConcurrentHashMap<>();

    /**
     * 初始化类加载工具
     */
    public static void initGroovyClassLoader() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceEncoding("UTF-8");
        // 设置该GroovyClassLoader的父ClassLoader为当前线程的加载器(默认)
        groovyClassLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), config);
//        groovyClassLoader = new GroovyClassLoader();
    }

    /**
     * 获取类加载工具
     *
     * @return
     */
    public static GroovyClassLoader getGroovyClassLoader() {
        if (groovyClassLoader != null) {
            return groovyClassLoader;
        } else {
            initGroovyClassLoader();
            return groovyClassLoader;
        }
    }

    public static boolean checkVersion(String beanName,String version){
        String localVersion =  LOCAL_VERSION.get(beanName);
        if(localVersion == null){
            return false;
        }
        return localVersion.equals(version);
    }

    /**
     * 获取动态代码执行器
     *
     * @return
     */
    public static Object getBean(String beanName, String beanCode, String version) {
        Object bean;
        if (SpringContextUtil.containsBean(beanName)) {
            if (checkVersion(beanName, version)) {
                bean = SpringContextUtil.getBean(beanName);
            } else {
                synchronized (DynamicCodeUtil.class) {
                    if (checkVersion(beanName, version)) {
                        bean = SpringContextUtil.getBean(beanName);
                    } else {
                        bean = reloadBean(beanName, beanCode, version);
                    }
                }
            }
        } else {
            synchronized (DynamicCodeUtil.class) {
                if (SpringContextUtil.containsBean(beanName)) {
                    bean = SpringContextUtil.getBean(beanName);
                } else {
                    GroovyClassLoader groovyClassLoader = getGroovyClassLoader();
                    Class clazz = groovyClassLoader.parseClass(beanCode);
                    SpringContextUtil.registerBean(beanName, clazz);
                    bean = SpringContextUtil.getBean(beanName);
                    SpringContextUtil.autowireBean(bean);
                    LOCAL_VERSION.put(beanName, version);
                    // groovyClassLoader.clearCache();
                }
            }
        }
        return bean;
    }

    private static Object reloadBean(String beanName, String beanCode, String version) {
        GroovyClassLoader groovyClassLoader = getGroovyClassLoader();
        Class clazz = groovyClassLoader.parseClass(beanCode);
        SpringContextUtil.registerBean(beanName, clazz);
        Object bean = SpringContextUtil.getBean(beanName);
        SpringContextUtil.autowireBean(bean);
        LOCAL_VERSION.put(beanName,version);
//        groovyClassLoader.clearCache();
        return bean;
    }

}
