package io.oi.spring.adapter.aop;

import io.oi.core.config.OiCoreProperties;
import io.oi.core.trace.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

@Aspect
public class OiAspect {

    private static final Logger log = LoggerFactory.getLogger(OiAspect.class);

    @Autowired
    private OiCoreProperties properties;

    @Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void webControllerPointcut() {}

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void servicePointcut() {}

    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryPointcut() {}

    @Pointcut("execution(* *(..))")
    public void allMethodsPointcut() {}

    @Around("webControllerPointcut() && allMethodsPointcut()")
    public Object aroundWebController(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getMethod().getName();

        // Check if framework instrumentation is disabled and if the class is a Spring internal one
        if (!isFrameworkEnabled() && className.startsWith("org.springframework")) {
            return joinPoint.proceed();
        }

        log.debug("AOP tracing web controller method: {}.{}", className, methodName);
        
        Object result = null;
        Throwable exception = null;
        try {
            // The agent will handle the detailed instrumentation
            // This AOP advice ensures web entry points are captured
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        }
    }

    @Around("servicePointcut() && allMethodsPointcut()")
    public Object aroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getMethod().getName();

        // Check if framework instrumentation is disabled and if the class is a Spring internal one
        if (!isFrameworkEnabled() && className.startsWith("org.springframework")) {
            return joinPoint.proceed();
        }

        log.debug("AOP tracing service method: {}.{}", className, methodName);
        
        Object result = null;
        Throwable exception = null;
        try {
            // The agent will handle the detailed instrumentation
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        }
    }

    @Around("repositoryPointcut() && allMethodsPointcut()")
    public Object aroundRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getMethod().getName();

        // Check if framework instrumentation is disabled and if the class is a Spring internal one
        if (!isFrameworkEnabled() && className.startsWith("org.springframework")) {
            return joinPoint.proceed();
        }

        log.debug("AOP tracing repository method: {}.{}", className, methodName);
        
        Object result = null;
        Throwable exception = null;
        try {
            // The agent will handle the detailed instrumentation
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        }
    }
    
    private boolean isFrameworkEnabled() {
        return properties != null && properties.getInstrumentation() != null && properties.getInstrumentation().isFramework();
    }
} 