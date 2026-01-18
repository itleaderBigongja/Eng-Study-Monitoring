package com.study.monitoring.studymonitoring.model.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * MONITORING_ALERT_RULE 테이블 VO
 * ============================================================================
 *
 * 역할:
 * - DB 테이블과 1:1 매핑
 * - MyBatis ResultMap 매핑
 *
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleVO {

    /**
     * 알림 규칙 ID (Primary Key)
     */
    private Long alertRuleId;

    /**
     * 알림 규칙 이름 (고유값)
     */
    private String alertName;

    /**
     * 대상 애플리케이션
     * 예: eng-study, monitoring, postgres, elasticsearch
     */
    private String application;

    /**
     * 알림 유형
     * THRESHOLD: 임계치 기반
     * PATTERN: 패턴 기반 (향후 구현)
     * ANOMALY: 이상 탐지 (향후 구현)
     */
    private String alertType;

    /**
     * 메트릭 유형
     * CPU_USAGE, HEAP_USAGE, TPS, ERROR_RATE, DB_CONNECTIONS 등
     */
    private String metricType;

    /**
     * 조건 연산자
     * >: 초과
     * >=: 이상
     * <: 미만
     * <=: 이하
     * ==: 같음
     */
    private String conditionOperator;

    /**
     * 임계값
     */
    private BigDecimal thresholdValue;

    /**
     * 지속 시간 (분)
     * 조건을 만족하는 상태가 이 시간만큼 지속되면 알림 발생
     */
    private Integer durationMinutes;

    /**
     * 심각도
     * CRITICAL: 치명적
     * ERROR: 에러
     * WARNING: 경고
     * INFO: 정보
     */
    private String severity;

    /**
     * 활성 여부
     */
    private Boolean isActive;

    /**
     * 알림 수신 방법 (쉼표로 구분)
     * 예: "EMAIL,SLACK"
     */
    private String notificationMethods;

    /**
     * 알림 이메일
     */
    private String notificationEmail;

    /**
     * 알림 Slack 채널
     * 예: #monitoring, #alerts
     */
    private String notificationSlack;

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
     * 생성자 ID
     */
    private String createdId;

    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    private String updatedId;

    // ========================================================================
    // 헬퍼 메서드
    // ========================================================================

    /**
     * 알림 수신 방법을 List로 변환
     */
    public List<String> getNotificationMethodList() {
        if (notificationMethods == null || notificationMethods.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(notificationMethods.split(","));
    }

    /**
     * List를 문자열로 변환하여 저장
     */
    public void setNotificationMethodList(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            this.notificationMethods = "";
        } else {
            this.notificationMethods = String.join(",", methods);
        }
    }

    /**
     * 조건 검사 (임계치 초과 여부)
     */
    public boolean isThresholdExceeded(Double currentValue) {
        if (currentValue == null) {
            return false;
        }

        BigDecimal current = BigDecimal.valueOf(currentValue);

        return switch (conditionOperator) {
            case ">" -> current.compareTo(thresholdValue) > 0;
            case ">=" -> current.compareTo(thresholdValue) >= 0;
            case "<" -> current.compareTo(thresholdValue) < 0;
            case "<=" -> current.compareTo(thresholdValue) <= 0;
            case "==" -> current.compareTo(thresholdValue) == 0;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return "AlertRuleVO{" +
                "alertRuleId=" + alertRuleId +
                ", alertName='" + alertName + '\'' +
                ", application='" + application + '\'' +
                ", metricType='" + metricType + '\'' +
                ", thresholdValue=" + thresholdValue +
                ", isActive=" + isActive +
                '}';
    }
}