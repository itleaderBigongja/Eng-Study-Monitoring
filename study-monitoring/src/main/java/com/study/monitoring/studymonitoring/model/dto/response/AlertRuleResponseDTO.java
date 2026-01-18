package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * 알림 규칙 응답 DTO
 * ============================================================================
 *
 * 역할:
 * - 클라이언트에게 알림 규칙 데이터 전달
 * - VO → DTO 변환 결과
 *
 * 응답 예시:
 * {
 *   "id": 1,
 *   "name": "Eng-Study CPU 경고",
 *   "application": "eng-study",
 *   "metricType": "CPU_USAGE",
 *   "condition": ">",
 *   "threshold": 80.0,
 *   "durationMinutes": 5,
 *   "notificationMethods": ["SLACK"],
 *   "active": true,
 *   "createdAt": "2026-01-18T10:00:00",
 *   "updatedAt": "2026-01-18T10:00:00",
 *   "conditionDescription": "CPU 사용률 > 80.0%"
 * }
 *
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleResponseDTO {

    /**
     * 알림 규칙 ID
     */
    private Long id;

    /**
     * 알림 규칙 이름
     */
    private String name;

    /**
     * 애플리케이션 이름
     */
    private String application;

    /**
     * 알림 유형
     * 예: THRESHOLD
     */
    private String alertType;

    /**
     * 메트릭 타입
     */
    private String metricType;

    /**
     * 조건 연산자
     */
    private String condition;

    /**
     * 임계치
     */
    private BigDecimal threshold;

    /**
     * 지속 시간 (분)
     */
    private Integer durationMinutes;

    /**
     * 심각도
     * CRITICAL, ERROR, WARNING, INFO
     */
    private String severity;

    /**
     * 알림 수신 방법 리스트
     */
    private List<String> notificationMethods;

    /**
     * 알림 이메일
     */
    private String notificationEmail;

    /**
     * 알림 Slack 채널
     */
    private String notificationSlack;

    /**
     * 활성화 여부
     */
    private Boolean active;

    /**
     * 마지막 발생 시간
     */
    private LocalDateTime lastTriggeredAt;

    /**
     * 발생 횟수
     */
    private Integer triggerCount;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;

    /**
     * 조건 설명 (UI 표시용 - 계산됨)
     * 예: "CPU 사용률 > 80%"
     */
    private String conditionDescription;

    // ========================================================================
    // 헬퍼 메서드
    // ========================================================================

    /**
     * 조건 설명 생성 (Getter 호출 시 자동 계산)
     *
     * @return 사람이 읽기 쉬운 조건 문자열
     */
    public String getConditionDescription() {
        if (conditionDescription != null && !conditionDescription.isEmpty()) {
            return conditionDescription;
        }

        String metricName = getMetricDisplayName(metricType);
        String unit = getMetricUnit(metricType);

        this.conditionDescription = String.format("%s %s %s%s",
                metricName, condition, threshold, unit);

        return this.conditionDescription;
    }

    /**
     * 메트릭 타입 → 표시 이름 변환
     */
    private String getMetricDisplayName(String metricType) {
        if (metricType == null) {
            return "";
        }

        return switch (metricType) {
            case "CPU_USAGE" -> "CPU 사용률";
            case "HEAP_USAGE" -> "Heap 사용률";
            case "TPS" -> "TPS";
            case "ERROR_RATE" -> "에러율";
            case "DB_CONNECTIONS" -> "DB 연결 수";
            case "DB_SIZE" -> "DB 크기";
            default -> metricType;
        };
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
     * 심각도별 색상 코드 (UI용)
     *
     * @return CSS 색상 클래스
     */
    public String getSeverityColorClass() {
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
     * 활성 상태 문자열
     *
     * @return "활성" 또는 "비활성"
     */
    public String getActiveStatusText() {
        return active != null && active ? "활성" : "비활성";
    }

    @Override
    public String toString() {
        return "AlertRuleResponseDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", application='" + application + '\'' +
                ", metricType='" + metricType + '\'' +
                ", condition='" + condition + '\'' +
                ", threshold=" + threshold +
                ", active=" + active +
                ", severity='" + severity + '\'' +
                '}';
    }
}