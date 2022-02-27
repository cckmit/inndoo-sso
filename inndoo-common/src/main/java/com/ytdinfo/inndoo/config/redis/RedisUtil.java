package com.ytdinfo.inndoo.config.redis;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import com.ytdinfo.inndoo.common.utils.XXLConfUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.*;
import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.Pool;

import java.util.*;

/**
 * @author timmy
 * @date 2020/10/12
 */
public class RedisUtil {

    private static String keySetName = "platform:keystore:";

    public static JedisCommands getJedis(){
        if(StrUtil.isEmpty(XXLConfUtil.redisMode) || RedisMode.CLUSTER.equals(XXLConfUtil.redisMode)){
            JedisCluster pool = SpringContextUtil.getBean(JedisCluster.class);
            return pool;
        }
        if(RedisMode.STANDALONE.equals(XXLConfUtil.redisMode) || RedisMode.SENTINEL.equals(XXLConfUtil.redisMode)){
            Pool<Jedis> jedisPool = (Pool<Jedis>)SpringContextUtil.getBean("jedisPool");
            return jedisPool.getResource();
        }
        return null;
    }

    public static Object hashMapGet(String prefix, String key){
        StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
        Object value = redisTemplate.opsForHash().get(prefix, key);
        if(value != null){
            return value;
        }
        return value;
    }

    public static void addKeyToStore(String prefix, String key) {
        StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
        redisTemplate.opsForSet().add(keySetName + prefix,key);
    }

    public static void addAllKeyToStore(String prefix, Set<String> keys) {
        StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
        String[] keyArray = new String[keys.size()];
        keys.toArray(keyArray);
        redisTemplate.opsForSet().add(keySetName + prefix, keyArray);
    }

    public static void removeKeyFromStore(String prefix, String key) {
        StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
        redisTemplate.opsForSet().remove(keySetName + prefix, key);
    }

    public static void clearKeyFromStore(String prefix) {
        StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
        redisTemplate.unlink(keySetName + prefix);
    }

    public static Set<String> membersFromKeyStore(String prefix){
        StringRedisTemplate redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);
        return redisTemplate.opsForSet().members(keySetName + prefix);
    }

    private static Set<String> getScan(Jedis redisService, String key) {
        Set<String> list = new HashSet<>();
        ScanParams params = new ScanParams();
        params.match(key);
        params.count(1000);
        String cursor = "0";
        while (true) {
            ScanResult scanResult = redisService.scan(cursor, params);
            List<String> elements = scanResult.getResult();
            if (elements != null && elements.size() > 0) {
                list.addAll(elements);
            }
            cursor = scanResult.getStringCursor();
            if ("0".equals(cursor)) {
                break;
            }
        }
        return list;
    }

    public static Set<String> keys(String matchKey) {
        String prefix = "core:";
        String tenantId = UserContext.getTenantId();
        if (StrUtil.isNotEmpty(tenantId)) {
            prefix += tenantId + ":";
        }
        if (!StrUtil.startWith(matchKey, prefix)) {
            matchKey = prefix + matchKey;
        }
        Set<String> list = new HashSet<>();
        if(StrUtil.isEmpty(XXLConfUtil.redisMode) || RedisMode.CLUSTER.equals(XXLConfUtil.redisMode)){
            try {
                JedisCluster jedisCluster = SpringContextUtil.getBean(JedisCluster.class);
                Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
                for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
                    Jedis jedis = entry.getValue().getResource();
                    // 判断非从节点(因为若主从复制，从节点会跟随主节点的变化而变化)
                    if (!jedis.info("replication").contains("role:slave")) {
                        Set<String> keys = getScan(jedis, matchKey);
                        if (keys.size() > 0) {
                            Map<Integer, List<String>> map = new HashMap<>();
                            for (String key : keys) {
                                // cluster模式执行多key操作的时候，这些key必须在同一个slot上，不然会报:JedisDataException:
                                // CROSSSLOT Keys in request don't hash to the same slot
                                int slot = JedisClusterCRC16.getSlot(key);
                                // 按slot将key分组，相同slot的key一起提交
                                if (map.containsKey(slot)) {
                                    map.get(slot).add(key);
                                } else {
                                    map.put(slot, Lists.newArrayList(key));
                                }
                            }
                            for (Map.Entry<Integer, List<String>> integerListEntry : map.entrySet()) {
                                // System.out.println("integerListEntry="+integerListEntry);
                                list.addAll(integerListEntry.getValue());
                            }
                        }
                    }
                }
            } finally {
                return list;
            }
        }
        if(RedisMode.SENTINEL.equals(XXLConfUtil.redisMode) || RedisMode.STANDALONE.equals(XXLConfUtil.redisMode)){
            Pool<Jedis> pool = (Pool<Jedis>)SpringContextUtil.getBean("jedisPool");
            Jedis resource = pool.getResource();
            Set<String> keys = getScan(resource, matchKey);
            if (keys.size() > 0) {
                list.addAll(keys);
            }
        }
        return list;

    }

    public static Set<String> keys2(String matchKey) {
        String prefix = "core:";
        if (!StrUtil.startWith(matchKey, prefix)) {
            matchKey = prefix + matchKey;
        }
        Set<String> list = new HashSet<>();
        if(StrUtil.isEmpty(XXLConfUtil.redisMode) || RedisMode.CLUSTER.equals(XXLConfUtil.redisMode)){
            try {
                JedisCluster jedisCluster = SpringContextUtil.getBean(JedisCluster.class);
                Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
                for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
                    Jedis jedis = entry.getValue().getResource();
                    // 判断非从节点(因为若主从复制，从节点会跟随主节点的变化而变化)
                    if (!jedis.info("replication").contains("role:slave")) {
                        Set<String> keys = getScan(jedis, matchKey);
                        if (keys.size() > 0) {
                            Map<Integer, List<String>> map = new HashMap<>();
                            for (String key : keys) {
                                // cluster模式执行多key操作的时候，这些key必须在同一个slot上，不然会报:JedisDataException:
                                // CROSSSLOT Keys in request don't hash to the same slot
                                int slot = JedisClusterCRC16.getSlot(key);
                                // 按slot将key分组，相同slot的key一起提交
                                if (map.containsKey(slot)) {
                                    map.get(slot).add(key);
                                } else {
                                    map.put(slot, Lists.newArrayList(key));
                                }
                            }
                            for (Map.Entry<Integer, List<String>> integerListEntry : map.entrySet()) {
                                // System.out.println("integerListEntry="+integerListEntry);
                                list.addAll(integerListEntry.getValue());
                            }
                        }
                    }
                }
            } finally {
                return list;
            }
        }
        if(RedisMode.SENTINEL.equals(XXLConfUtil.redisMode) || RedisMode.STANDALONE.equals(XXLConfUtil.redisMode)){
            Pool<Jedis> pool = (Pool<Jedis>)SpringContextUtil.getBean("jedisPool");
            Jedis resource = pool.getResource();
            Set<String> keys = getScan(resource, matchKey);
            if (keys.size() > 0) {
                list.addAll(keys);
            }
        }
        return list;

    }
}