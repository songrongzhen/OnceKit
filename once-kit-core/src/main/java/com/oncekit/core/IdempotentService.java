package com.oncekit.core;

public interface IdempotentService {

    /**
     * 尝试获取幂等锁
     * @param key 幂等 key
     * @param expire 过期时间（秒）
     * @return true 表示首次请求，false 表示重复
     */
    boolean tryLock(String key, int expire);

    /**
     * 释放锁（可选）
     */
    void unlock(String key);
}