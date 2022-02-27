package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.ytdinfo.inndoo.config.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpClientConnectionManager;
import org.springframework.core.env.Environment;
import redis.clients.jedis.JedisCommands;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 原作者 zzxadi https://github.com/zzxadi/Snowflake-IdWorker
 * @author Exrickx
 */
@Slf4j
public class SnowFlakeUtil {

    /**
     * 缓存key前缀
     */
    private static String prefixKey = "workerid:core";

    /**
     * 缓存过期时间
     */
    private static int expireSeconds = 1800;
    /**
     * Id
     */
    private final long id;
    /**
     * 时间起始标记点，作为基准，一般取系统的最近时间
     */
    private final long epoch = 1524291141010L;
    /**
     * 机器标识位数
     */
    private final static long workerIdBits = 10L;
    /**
     * 机器ID最大值: 1023
     */
    private final static long maxWorkerId = -1L ^ -1L << workerIdBits;
    /**
     * 0，并发控制
     */
    private long sequence = 0L;
    /**
     * 毫秒内自增位
     */
    private final long sequenceBits = 12L;

    /**
     * 12
     */
    private final long workerIdShift = this.sequenceBits;
    /**
     * 22
     */
    private final long timestampLeftShift = this.sequenceBits + workerIdBits;
    /**
     * 4095,111111111111,12位
     */
    private final long sequenceMask = -1L ^ -1L << this.sequenceBits;
    private long lastTimestamp = -1L;

    private SnowFlakeUtil(long id) {
        if (id > maxWorkerId || id < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        this.id = id;
    }

    public synchronized String getNextId(){
        return String.valueOf(nextId());
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (this.lastTimestamp == timestamp) {
            //如果上一个timestamp与新产生的相等，则sequence加一(0-4095循环); 对新的timestamp，sequence从0开始
            this.sequence = this.sequence + 1 & this.sequenceMask;
            if (this.sequence == 0) {
                // 重新生成timestamp
                timestamp = this.tilNextMillis(this.lastTimestamp);
            }
        } else {
            this.sequence = 0;
        }

        if (timestamp < this.lastTimestamp) {
            log.error(String.format("clock moved backwards.Refusing to generate id for %d milliseconds", (this.lastTimestamp - timestamp)));
            return -1;
        }

        this.lastTimestamp = timestamp;
        return timestamp - this.epoch << this.timestampLeftShift | this.id << this.workerIdShift | this.sequence;
    }

    private static SnowFlakeUtil flowIdWorker;

    public synchronized static void setFlowIdInstance() {
        if(flowIdWorker == null){
            flowIdWorker = getFlowIdWorker();
        }
    }
    public synchronized static SnowFlakeUtil getFlowIdInstance() {
        if(flowIdWorker == null){
            flowIdWorker = getFlowIdWorker();
        }
        return flowIdWorker;
    }

    private synchronized static SnowFlakeUtil getFlowIdWorker(){
        ServerUtil serverUtil = SpringContextUtil.getBean(ServerUtil.class);
        String address = serverUtil.getIpAddress();
        String port = serverUtil.getPort();
        String ipPort = address + ":" + port;
        JedisCommands jedis = RedisUtil.getJedis();
        String workerKey = prefixKey + ":" + ipPort;
        String workerMapKey = prefixKey + ":map";
        String id = jedis.get(workerKey);
        Integer workerId = 0;
        SnowFlakeHeartbeatThread snowFlakeHeartbeatThread = new SnowFlakeHeartbeatThread();
        snowFlakeHeartbeatThread.start();
        try {
            snowFlakeHeartbeatThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(StrUtil.isNotEmpty(id)){
            workerId = Integer.parseInt(id);
            return new SnowFlakeUtil(workerId);
        }
        String idInMap = jedis.hget(workerMapKey, ipPort);
        if(StrUtil.isNotEmpty(idInMap)){
            workerId = Integer.parseInt(idInMap);
            jedis.setex(workerKey,expireSeconds,workerId.toString());
            return new SnowFlakeUtil(workerId);
        }
        Long nextId = jedis.incr(prefixKey);
        if(nextId < maxWorkerId){
            workerId = nextId.intValue();
            //30分钟过期
            jedis.setex(workerKey,expireSeconds,workerId.toString());
            jedis.hset(workerMapKey, ipPort, workerId.toString());
        }else{
            Map<String, String> workerMap = jedis.hgetAll(workerMapKey);
            for (String k : workerMap.keySet()) {
                String key = prefixKey + ":" + k;
                //过期可重用
                if(!jedis.exists(key)){
                    String v = workerMap.get(k);
                    jedis.setex(workerKey,expireSeconds,v);
                    jedis.hdel(workerMapKey,k);
                    jedis.hset(workerMapKey, ipPort, v);
                    workerId = Integer.parseInt(v);
                    break;
                }
            }
        }
        return new SnowFlakeUtil(workerId);
    }

    /**
     * 等待下一个毫秒的到来, 保证返回的毫秒数在参数lastTimestamp之后
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获得系统当前毫秒数
     */
    private static long timeGen() {
        return System.currentTimeMillis();
    }


    public static class SnowFlakeHeartbeatThread extends Thread {
        private volatile boolean shutdown;
        @Override
        public void run() {
            ServerUtil serverUtil = SpringContextUtil.getBean(ServerUtil.class);
            String address = serverUtil.getIpAddress();
            String port = serverUtil.getPort();
            String ipPort = address + ":" + port;
            JedisCommands jedis = RedisUtil.getJedis();
            String workerKey = prefixKey + ":" + ipPort;
            try {
                while (!shutdown) {
                    synchronized (this) {
                        //30秒走一次
                        wait(30000);
                        System.out.println("SnowFlakeHeartbeatThread:" + DateUtil.format(new Date(),"yyyy-MM-dd HH:mm:ss"));
                        jedis.expire(workerKey,expireSeconds);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }

    public static void main(String[] args) {
        for(int i=0;i<100;i++){
            SnowFlakeUtil snowFlakeUtil = SnowFlakeUtil.getFlowIdInstance();
            System.out.println(snowFlakeUtil.nextId());
        }
    }


}
