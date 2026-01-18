package com.study.monitoring.studymonitoring.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================================
 * 알림 규칙 생성/수정 요청 DTO
 * ============================================================================
 *
 * 역할:
 * - 클라이언트로부터 알림 규칙 데이터 수신
 * - 입력 검증 (Validation)
 *
 * 요청 예시:
 * {
 *   "name": "Eng-Study CPU 경고",
 *   "application": "eng-study",
 *   "metricType": "CPU_USAGE",
 *   "condition": ">",
 *   "threshold": 80.0,
 *   "durationMinutes": 5,
 *   "notificationMethods": ["SLACK"],
 *   "active": true
 * }
 *
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleRequestDTO {

    /**
     * 알림 규칙 이름
     *
     * 검증:
     * - 필수 입력
     * - 1~255자
     */
    @NotBlank(message = "알림 규칙 이름은 필수입니다")
    @Size(min = 1, max = 255, message = "이름은 1~255자 사이여야 합니다")
    private String name;

    /**
     * 애플리케이션 이름
     *
     * 검증:
     * - 필수 입력
     * - 허용 값: eng-study, monitoring, postgres, elasticsearch
     */
    @NotBlank(message = "애플리케이션은 필수입니다")
    @Pattern(
            regexp = "eng-study|monitoring|postgres|elasticsearch",
            message = "유효하지 않은 애플리케이션입니다 (eng-study, monitoring, postgres, elasticsearch 중 선택)"
    )
    private String application;

    /**
     * 메트릭 타입
     *
     * 검증:
     * - 필수 입력
     * - 허용 값: CPU_USAGE, HEAP_USAGE, TPS, ERROR_RATE, DB_CONNECTIONS, DB_SIZE
     */
    @NotBlank(message = "메트릭 타입은 필수입니다")
    @Pattern(
            regexp = "CPU_USAGE|HEAP_USAGE|TPS|ERROR_RATE|DB_CONNECTIONS|DB_SIZE",
            message = "유효하지 않은 메트릭 타입입니다 (CPU_USAGE, HEAP_USAGE, TPS, ERROR_RATE, DB_CONNECTIONS, DB_SIZE 중 선택)"
    )
    private String metricType;

    /**
     * 조건 연산자
     *
     * 검증:
     * - 필수 입력
     * - 허용 값: >, >=, <, <=, ==
     */
    @NotBlank(message = "조건은 필수입니다")
    @Pattern(
            regexp = ">|>=|<|<=|==",
            message = "유효하지 않은 조건입니다 (>, >=, <, <=, == 중 선택)"
    )
    private String condition;

    /**
     * 임계치
     *
     * 검증:
     * - 필수 입력
     * - 0 이상
     */
    @NotNull(message = "임계치는 필수입니다")
    @DecimalMin(value = "0.0", message = "임계치는 0 이상이어야 합니다")
    @DecimalMax(value = "999999.99", message = "임계치는 999999.99 이하여야 합니다")
    private BigDecimal threshold;

    /**
     * 지속 시간 (분)
     *
     * 검증:
     * - 1~60분
     */
    @NotNull(message = "지속 시간은 필수입니다")
    @Min(value = 1, message = "지속 시간은 최소 1분입니다")
    @Max(value = 60, message = "지속 시간은 최대 60분입니다")
    @Builder.Default
    private Integer durationMinutes = 1;

    /**
     * 알림 수신 방법 리스트
     *
     * 검증:
     * - 최소 1개 이상
     * - 허용 값: EMAIL, SLACK
     */
    @NotEmpty(message = "알림 수신 방법은 최소 1개 이상이어야 합니다")
    @Size(min = 1, max = 10, message = "알림 수신 방법은 1~10개 사이여야 합니다")
    private List<@Pattern(
            regexp = "EMAIL|SLACK",
            message = "유효하지 않은 알림 방법입니다 (EMAIL 또는 SLACK만 가능)"
    ) String> notificationMethods;

    /**
     * 활성화 여부
     *
     * 기본값: true
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * 알림 이메일 (선택사항)
     *
     * EMAIL 방법 선택 시 사용
     */
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String notificationEmail;

    /**
     * 알림 Slack 채널 (선택사항)
     *
     * SLACK 방법 선택 시 사용
     * 예: #monitoring, #alerts
     */
    @Size(max = 100, message = "Slack 채널명은 100자 이하여야 합니다")
    private String notificationSlack;

    @Override
    public String toString() {
        return "AlertRuleRequestDTO{" +
                "name='" + name + '\'' +
                ", application='" + application + '\'' +
                ", metricType='" + metricType + '\'' +
                ", condition='" + condition + '\'' +
                ", threshold=" + threshold +
                ", durationMinutes=" + durationMinutes +
                ", notificationMethods=" + notificationMethods +
                ", active=" + active +
                '}';
    }
}