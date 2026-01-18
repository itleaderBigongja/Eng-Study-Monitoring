package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * 알림 히스토리 응답 DTO
 * ============================================================================
 *
 * 역할:
 * - 알림 발생 기록을 클라이언트에 전달
 *
 * 응답 예시:
 * {
 *   "id": 1,
 *   "alertRuleId": 1,
 *   "alertRuleName": "Eng-Study CPU 경고",
 *   "application": "eng-study",
 *   "triggeredAt": "2026-01-18T14:30:00",
 *   "currentValue": 85.5,
 *   "thresholdValue": 80.0,
 *   "message": "eng-study의 CPU 사용률가 85.5%로 임계치 80.0%를 초과했습니다.",
 *   "severity": "WARNING",
 *   "resolved": false,
 *   "resolvedAt": null,
 *   "durationMinutes": null,
 *   "notificationSent": true,
 *   "notificationMethods": "SLACK",
 *   "notificationResult": "성공"
 * }
 *
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistoryResponseDTO {

    /**
     * 알림 히스토리 ID
     */
    private Long id;

    /**
     * 알림 규칙 ID
     */
    private Long alertRuleId;

    /**
     * 알림 규칙 이름 (조인 결과)
     */
    private String alertRuleName;

    /**
     * 애플리케이션 이름
     */
    private String application;

    /**
     * 메트릭 타입
     */
    private String metricType;

    /**
     * 알림 발생 시간
     */
    private LocalDateTime triggeredAt;

    /**
     * 알림 발생 시점의 실제 값
     */
    private BigDecimal currentValue;

    /**
     * 발생 시점의 임계값
     */
    private BigDecimal thresholdValue;

    /**
     * 알림 메시지
     */
    private String message;

    /**
     * 심각도
     * CRITICAL, ERROR, WARNING, INFO
     */
    private String severity;

    /**
     * 해결 여부
     */
    private Boolean resolved;

    /**
     * 해결 시간
     */
    private LocalDateTime resolvedAt;

    /**
     * 해결 메시지
     */
    private String resolvedMessage;

    /**
     * 알림 지속 시간 (분)
     */
    private Long durationMinutes;

    /**
     * 알림 전송 여부
     */
    private Boolean notificationSent;

    /**
     * 전송된 알림 방법
     */
    private String notificationMethods;

    /**
     * 알림 전송 결과
     */
    private String notificationResult;

    /**
     * 알림 전송 에러 메시지
     */
    private String notificationError;

    // ========================================================================
    // 헬퍼 메서드
    // ========================================================================

    /**
     * 심각도 문자열 (UI 표시용)
     *
     * 해결되지 않은 알림은 심각도를 높게 표시
     *
     * @return 심각도 문자열
     */
    public String getDisplaySeverity() {
        if (!resolved) {
            return "HIGH";
        }

        if (durationMinutes != null && durationMinutes < 60) {
            return "MEDIUM";
        }

        return "LOW";
    }

    /**
     * 심각도별 색상 코드 (UI용)
     *
     * @return CSS 색상 클래스
     */
    public String getSeverityColorClass() {
        if (!resolved) {
            return "red";
        }

        if (severity == null) {
            return "gray";
        }

        return switch (severity) {
            case "CRITICAL" -> "red";
            case "ERROR" -> "orange";
            case "WARNING" -> "yellow";
            case "INFO" -> "blue";
            default -> "gray";
        };
    }

    /**
     * 해결 상태 텍스트
     *
     * @return "해결됨" 또는 "미해결"
     */
    public String getResolvedStatusText() {
        return resolved != null && resolved ? "해결됨" : "미해결";
    }

    /**
     * 알림 발생 시간 포맷 (UI용)
     *
     * @return 포맷된 시간 문자열
     */
    public String getTriggeredAtFormatted() {
        if (triggeredAt == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return triggeredAt.format(formatter);
    }

    /**
     * 해결 시간 포맷 (UI용)
     *
     * @return 포맷된 시간 문자열
     */
    public String getResolvedAtFormatted() {
        if (resolvedAt == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return resolvedAt.format(formatter);
    }

    /**
     * 지속 시간 텍스트 (UI용)
     *
     * @return 포맷된 지속 시간 문자열
     */
    public String getDurationText() {
        if (durationMinutes == null) {
            return "-";
        }

        if (durationMinutes < 60) {
            return durationMinutes + "분";
        }

        long hours = durationMinutes / 60;
        long minutes = durationMinutes % 60;

        if (minutes == 0) {
            return hours + "시간";
        }

        return hours + "시간 " + minutes + "분";
    }

    /**
     * 현재 값 포맷 (단위 포함)
     *
     * @return 포맷된 현재 값
     */
    public String getCurrentValueFormatted() {
        if (currentValue == null) {
            return "-";
        }

        String unit = getMetricUnit(metricType);
        return String.format("%.2f%s", currentValue, unit);
    }

    /**
     * 임계값 포맷 (단위 포함)
     *
     * @return 포맷된 임계값
     */
    public String getThresholdValueFormatted() {
        if (thresholdValue == null) {
            return "-";
        }

        String unit = getMetricUnit(metricType);
        return String.format("%.2f%s", thresholdValue, unit);
    }

    /**
     * 메트릭 타입 → 단위 변환
     */
    private String getMetricUnit(String metricType) {
        if (metricType == null) {
            return "";
        }

        return switch (metricType) {
            case "CPU_USAGE", "HEAP_USAGE", "ERROR_RATE" -> "%";
            case "TPS" -> " req/s";
            case "DB_CONNECTIONS" -> "개";
            case "DB_SIZE" -> " MB";
            default -> "";
        };
    }

    /**
     * 알림 전송 상태 아이콘 (UI용)
     *
     * @return 이모지 아이콘
     */
    public String getNotificationStatusIcon() {
        if (notificationSent == null) {
            return "⚪";
        }

        return notificationSent ? "✅" : "❌";
    }

    /**
     * 알림 전송 상태 텍스트
     *
     * @return 전송 상태 문자열
     */
    public String getNotificationStatusText() {
        if (notificationSent == null) {
            return "미전송";
        }

        if (notificationSent) {
            return "전송 성공";
        }

        return "전송 실패" + (notificationError != null ? ": " + notificationError : "");
    }

    @Override
    public String toString() {
        return "AlertHistoryResponseDTO{" +
                "id=" + id +
                ", alertRuleName='" + alertRuleName + '\'' +
                ", application='" + application + '\'' +
                ", triggeredAt=" + triggeredAt +
                ", currentValue=" + currentValue +
                ", resolved=" + resolved +
                ", notificationSent=" + notificationSent +
                '}';
    }
}