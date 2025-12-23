package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 성능 메트릭 통계 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsStatisticsResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;

    private String timePeriod;

    // 시스템 메트릭 평균
    private SystemMetrics systemMetrics;

    // JVM 메트릭 평균
    private JvmMetrics jvmMetrics;

    // 시간대별 분포
    private List<MetricDistribution> distributions;



    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMetrics {
        private Double avgCpuUsage;             // 평균 CPU 사용량
        private Double avgMemoryUsage;          // 평균 메모리 사용량
        private Double avgDiskUsage;            // 평균 디스크 사용량
        private Double maxCpuUsage;             // 최대 CPU 사용량
        private Double maxMemoryUsage;          // 최대 메모리 사용량
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmMetrics {
        private Double avgHeapUsed;             // 평균 힙 사용량
        private Double maxHeapUsed;             // 최대 힙 사용량
        private Long totalGcCount;              // 총 가비지 수
        private Long totalGcTime;               // 총 가비지 시간
        private Double avgThreadCount;          // 평균 스레드 수
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricDistribution {
        private String timestamp;
        private Double cpuUsage;                // CPU 사용량
        private Double memoryUsage;             // 메모리 사용량
        private Double heapUsage;               // 메트릭 힙 사용량
    }
}
