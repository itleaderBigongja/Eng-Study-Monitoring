package com.eng.study.engstudy.aop;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers; // 이거 필수!
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.HashMap;
import java.util.Map;

// LoggingAspect → Controller 레이어 메서드 실행 시간 측정
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // "PERFORMANCE_LOGGER"라는 이름으로 별도 로거를 생성 (logback-spring.xml에서 파일 분리용)
    private final org.slf4j.Logger perfLogger = org.slf4j.LoggerFactory.getLogger("PERFORMANCE_LOGGER");

    @Around("execution(* com.eng.study.engstudy.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Object proceed = joinPoint.proceed();

        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();

        Map<String, Object> perfMap = new HashMap<>();
        perfMap.put("execution_time_ms", totalTimeMillis); // [수정됨] duration_ms -> execution_time_ms
        perfMap.put("class", joinPoint.getSignature().getDeclaringTypeName());
        perfMap.put("method", joinPoint.getSignature().getName());

        perfLogger.info(Markers.appendEntries(perfMap), "Performance Data"); // 메시지도 명확하게 변경
        return proceed;
    }
}