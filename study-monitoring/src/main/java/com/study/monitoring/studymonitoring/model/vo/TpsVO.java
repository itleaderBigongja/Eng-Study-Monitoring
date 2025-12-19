package com.study.monitoring.studymonitoring.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TPS 실시간 감시 VO
 *
 * 테이블: MONITORING_TPS
 *
 * 설명:
 * - 초당 트랜잭션 수(Transaction Per Second)
 * - 요청/성공/실패 카운트
 * - 평균/최소/최대 응답 시간
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TpsVO {

    private Long tpsId;                    // TPS ID (PK)
    private Long processId;                // 프로세스 ID (FK)
    private BigDecimal tpsValue;           // TPS 값 (초당 트랜잭션 수)
    private Long requestCount;             // 요청 수
    private Long successCount;             // 성공 수
    private Long errorCount;               // 에러 수
    private BigDecimal avgResponseTimeMs;  // 평균 응답 시간 (ms)
    private BigDecimal minResponseTimeMs;  // 최소 응답 시간 (ms)
    private BigDecimal maxResponseTimeMs;  // 최대 응답 시간 (ms)
    private BigDecimal peakTps;            // 피크 TPS
    private Boolean isPeak;                // 피크 여부
    private LocalDateTime collectedAt;     // 수집 시간
    private LocalDateTime createdAt;       // 생성일시

    /**
     * 에러율 계산 메서드
     *
     * @return 에러율 (%)
     */
    public BigDecimal calculateErrorRate() {
        if (requestCount == null || requestCount == 0) {
            return BigDecimal.ZERO;
        }

        long totalErrors = errorCount != null ? errorCount : 0L;

        return BigDecimal.valueOf(totalErrors)
                .divide(BigDecimal.valueOf(requestCount), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 성공률 계산 메서드
     *
     * @return 성공률 (%)
     */
    public BigDecimal calculateSuccessRate() {
        if (requestCount == null || requestCount == 0) {
            return BigDecimal.ZERO;
        }

        long totalSuccess = successCount != null ? successCount : 0L;

        return BigDecimal.valueOf(totalSuccess)
                .divide(BigDecimal.valueOf(requestCount), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * TPS가 피크인지 확인
     *
     * @param threshold 임계치
     * @return 초과 여부
     */
    public boolean isAboveThreshold(BigDecimal threshold) {
        if (tpsValue == null || threshold == null) {
            return false;
        }
        return tpsValue.compareTo(threshold) > 0;
    }

    /**
     * 응답 시간이 느린지 확인
     *
     * @param thresholdMs 임계치 (ms)
     * @return 느림 여부
     */
    public boolean isSlowResponse(BigDecimal thresholdMs) {
        if (avgResponseTimeMs == null || thresholdMs == null) {
            return false;
        }
        return avgResponseTimeMs.compareTo(thresholdMs) > 0;
    }
}