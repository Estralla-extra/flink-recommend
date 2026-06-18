package com.recommend.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(20);
        config.setMaxIdle(5);
        config.setJmxEnabled(false);   // 禁用 JMX，避免与 Flink 作业冲突
        return new JedisPool(config, "192.168.249.102", 6379, 2000, "myredis123");
    }
}
