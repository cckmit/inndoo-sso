package com.ytdinfo.inndoo.config.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.util.ReflectionUtils;

import java.time.Duration;
import java.util.*;

/**
 * Redis 容易出现缓存问题（超时、Redis 宕机等），当使用 spring cache 的注释 Cacheable、Cacheput 等处理缓存问题时，
 * 我们无法使用 try catch 处理出现的异常，所以最后导致结果是整个服务报错无法正常工作。
 * 通过自定义 TedisCacheManager 并继承 RedisCacheManager 来处理异常可以解决这个问题。
 */
@Slf4j
public class InndooRedisCacheManager extends RedisCacheManager implements ApplicationContextAware, InitializingBean {
    private ApplicationContext applicationContext;

    private Map<String, RedisCacheConfiguration> initialCacheConfiguration = new LinkedHashMap<>();

    private RedisCacheConfiguration redisCacheConfiguration;



    public InndooRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
        this.redisCacheConfiguration = defaultCacheConfiguration;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = super.getCache(name);
        return cache;
        //return new RedisCacheWrapper(cache);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            final Class clazz = applicationContext.getType(beanName);
            add(clazz);
        }
        super.afterPropertiesSet();
    }

    @Override
    protected Collection<RedisCache> loadCaches() {
        List<RedisCache> caches = new LinkedList<>();
        for (Map.Entry<String, RedisCacheConfiguration> entry : initialCacheConfiguration.entrySet()) {
            caches.add(super.createRedisCache(entry.getKey(), entry.getValue()));
        }
        return caches;
    }

    private void add(final Class clazz) {
        CacheConfig cacheConfig = AnnotationUtils.findAnnotation(clazz, CacheConfig.class);
        ReflectionUtils.doWithMethods(clazz, method -> {
            ReflectionUtils.makeAccessible(method);
            CacheExpire cacheExpire = AnnotationUtils.findAnnotation(method, CacheExpire.class);
            if (cacheExpire == null) {
                return;
            }
            Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);
            if (cacheable != null) {
                String[] cacheNames = cacheable.cacheNames();
                if(cacheNames == null || cacheNames.length == 0){
                    if(cacheConfig != null){
                        cacheNames = cacheConfig.cacheNames();
                    }
                }
                add(cacheNames, cacheExpire);
                return;
            }
            Caching caching = AnnotationUtils.findAnnotation(method, Caching.class);
            if (caching != null) {
                Cacheable[] cs = caching.cacheable();
                if (cs.length > 0) {
                    for (Cacheable c : cs) {
                        if (cacheExpire != null && c != null) {
                            String[] cacheNames = c.cacheNames();
                            if(cacheNames == null || cacheNames.length == 0){
                                if(cacheConfig != null){
                                    cacheNames = cacheConfig.cacheNames();
                                }
                            }
                            add(cacheNames, cacheExpire);
                        }
                    }
                }
            } else {
                if (cacheConfig != null) {
                    add(cacheConfig.cacheNames(), cacheExpire);
                }
            }
        }, method -> null != AnnotationUtils.findAnnotation(method, CacheExpire.class));
    }

    private void add(String[] cacheNames, CacheExpire cacheExpire) {
        for (String cacheName : cacheNames) {
            RedisSerializationContext.SerializationPair<Object> valueSerializationPair = redisCacheConfiguration.getValueSerializationPair();
            RedisSerializationContext.SerializationPair<String> keySerializationPair = redisCacheConfiguration.getKeySerializationPair();
            long expire = cacheExpire.expire();
            if (expire >= 0) {
                RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(expire))
                        //.disableCachingNullValues()
                        .serializeKeysWith(keySerializationPair)
                        .serializeValuesWith(valueSerializationPair);
                initialCacheConfiguration.put(cacheName, cacheConfiguration);
            } else {
                RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                        //.disableCachingNullValues()
                        .serializeKeysWith(keySerializationPair)
                        .serializeValuesWith(valueSerializationPair);
                initialCacheConfiguration.put(cacheName, cacheConfiguration);
                log.warn("{} use default expiration.", cacheName);
            }
        }
    }
}