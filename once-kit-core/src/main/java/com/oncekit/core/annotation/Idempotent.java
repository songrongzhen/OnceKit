package com.oncekit.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等 key，支持 SpEL 表达式，如 "pay:{#orderId}"
     */
    String key();

    /**
     * 有效期（秒），默认 60 秒
     */
    int expire() default 60;

    /**
     * 重复请求时的行为
     */
    Mode mode() default Mode.REJECT;

    enum Mode {
        /**
         * 拒绝重复请求，抛出异常
         */
        REJECT,
        /**
         * 返回上次结果（需配合缓存）
         */
        RETURN_CACHE
    }
}