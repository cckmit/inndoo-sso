package com.ytdinfo.inndoo.common.utils;

import com.ytdinfo.inndoo.common.context.UserContext;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BulkheadContainter {
    private static Map<String, ThreadPoolBulkhead> container = new ConcurrentHashMap<>();

    private static ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom().maxThreadPoolSize(ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE * 3)
            .coreThreadPoolSize(ThreadPoolBulkheadConfig.DEFAULT_CORE_THREAD_POOL_SIZE * 2).queueCapacity(ThreadPoolBulkheadConfig.DEFAULT_QUEUE_CAPACITY).build();

    private static ThreadPoolBulkheadRegistry registry = ThreadPoolBulkheadRegistry.of(config);

//    static {
//        int maxSize = ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE / 2;
//        if (maxSize <= 0) {
//            maxSize = 1;
//        }
//        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom().maxThreadPoolSize(maxSize).coreThreadPoolSize(1).queueCapacity(150).build();
//        ThreadPoolBulkheadRegistry registry = ThreadPoolBulkheadRegistry.of(config);
//        String[] array = new String[]{"tag"};
//        for (String s : array) {
//            ThreadPoolBulkhead apiBulkhead = registry.bulkhead(s);
//            container.put(s, apiBulkhead);
//        }
//    }

    public static void init(String tenantId) {
//        int maxSize = ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE / 2;
//        if (maxSize <= 0) {
//            maxSize = 1;
//        }
//        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom().maxThreadPoolSize(maxSize).coreThreadPoolSize(1).queueCapacity(150).build();
//        String[] array = new String[]{"tag", "apicheck"};
//        for (String s : array) {
//            ThreadPoolBulkhead apiBulkhead = registry.bulkhead(s);
//            container.putIfAbsent(tenantId + "_" + s, apiBulkhead);
//        }
    }

    public static ThreadPoolBulkhead get(String name) {
        String key = UserContext.getTenantId() + "_" + name;
        if (container.containsKey(key)) {
            return container.get(key);
        }
        return null;
    }
}
