package com.study.monitoring.studymonitoring.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 알람 설정 및 이력 VO
 *
 * 테이블: MONITORING_ALERT
 *
 * 설명:
 * - 임계값 기반 알람 설정
 * - 이메일/Slack 알림 연동
 * - 알람 발생 이력 추적
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertVO {

    private Long alertId;                  // 알람 ID (PK)
    private String alertName;              // 알람명
    private String alertType;              // 알람 유형 (THRESHOLD, PATTERN, ANOMALY)
    private String metricType;             // 메트릭 유형
    private String conditionOperator;      // 조건 연산자 (GT, GTE, LT, LTE, EQ, NEQ)
    private BigDecimal thresholdValue;     // 임계값
    private String severity;               // 심각도 (INFO, WARNING, ERROR, CRITICAL)
    private Boolean isActive;              // 활성 여부
    private String notificationEmail;      // 알림 이메일
    private String notificationSlack;      // 알림 Slack 채널
    private LocalDateTime lastTriggeredAt; // 마지막 발생 시간
    private Integer triggerCount;          // 발생 횟수
    private LocalDateTime createdAt;       // 생성일시
    private String createdId;              // 생성자 ID
    private LocalDateTime updatedAt;       // 수정일시
    private String updatedId;              // 수정자 ID

    /**
     * 알람 유형 Enum
     */
    public enum AlertType {
        THRESHOLD,      // 임계값 기반
        PATTERN,        // 패턴 기반
        ANOMALY         // 이상 탐지
    }

    /**
     * 조건 연산자 Enum
     */
    public enum ConditionOperator {
        GT,     // Greater Than (>)
        GTE,    // Greater Than or Equal (>=)
        LT,     // Less Than (<)
        LTE,    // Less Than or Equal (<=)
        EQ,     // Equal (=)
        NEQ     // Not Equal (!=)
    }

    /**
     * 심각도 Enum
     */
    public enum Severity {
        INFO,       // 정보
        WARNING,    // 경고
        ERROR,      // 에러
        CRITICAL    // 긴급
    }

    /**
     * 알람 활성 상태 확인
     *
     * @return 활성 여부
     */
    public boolean isAlertActive() {
        return this.isActive != null && this.isActive;
    }

    /**
     * 알람 발생 기록
     */
    public void triggerAlert() {
        this.lastTriggeredAt = LocalDateTime.now();
        if (this.triggerCount == null) {
            this.triggerCount = 1;
        } else {
            this.triggerCount++;
        }
    }

    /**
     * Critical 알람인지 확인
     *
     * @return Critical 여부
     */
    public boolean isCritical() {
        return Severity.CRITICAL.name().equals(this.severity);
    }

    /**
     * 조건 평가
     *
     * @param actualValue 실제 값
     * @return 조건 만족 여부
     */
    public boolean evaluateCondition(BigDecimal actualValue) {
        if (actualValue == null || thresholdValue == null) {
            return false;
        }

        return switch (ConditionOperator.valueOf(this.conditionOperator)) {
            case GT -> actualValue.compareTo(thresholdValue) > 0;
            case GTE -> actualValue.compareTo(thresholdValue) >= 0;
            case LT -> actualValue.compareTo(thresholdValue) < 0;
            case LTE -> actualValue.compareTo(thresholdValue) <= 0;
            case EQ -> actualValue.compareTo(thresholdValue) == 0;
            case NEQ -> actualValue.compareTo(thresholdValue) != 0;
        };
    }
}