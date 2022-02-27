package com.ytdinfo.inndoo.config.redis;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.core.entity.RedisVo;
import com.ytdinfo.core.util.JedisUtil;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.integration.redis.util.RedisLockRegistry;
import redis.clients.jedis.*;
import redis.clients.util.Pool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Exrickx
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @XxlConf("inndoo-sso.redis.cluster.nodes")
    private String clusterNodes;

    @XxlConf("inndoo-sso.redis.cluster.password")
    private String clusterPassword;

    @XxlConf("inndoo-sso.redis.host")
    private String host;

    @XxlConf("inndoo-sso.redis.password")
    private String password;

    @XxlConf("inndoo-sso.redis.mastername")
    private String masterName;

    @XxlConf("inndoo-sso.redis.mode")
    private String mode;

    @XxlConf("inndoo-sso.redis.database")
    private Integer database;

    private String secret = "rPMYoxxUmZVnGh3n";

    @Bean
    public RedisConfiguration redisConfiguration(){
        String redisHost = clusterNodes;
        String redisPassword = clusterPassword;
        if(StrUtil.isEmpty(redisHost)) {
            redisHost = host;
        }
        if(StrUtil.isEmpty(redisPassword)){
            redisPassword = password;
        }
        redisHost = AESUtil.decrypt(redisHost, secret);
        redisPassword = AESUtil.decrypt(redisPassword, secret);

        if(RedisMode.SENTINEL.equals(mode)){
            String[] hosts = redisHost.split(",");
            Set<String> setRedisNode = new HashSet<>();
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i].trim();
                setRedisNode.add(host);
            }
            RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration(masterName, setRedisNode);
            redisSentinelConfiguration.setPassword(RedisPassword.of(redisPassword));
            return redisSentinelConfiguration;
        }else if(RedisMode.STANDALONE.equals(mode)){
            String h = redisHost.split(":")[0].trim();
            int p = Integer.parseInt(redisHost.split(":")[1].trim());
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(h,p);
            redisStandaloneConfiguration.setPassword(RedisPassword.of(redisPassword));
            if(database != null){
                redisStandaloneConfiguration.setDatabase(database);
            }
            return redisStandaloneConfiguration;
        }else{
            RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
            String[] hosts = AESUtil.decrypt(clusterNodes, secret).split(",");
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i].trim();
                String h = host.split(":")[0].trim();
                int p = Integer.parseInt(host.split(":")[1].trim());
                redisClusterConfiguration.addClusterNode(new RedisClusterNode(h,p));
            }
            redisClusterConfiguration.setMaxRedirects(hosts.length);
            redisClusterConfiguration.setPassword(RedisPassword.of(redisPassword));
            return redisClusterConfiguration;
        }

    }

    /**
     * 连接池配置信息
     * @return
     */
    @Bean
    public JedisPoolConfig jedisPoolConfig() {

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //最大连接数
        jedisPoolConfig.setMaxTotal(1000);
        //最小空闲连接数
        jedisPoolConfig.setMinIdle(50);
        //当池内没有可用的连接时，最大等待时间
        jedisPoolConfig.setMaxWaitMillis(1000);
        // 检查连接可用性, 确保获取的redis实例可用
        // 在获取连接的时候检查有效性, 默认false
        jedisPoolConfig.setTestOnBorrow(true);
        // 调用returnObject方法时，是否进行有效检查
        jedisPoolConfig.setTestOnReturn(true);
        // Idle时进行连接扫描
        jedisPoolConfig.setTestWhileIdle(true);
        // 表示idle object evitor两次扫描之间要sleep的毫秒数
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000);
        // 表示idle object evitor每次扫描的最多的对象数
        jedisPoolConfig.setNumTestsPerEvictionRun(10);
        // 表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        jedisPoolConfig.setMinEvictableIdleTimeMillis(60000);


        //        设置sso redis配置
        RedisVo redisVo=new RedisVo();
        redisVo.setClusterRedisHost(clusterNodes);
        redisVo.setClusterRedisPassword(clusterPassword);
        redisVo.setHost(host);
        redisVo.setPassword(password);
        redisVo.setMode(mode);
        redisVo.setDatabaseStr(database==null?null:String.valueOf(database));
        redisVo.setMasterName(masterName);
        JedisUtil.setRedisVo(redisVo);
        return jedisPoolConfig;
    }

    @Bean
    public JedisCluster jedisCluster(){
        if(RedisMode.CLUSTER.equals(mode) || StrUtil.isEmpty(mode)){
            JedisPoolConfig jedisPoolConfig = jedisPoolConfig();
            RedisClusterConfiguration redisClusterConfiguration = (RedisClusterConfiguration)redisConfiguration();
            Set<RedisNode> clusterNodes = redisClusterConfiguration.getClusterNodes();
            Set<HostAndPort> hostAndPortSet = new HashSet<>();
            clusterNodes.forEach(redisNode -> {
                hostAndPortSet.add(new HostAndPort(redisNode.getHost(),redisNode.getPort()));
            });
            JedisCluster jedisCluster = new JedisCluster(hostAndPortSet, 2000, 2000, 2, AESUtil.decrypt(clusterPassword, secret), jedisPoolConfig);
            return jedisCluster;
        }
        return null;
    }

    @Bean("jedisPool")
    public Pool<Jedis> jedisPool(){
        String redisPassword = clusterPassword;
        if(StrUtil.isEmpty(redisPassword)){
            redisPassword = password;
        }
        redisPassword = AESUtil.decrypt(redisPassword, secret);
        Pool<Jedis> pool = new JedisPool();
        if(RedisMode.SENTINEL.equals(mode)){
            RedisSentinelConfiguration redisSentinelConfiguration = (RedisSentinelConfiguration)redisConfiguration();
            Set<String> sentinels = new HashSet<>();
            Set<RedisNode> redisNodes = redisSentinelConfiguration.getSentinels();
            redisNodes.forEach(redisNode -> {
                sentinels.add(redisNode.asString());
            });
            JedisPoolConfig jedisPoolConfig = jedisPoolConfig();
            pool = new JedisSentinelPool(masterName,sentinels,jedisPoolConfig,1000,redisPassword);
        }
        if(RedisMode.STANDALONE.equals(mode)){
            RedisStandaloneConfiguration redisStandaloneConfiguration = (RedisStandaloneConfiguration)redisConfiguration();
            JedisPoolConfig jedisPoolConfig = jedisPoolConfig();
            pool = new JedisPool(jedisPoolConfig,redisStandaloneConfiguration.getHostName(),redisStandaloneConfiguration.getPort(),1000,redisPassword,redisStandaloneConfiguration.getDatabase());
        }
        return pool;
    }

    @Bean
    public RedisClusterClient redisClusterClient(){
        if(RedisMode.CLUSTER.equals(mode) || StrUtil.isEmpty(mode)){
            List<RedisURI> nodes = new ArrayList<>();
            String[] hosts = AESUtil.decrypt(clusterNodes, secret).split(",");
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i].trim();
                String h = host.split(":")[0].trim();
                int p = Integer.parseInt(host.split(":")[1].trim());
                RedisURI redisURI = RedisURI.create(h, p);
                redisURI.setPassword(AESUtil.decrypt(clusterPassword, secret));
                redisURI.setDatabase(0);
                nodes.add(redisURI);
            }
            ClientResources clientResources = ClientResources.create();
            RedisClusterClient clusterClient = RedisClusterClient.create(clientResources,nodes);
            ClusterClientOptions clientOptions = ClusterClientOptions.builder().autoReconnect(true).maxRedirects(3).build();
            clusterClient.setOptions(clientOptions);
            return clusterClient;
        }
        return null;
    }


    /**
     * jedis连接工厂
     * @param jedisPoolConfig
     * @return
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(JedisPoolConfig jedisPoolConfig,RedisConfiguration redisConfiguration) {
        //JedisClientConfiguration.JedisPoolingClientConfigurationBuilder jpcb =
        //        (JedisClientConfiguration.JedisPoolingClientConfigurationBuilder)JedisClientConfiguration.builder();
        //jpcb.poolConfig(jedisPoolConfig);
        //通过构造器来构造jedis客户端配置
        //JedisClientConfiguration jedisClientConfiguration = jpcb.build();

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(jedisPoolConfig);
        LettucePoolingClientConfiguration lettucePoolingClientConfiguration = builder.build();


        //单机配置 + 客户端配置 = jedis连接工厂
        //JedisConnectionFactory connectionFactory = new JedisConnectionFactory(redisClusterConfiguration, jedisClientConfiguration);
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisConfiguration,lettucePoolingClientConfiguration);
        return lettuceConnectionFactory;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        Jackson2JsonRedisSerializer<Object> redisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        PrefixStringRedisSerializer prefixStringRedisSerializer = new PrefixStringRedisSerializer();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

        redisSerializer.setObjectMapper(objectMapper);

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(prefixStringRedisSerializer));

        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
        InndooRedisCacheManager redisCacheManager = new InndooRedisCacheManager(cacheWriter,cacheConfiguration);

        return redisCacheManager;
    }

    @Bean
    RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        redisTemplate.setDefaultSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setKeySerializer(new PrefixStringRedisSerializer());
        redisTemplate.setEnableTransactionSupport(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new PrefixStringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockRegistry(redisConnectionFactory, "inndoo-lock");
    }

}