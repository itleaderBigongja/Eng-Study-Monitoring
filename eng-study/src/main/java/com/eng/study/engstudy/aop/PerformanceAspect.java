package com.eng.study.engstudy.aop; // 패키지명 확인

import net.logstash.logback.marker.Markers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

// PerformanceAspect → Service 레이어 메서드 실행 시간 측정
@Aspect
@Component
public class PerformanceAspect {

    // Logback의 <logger name="com.eng.study.performance"> 와 일치
    private static final Logger perfLogger = LoggerFactory.getLogger("com.eng.study.performance");

    // Service 패키지 하위의 모든 메소드 감지
    @Pointcut("execution(* com.eng.study.engstudy.service..*(..))")
    public void serviceLayer() {}

    @Around("serviceLayer()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - start;

            Map<String, Object> perfData = new HashMap<>();
            perfData.put("class", joinPoint.getSignature().getDeclaringTypeName());
            perfData.put("method", joinPoint.getSignature().getName());
            perfData.put("execution_time_ms", executionTime);

            perfLogger.info(Markers.appendEntries(perfData), "Method Execution Time");
        }
    }
}