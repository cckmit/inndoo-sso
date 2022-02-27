package com.ytdinfo.inndoo.config.redis;

/**
 *
 * @author timmy
 * @date 2021/5/27
 */
public class RedisMode {
    /**
     * 集群模式
     */
    public static String CLUSTER = "cluster";
    /**
     * 哨兵模式
     */
    public static String SENTINEL = "sentinel";
    /**
     * 单机模式
     */
    public static String STANDALONE = "standalone";
}