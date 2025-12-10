package com.oncekit.starter.aop;

import com.oncekit.core.IdempotentService;
import com.oncekit.core.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@Aspect
public class IdempotentAspect {

    private final IdempotentService idempotentService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final StandardReflectionParameterNameDiscoverer discoverer =
            new StandardReflectionParameterNameDiscoverer();

    public IdempotentAspect(IdempotentService idempotentService) {
        this.idempotentService = idempotentService;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = discoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        // 解析 SpEL key
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        String key = parser.parseExpression(idempotent.key()).getValue(context, String.class);

        if (!idempotentService.tryLock(key, idempotent.expire())) {
            if (idempotent.mode() == Idempotent.Mode.REJECT) {
                throw new IllegalStateException("重复请求，请勿重复提交");
            }
            // TODO: RETURN_CACHE 模式（需结果缓存）
        }

        return joinPoint.proceed();
    }
}