package com.oncekit.core.redis;

import com.oncekit.core.IdempotentService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisIdempotentService implements IdempotentService {

    private final JedisPool jedisPool;

    public RedisIdempotentService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    private static final String LOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('PEXPIRE', KEYS[1], ARGV[2]) " +
                    "else " +
                    "  return redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX') " +
                    "end";

    @Override
    public boolean tryLock(String key, int expire) {
        String requestId = java.util.UUID.randomUUID().toString();
        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(LOCK_SCRIPT, java.util.Collections.singletonList("idempotent:" + key),
                    java.util.Arrays.asList(requestId, String.valueOf(expire * 1000L)));
            return "OK".equals(result) || Boolean.TRUE.equals(result);
        }
    }

    @Override
    public void unlock(String key) {
        // 自动过期，无需主动 unlock
    }
}