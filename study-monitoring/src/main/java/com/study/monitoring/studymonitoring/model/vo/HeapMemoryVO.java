package com.study.monitoring.studymonitoring.model.vo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Heap 메모리 사용 현황 VO
 *
 * 테이블: MONITORING_HEAP_MEMORY
 *
 * 설명:
 * - JVM Heap 메모리 사용량 모니터링
 * - GC 발생 횟수 및 소요 시간 추적
 * - 클러스터/노드별 메모리 현황
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeapMemoryVO {

    private Long heapMemoryId;             // Heap 메모리 ID (PK)
    private Long processId;                // 프로세스 ID (FK)
    private String clusterName;            // 클러스터명
    private String nodeName;               // 노드명

    private BigDecimal heapUsedMb;         // 사용 중인 Heap 메모리(MB)
    private BigDecimal heapMaxMb;          // 최대 Heap 메모리(MB)
    private BigDecimal heapUsagePercent;   // Heap 사용률(%)
    private BigDecimal nonHeapUsedMb;      // Non-Heap 사용 메모리(MB)
    private BigDecimal nonHeapMaxMb;       // Non-Heap 최대 메모리(MB)
    private Integer gcCount;               // GC 발생 횟수
    private Long gcTimeMs;                 // GC 소요 시간(ms)
    private Boolean isWarning;             // 경고 상태(80% 이상)
    private Boolean isCritical;            // 심각 상태(90% 이상)
    private LocalDateTime collectedAt;     // 수집 시간
    private LocalDateTime createdAt;       // 생성일시

    /**
     * 경고 상태 자동 계산
     *
     * @return 경고 여부
     */
    public boolean calculateWarning() {
        if (heapUsagePercent == null) {
            return false;
        }
        return heapUsagePercent.compareTo(BigDecimal.valueOf(80)) >= 0;
    }

    /**
     * 심각 상태 자동 계산
     *
     * @return 심각 여부
     */
    public boolean calculateCritical() {
        if (heapUsagePercent == null) {
            return false;
        }
        return heapUsagePercent.compareTo(BigDecimal.valueOf(90)) >= 0;
    }

    /**
     * 사용률 계산 메서드
     *
     * @return 사용률 (%)
     */
    public BigDecimal calculateUsagePercentage() {
        if (heapMaxMb == null || heapMaxMb.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return heapUsedMb
                .divide(heapMaxMb, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}