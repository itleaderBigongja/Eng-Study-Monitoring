package com.study.monitoring.studymonitoring.model.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * MONITORING_ALERT_HISTORY 테이블 VO
 * ============================================================================
 *
 * 역할:
 * - DB 테이블과 1:1 매핑
 * - MyBatis ResultMap 매핑
 * - 알림 발생 기록 저장
 *
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistoryVO {

    /**
     * 알림 히스토리 ID (Primary Key)
     */
    private Long historyId;

    /**
     * 알림 규칙 ID (Foreign Key)
     */
    private Long alertRuleId;

    /**
     * 알림 발생 시간
     */
    private LocalDateTime triggeredAt;

    /**
     * 알림 발생 시점의 실제 값
     * 예: 85.5 (CPU 85.5%)
     */
    private BigDecimal currentValue;

    /**
     * 발생 시점의 임계값
     */
    private BigDecimal thresholdValue;

    /**
     * 알림 메시지
     * 예: "Eng-Study CPU 사용률이 85.5%로 임계치 80%를 초과했습니다."
     */
    private String alertMessage;

    /**
     * 심각도
     * CRITICAL, ERROR, WARNING, INFO
     */
    private String severity;

    /**
     * 해결 여부
     * true: 정상 복구됨
     * false: 아직 문제 지속 중
     */
    @Builder.Default
    private Boolean isResolved = false;

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
    private Integer durationMinutes;

    /**
     * 알림 전송 여부
     */
    @Builder.Default
    private Boolean notificationSent = false;

    /**
     * 전송된 알림 방법
     * 예: "SLACK", "EMAIL,SLACK"
     */
    private String notificationMethods;

    /**
     * 알림 전송 결과
     * 예: "성공", "Slack 전송 실패: Webhook URL 오류"
     */
    private String notificationResult;

    /**
     * 알림 전송 에러 메시지
     */
    private String notificationError;

    /**
     * 애플리케이션 이름 (비정규화)
     */
    private String application;

    /**
     * 메트릭 타입 (비정규화)
     */
    private String metricType;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    // ========================================================================
    // 조인 결과 필드 (AlertRule 조인 시 사용)
    // ========================================================================

    /**
     * 알림 규칙 이름 (조인 결과)
     */
    @Builder.Default
    private String alertName = null;

    // ========================================================================
    // 헬퍼 메서드
    // ========================================================================

    /**
     * 알림 해결 처리
     *
     * @param resolveMessage 해결 메시지
     */
    public void resolve(String resolveMessage) {
        this.isResolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedMessage = resolveMessage;

        // 지속 시간 계산
        if (this.triggeredAt != null) {
            this.durationMinutes = (int) java.time.Duration
                    .between(this.triggeredAt, this.resolvedAt)
                    .toMinutes();
        }
    }

    /**
     * 알림 전송 성공 처리
     *
     * @param methods 전송된 방법
     * @param result 전송 결과
     */
    public void markNotificationSent(String methods, String result) {
        this.notificationSent = true;
        this.notificationMethods = methods;
        this.notificationResult = result;
    }

    /**
     * 알림 전송 실패 처리
     *
     * @param methods 시도한 방법
     * @param error 에러 메시지
     */
    public void markNotificationFailed(String methods, String error) {
        this.notificationSent = false;
        this.notificationMethods = methods;
        this.notificationError = error;
        this.notificationResult = "실패: " + error;
    }

    @Override
    public String toString() {
        return "AlertHistoryVO{" +
                "historyId=" + historyId +
                ", alertRuleId=" + alertRuleId +
                ", triggeredAt=" + triggeredAt +
                ", currentValue=" + currentValue +
                ", isResolved=" + isResolved +
                ", notificationSent=" + notificationSent +
                '}';
    }
}