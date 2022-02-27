package com.ytdinfo.inndoo.config.redis;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisAtomicService {

    public RedisAtomicService() {
        script = new DefaultRedisScript<>();
        String text = " local leftvalue = redis.call('get', KEYS[1]); "
                + " if ARGV[1] - leftvalue > 0 then return nil; else "
                + " return redis.call('decrby', KEYS[1], ARGV[1]); end; ";
        script.setScriptText(text);
        script.setResultType(Long.class);

        scriptIncrby = new DefaultRedisScript<>();
        String textIncrby = " local usedvalue = redis.call('get', KEYS[1]); "
                + " if tonumber(ARGV[2]) - usedvalue < tonumber(ARGV[1]) then return nil; else "
                + " return redis.call('incrby', KEYS[1], ARGV[1]); end; ";
        scriptIncrby.setScriptText(textIncrby);
        scriptIncrby.setResultType(Long.class);

        scriptValue = new DefaultRedisScript<>();
        String textValue = " return tonumber(redis.call('get', KEYS[1])); ";
        scriptValue.setScriptText(textValue);
        scriptValue.setResultType(Long.class);
    }

    public void setStringSerializer() {

    }

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private StringRedisTemplate redisTemplate;

    private synchronized StringRedisTemplate atomicStringRedisTemplate() {
        if (redisTemplate == null) {
            synchronized (RedisAtomicService.class) {
                if (redisTemplate == null) {
                    StringRedisTemplate redisTemplate = new StringRedisTemplate();
                    redisTemplate.setConnectionFactory(this.redisConnectionFactory);
                    DefaultStrSerializer stringSerializer = new DefaultStrSerializer();
                    redisTemplate.setKeySerializer(stringSerializer);
                    redisTemplate.setValueSerializer(stringSerializer);
                    redisTemplate.setHashKeySerializer(stringSerializer);
                    redisTemplate.setHashValueSerializer(stringSerializer);
                    redisTemplate.afterPropertiesSet();
                    this.redisTemplate = redisTemplate;
                }
            }
        }

        return this.redisTemplate;
    }

    private DefaultRedisScript<Long> script;

    private DefaultRedisScript<Long> scriptIncrby;

    private DefaultRedisScript<Long> scriptValue;

    private String getKey(String key) {
        String prefix = "act:";
        String tenantId = UserContext.getTenantId();
        if (StrUtil.isNotEmpty(tenantId)) {
            prefix += tenantId + ":";
        }
        key = prefix + key;
        return key;
    }

    /***
     * 原子性操作
     * @param key
     * @param num
     * @return
     */
    public Long increment(String key, long num) {
        setStringSerializer();
        key = getKey(key);
        Long increment = atomicStringRedisTemplate().boundValueOps(key).increment(num);
        return increment;

    }

    public void deleteKey(String key) {
        setStringSerializer();
        key = getKey(key);
        atomicStringRedisTemplate().delete(key);
    }

    public Object getHkeyValue(String key,String hkey) {
        setStringSerializer();
        key = getKey(key);
        return atomicStringRedisTemplate().opsForHash().get(key,hkey);
    }

    public Boolean exists(String key) {
        setStringSerializer();
        key = getKey(key);
        return atomicStringRedisTemplate().hasKey(key);
    }

    /***
     * 原子操作减库存直到为0 但返回结果小于0的时候说明没有库存
     * @param key
     * @param num
     * @return
     */
    public Long decreaseUntil0(String key, long num) {
        key = getKey(key);
        atomicStringRedisTemplate().setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        List<String> keyList = new ArrayList<String>();
        keyList.add(key);
        Long left = atomicStringRedisTemplate().execute(script, keyList, num);
        if (left == null) {
            return -1L;
        }
        return left;
    }

    public Long incrementUntilMaxNum(String key, int num, int max) {
        key = getKey(key);
        atomicStringRedisTemplate().setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        List<String> keyList = new ArrayList<String>();
        keyList.add(key);
        Long left = atomicStringRedisTemplate().execute(scriptIncrby, keyList, num, max);
        if (left == null) {
            return -1L;
        }
        return left;
    }

    public Long getValue(String key) {
        key = getKey(key);
        atomicStringRedisTemplate().setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        List<String> keyList = new ArrayList<String>();
        keyList.add(key);
        Long usedvalue = atomicStringRedisTemplate().execute(scriptValue, keyList);
        return usedvalue;
    }

    private List<String> getKeyList(String key) {
        key = getKey(key);
        List<String> keyList = new LinkedList<>();
        keyList.add(key);
        return keyList;
    }

    /***
     * hash数据结构存储的总次数
     * @param key
     * @param hkeys
     * @return
     */
    public Long getHashValue(String key, List<String> hkeys) {
        String append = "{" + key + "}";
        List<String> keyList = getKeyList(append);
        String keyAppend = "";
        int len = 0;
        for (String hkey : hkeys) {
            //keyList.add(append + hkey);
            keyAppend += ",\"" + append + hkey + "\"";
            len++;
        }
        System.out.println(JSONUtil.toJsonStr(keyList));
        DefaultRedisScript<Long> scriptHashTime = new DefaultRedisScript<>();
        String textValue = "local key = KEYS[1]\n" +
                "local total = 0\n" +
                "local timeinfo = redis.call('HMGET',key" + keyAppend + ")\n" +
                "for i=1," + len + " do\n" +
                "\tif timeinfo[i] ~= nil and timeinfo[i] ~= false then\n" +
                "\t\ttotal = total + timeinfo[i]\n" +
                "\tend\n" +
                "end\n" +
                "return total";
        //        String textValue = "local key = KEYS[1]\n" +
//                "local total = '0'\n" +
//                "local timeinfo = redis.call('HMGET',key" + keyAppend + ")\n" +
//                "for i=1," + len + " do\n" +
//                "\t\ttotal = total..tostring(timeinfo[i])\n" +
//                "end\n" +
//                "return total";
        System.out.println(textValue);
        scriptHashTime.setScriptText(textValue);
        scriptHashTime.setResultType(Long.class);
        System.out.println(keyList.get(0));
        System.out.println(hkeys.get(0));
        Long usedvalue = atomicStringRedisTemplate().execute(scriptHashTime, keyList);
        return usedvalue;
    }

    public Long hset(String key, String hkey, int value) {
        String append = "{" + key + "}";
        List<String> keyList = getKeyList(append);
        keyList.add(append + hkey);
        DefaultRedisScript<Long> scriptHashTime = new DefaultRedisScript<>();
        String textValue = "redis.call('HSET',KEYS[1],KEYS[2],ARGV[1]);return 0;";
        scriptHashTime.setScriptText(textValue);
        scriptHashTime.setResultType(Long.class);
        Long usedvalue = atomicStringRedisTemplate().execute(scriptHashTime, keyList, value);
        return usedvalue;
    }

    public void expire(String key, long interval, TimeUnit timeUnit) {
        redisTemplate.expire(key, interval, timeUnit);
    }

    public void expireForHash(String key, long interval, TimeUnit timeUnit) {
        redisTemplate.expire("{" + key + "}", interval, timeUnit);
    }
}
