package com.study.monitoring.studymonitoring.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 통계 정보 VO
 *
 * 테이블: MONITORING_STATISTICS
 *
 * 설명:
 * - 시간 주기별 (분/시간/일/주/월) 통계 데이터
 * - 집계 방식 (AVG/SUM/MIN/MAX/COUNT)
 * - 조회 시작/종료 날짜 범위 검색 지원
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsVO {

    private Long statisticsId;             // 통계 ID (PK)
    private Long processId;                // 프로세스 ID (FK)
    private String metricType;             // 메트릭 유형 (TPS, ERROR_COUNT, HEAP_MEMORY 등)
    private String timePeriod;             // 시간 주기 (MINUTE, HOUR, DAY, WEEK, MONTH)
    private String aggregationType;        // 집계 방식 (AVG, SUM, MIN, MAX, COUNT)
    private LocalDateTime startTime;       // 시작 시간
    private LocalDateTime endTime;         // 종료 시간
    private BigDecimal metricValue;        // 메트릭 값
    private Integer sampleCount;           // 샘플 수
    private BigDecimal minValue;           // 최소값
    private BigDecimal maxValue;           // 최대값
    private LocalDateTime createdAt;       // 생성일시

    /**
     * 시간 주기 Enum
     */
    public enum TimePeriod {
        MINUTE,     // 분
        HOUR,       // 시간
        DAY,        // 일
        WEEK,       // 주
        MONTH       // 월
    }

    /**
     * 집계 방식 Enum
     */
    public enum AggregationType {
        AVG,        // 평균
        SUM,        // 합계
        MIN,        // 최소
        MAX,        // 최대
        COUNT       // 카운트
    }

    /**
     * 통계 기간 (분)
     *
     * @return 기간 (분)
     */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * 평균 샘플값 계산
     *
     * @return 평균값
     */
    public BigDecimal calculateAverageSample() {
        if (sampleCount == null || sampleCount == 0) {
            return BigDecimal.ZERO;
        }

        return metricValue.divide(
                BigDecimal.valueOf(sampleCount),
                4,
                BigDecimal.ROUND_HALF_UP
        );
    }

    /**
     * 변동성 계산 (최대값 - 최소값)
     *
     * @return 변동성
     */
    public BigDecimal calculateVariance() {
        if (minValue == null || maxValue == null) {
            return BigDecimal.ZERO;
        }

        return maxValue.subtract(minValue);
    }
}