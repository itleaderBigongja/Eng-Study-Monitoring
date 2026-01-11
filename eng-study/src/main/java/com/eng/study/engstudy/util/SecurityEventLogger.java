package com.eng.study.engstudy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 보안 이벤트 로거 유틸리티
 * ✅ [수정] CRITICAL 레벨을 올바르게 생성하도록 개선
 */
public class SecurityEventLogger {
    // ✅ [핵심 수정] 커스텀 로거 사용 (Spring Security 로거 X)
    private static final Logger log = LoggerFactory.getLogger("SecurityLog");

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
            // ✅ MDC에 보안 정보 담기
            MDC.put("attack_type", attackType);
            MDC.put("blocked", String.valueOf(blocked));
            MDC.put("source_ip", sourceIp);
            MDC.put("threat_level", threatLevel);

            // ✅ [핵심 수정] threat_level에 따라 로그 레벨을 정확히 매핑
            switch (threatLevel.toLowerCase()) {
                case "critical":
                    // ⭐ CRITICAL은 반드시 ERROR 레벨로 찍되, MDC에 "CRITICAL" 명시
                    MDC.put("log_level", "CRITICAL");
                    log.error("[CRITICAL] {}", message);
                    break;
                case "high":
                    MDC.put("log_level", "ERROR");
                    log.error("[HIGH] {}", message);
                    break;
                case "medium":
                    MDC.put("log_level", "WARN");
                    log.warn("[MEDIUM] {}", message);
                    break;
                case "low":
                default:
                    MDC.put("log_level", "INFO");
                    log.info("[LOW] {}", message);
                    break;
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