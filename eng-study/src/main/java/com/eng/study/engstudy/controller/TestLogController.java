package com.eng.study.engstudy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestLogController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 요청 주소: http://localhost:30080/test/critical
    @GetMapping("/test/critical")
    public String triggerCritical() {
        try {
            // 1. Logstash 필터가 감지할 수 있도록 MDC에 키워드 주입
            // (Logstash 설정파일에서 [mdc][log_level] == "CRITICAL"이면 승격시키도록 되어 있다고 가정)
            MDC.put("log_level", "CRITICAL");

            // 2. 로그 발생 (Java에서는 error로 찍지만, ELK에서는 Critical로 보임)
            log.error("🚨 [TEST] 이것은 테스트용 Critical 로그입니다.");

            return "CRITICAL 로그 발생 완료";
        } finally {
            // 3. MDC 초기화 (필수)
            MDC.clear();
        }
    }
}
