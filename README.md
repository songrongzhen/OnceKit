# OnceKit — 通用幂等与防重中间件

> ✅ Spring Boot 3.0 + JDK 17 + Redis + Lua 
> 🚀 一行注解，解决重复提交、重复回调、重复消费


## 🌟 特性

- **通用**：支持 Web、MQ、定时任务
- **安全**：Redis Lua 原子操作，防并发
- **高可用**：自动过期，无死锁
- **开箱即用**：Spring Boot Starter，5 分钟接入
- **生产就绪**：已在多个项目验证

## 📦 快速开始

### 1. 引入依赖

[![Maven Central](https://img.shields.io/badge/Maven%20Central-Search-blue)](https://mvnrepository.com/artifact/io.github.songrongzhen/once-kit-spring-boot-starter/)

```xml
<dependency>
    <groupId>io.github.songrongzhen</groupId>
    <artifactId>once-kit-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```
### 2. 加上注解
```java
@Idempotent(key = "'enroll:idcard:' + #candidate.idCard", expire = 300)
```
   ```java
    @PostMapping("/enroll")
    @Idempotent(key = "'enroll:idcard:' + #candidate.idCard", expire = 300)
    public String enroll(@RequestBody Candidate candidate) {
        return "报名成功";
    }
   ```

## 🔍 执行流程详解
### 步骤 1️⃣：请求进入 Spring MVC
- Tomcat 接收请求
- DispatcherServlet 将请求路由到 enroll 方法
### 步骤 2️⃣：AOP 切面拦截（关键！）
- OnceKit 的 @Aspect 切面 在方法执行前 拦截
- 因为方法上有 @Idempotent 注解，触发 IdempotentAspect.around() 方法
### 步骤 3️⃣：解析 SpEL 表达式，生成幂等 Key
- 从方法参数中提取 candidate 对象
- 执行 SpEL：'enroll:idcard:' + #candidate.idCard
- 得到实际 Key：
    ```text
  enroll:idcard:11010119900307XXXX
  ```
### 步骤 4️⃣：向 Redis 查询/设置幂等锁（原子操作）
- 调用 RedisIdempotentService.tryLock(key, 300)
- 执行 Lua 脚本（保证原子性）
     ```lua
    if redis.call('GET', KEYS[1]) == ARGV[1] then 
      -- 分支1：key存在且值等于预期值 → 续期（重置过期时间）
      return redis.call('PEXPIRE', KEYS[1], ARGV[2]) 
    else 
      -- 分支2：key不存在/值不匹配 → 尝试加锁（NX：仅当key不存在时SET）
      return redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX') 
    end
    ```
  ✅ 关键点：SET key value PX expire NX 是 Redis 原子命令，确保“查+设”不被并发打断。
### 步骤 5️⃣：根据 Lua 返回值决定是否放行
  - 情况 A：首次请求
     - Redis 返回 "OK"（或 true）
     - tryLock() 返回 true
  AOP 放行 → 执行 enroll() 业务逻辑
  - 情况 B：重复请求（300 秒内）
     - Redis 返回 nil（因为 NX 导致 SET 失败）
     - tryLock() 返回 false
     - AOP 抛出异常：
   ```java
     throw new IllegalStateException("重复请求，请勿重复提交");
   ```
- Spring MVC 捕获异常 → 返回 500 错误（或自定义的全局异常处理）
  ### 步骤 6️⃣：Redis 自动过期（无需清理）
  - 300 秒后，Key 自动消失
  - 下次请求视为新请求，可再次通过


