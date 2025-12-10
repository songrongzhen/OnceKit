package com.oncekit.starter.autoconfigure;

import com.oncekit.core.IdempotentService;
import com.oncekit.core.redis.RedisIdempotentService;
import com.oncekit.starter.aop.IdempotentAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * OnceKit 自动配置类
 * - 不暴露 JedisPool 为 Spring Bean，避免 JMX 注册失败
 * - 复用 Spring Data Redis 配置（host/port/password）
 */
@Configuration
public class OnceKitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentService idempotentService(RedisConnectionFactory redisConnectionFactory,
                                               RedisProperties redisProperties) {
        // 支持 Lettuce（Spring Boot 3 默认客户端）
        if (redisConnectionFactory instanceof LettuceConnectionFactory) {
            LettuceConnectionFactory lettuce = (LettuceConnectionFactory) redisConnectionFactory;
            String host = lettuce.getHostName();
            int port = lettuce.getPort();
            String password = redisProperties.getPassword();

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(redisProperties.getJedis().getPool().getMaxActive());
            poolConfig.setMaxIdle(redisProperties.getJedis().getPool().getMaxIdle());
            poolConfig.setMinIdle(redisProperties.getJedis().getPool().getMinIdle());

            // 添加对 redisProperties.getTimeout() 返回值的空值检查
            Duration timeoutDuration = redisProperties.getTimeout();
            int timeout = timeoutDuration != null ? (int) timeoutDuration.toMillis() : 2000;

            return new RedisIdempotentService(
                    new JedisPool(poolConfig, host, port, timeout, password)
            );
        } else {
            throw new UnsupportedOperationException("目前只支持 Lettuce Redis 连接");
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(IdempotentService idempotentService) {
        return new IdempotentAspect(idempotentService);
    }
}