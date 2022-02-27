package com.ytdinfo.inndoo.common.utils;

import com.ytdinfo.inndoo.common.enums.ThreadPoolType;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Exrickx
 */
public class ThreadPoolUtil {

    /**
     * 线程池HashMap
     */
    private static HashMap<ThreadPoolType, ThreadPoolExecutor> poolMap = new HashMap<>();

    /**
     * 根据线程池定义获取相对独立的线程池
     * @param poolType
     * @return
     */
    public static synchronized ThreadPoolExecutor getPool(ThreadPoolType poolType) {
        ThreadPoolExecutor executor = poolMap.get(poolType);
        if (executor == null || executor.isTerminated() || executor.isShutdown()) {
            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(poolType.getQueueSize());
            executor = new ThreadPoolExecutor(poolType.getCore(), poolType.getMax(), poolType.getActiveTime(), TimeUnit.MILLISECONDS, queue, new ThreadPoolExecutor.CallerRunsPolicy());
            executor.prestartAllCoreThreads();
            poolMap.put(poolType, executor);
        }
        return executor;
    }

    /**
     * 根据线程池定义获取相对独立的线程池，此方法需要自己手工关闭线程池
     *
     * @param poolType
     * @return
     */
    public static synchronized ThreadPoolExecutor createPool(ThreadPoolType poolType) {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(poolType.getQueueSize());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(poolType.getCore(), poolType.getMax(), poolType.getActiveTime(), TimeUnit.MILLISECONDS, queue, new ThreadPoolExecutor.CallerRunsPolicy());
        executor.prestartAllCoreThreads();
        return executor;
    }

    /**
     * 线程缓冲队列
     */
    private static BlockingQueue<Runnable> bqueue = new ArrayBlockingQueue<Runnable>(100);
    /**
     * 核心线程数，会一直存活，即使没有任务，线程池也会维护线程的最少数量
     */
    private static final int SIZE_CORE_POOL = 20;
    /**
     * 线程池维护线程的最大数量
     */
    private static final int SIZE_MAX_POOL = 50;
    /**
     * 线程池维护线程所允许的空闲时间
     */
    private static final long ALIVE_TIME = 2000;

    private static ThreadPoolExecutor pool = new ThreadPoolExecutor(SIZE_CORE_POOL, SIZE_MAX_POOL, ALIVE_TIME, TimeUnit.MILLISECONDS, bqueue, new ThreadPoolExecutor.CallerRunsPolicy());

    static {
        pool.prestartAllCoreThreads();
    }

    public static ThreadPoolExecutor getPool() {
        return pool;
    }

    public static void main(String[] args) {
        System.out.println(pool.getPoolSize());
    }
}
