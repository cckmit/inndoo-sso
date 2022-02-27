package com.ytdinfo.inndoo.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author Exrickx
 */
@Slf4j
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        SpringContextUtil.applicationContext = applicationContext;
    }

    public static Object getBean(String name) {

        return applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {

        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {

        return applicationContext.getBean(name, clazz);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private static AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
        return applicationContext.getAutowireCapableBeanFactory();
    }

    private static DefaultListableBeanFactory getDefaultListableBeanFactory() {
        return (DefaultListableBeanFactory) getAutowireCapableBeanFactory();
    }

    public static void registerBean(String beanName, Class clazz) {
        if (containsBean(beanName)) {
            removeBean(beanName);
        }
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        getDefaultListableBeanFactory().registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
    }

    public static void autowireBean(Object existingBean) {
        getAutowireCapableBeanFactory().autowireBean(existingBean);
    }

    public static boolean containsBean(String beanName) {
        return getAutowireCapableBeanFactory().containsBean(beanName);
    }

    public static void removeBean(String beanName) {
        getDefaultListableBeanFactory().removeBeanDefinition(beanName);
    }

}
