package com.eng.study.engstudy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** 보안 이벤트 로거 유틸리티
 *  MDC를 사용하여 구조화된 보안 로그 생성 */
public class SecurityEventLogger {
    private static final Logger log = LoggerFactory.getLogger("org.springframework.security");

    /**
     * 보안 이벤트 로깅
     * @param message 로그 메시지
     * @param attackType 공격 타입 (brute_force, sql_injection, xss, csrf, etc.)
     * @param blocked 차단 여부
     * @param sourceIp 출발지 IP
     * @param threatLevel 위협 레벨 (low, medium, high, critical)
     */
    public static void logSecurityEvent(
            String message,
            String attackType,
            boolean blocked,
            String sourceIp,
            String threatLevel) {
        try {
            MDC.put("attack_type", attackType);
            MDC.put("blocked", String.valueOf(blocked));
            MDC.put("source_ip", sourceIp);
            MDC.put("threat_level", threatLevel);

            if ("critical".equalsIgnoreCase(threatLevel) || "high".equalsIgnoreCase(threatLevel)) {
                log.error(message);
            } else {
                log.warn(message);
            }
        } finally {
            MDC.clear();
        }
    }

    /**
     * 로그인 실패 이벤트
     */
    public static void logLoginFailure(String username, String sourceIp, int attemptCount) {
        String message = String.format("Login failed for user '%s' (attempt: %d)", username, attemptCount);
        String threatLevel = attemptCount >= 5 ? "high" : "medium";
        logSecurityEvent(message, "login_failure", false, sourceIp, threatLevel);
    }

    /**
     * 접근 거부 이벤트
     */
    public static void logAccessDenied(String username, String resource, String sourceIp) {
        String message = String.format("Access denied for user '%s' to resource '%s'", username, resource);
        logSecurityEvent(message, "access_denied", true, sourceIp, "high");
    }

    /**
     * CSRF 공격 감지
     */
    public static void logCsrfAttack(String sourceIp) {
        String message = "CSRF attack detected and blocked";
        logSecurityEvent(message, "csrf", true, sourceIp, "critical");
    }

    /**
     * 무차별 대입 공격 감지
     */
    public static void logBruteForceAttack(String username, String sourceIp, boolean blocked) {
        String message = String.format("Brute force attack detected for user '%s'", username);
        logSecurityEvent(message, "brute_force", blocked, sourceIp, "critical");
    }
}
