package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 개요 응답 DTO
 *
 * 사용처:
 * - GET /api/dashboard/overview
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {

    private List<ProcessStatusDTO> processes;      // 프로세스 현황
    private MetricsSummaryDTO metrics;             // 메트릭 요약
    private List<ErrorLogDTO> recentErrors;        // 최근 에러
    private Map<String, Long> logCounts;           // 로그 레벨별 카운트
    private SystemStatisticsDTO statistics;        // 시스템 통계

    /**
     * 프로세스 상태 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessStatusDTO {
        private Long processId;
        private String processName;
        private String processType;
        private String status;
        private Double cpuUsage;
        private Double memoryUsage;
        private String uptime;
        private String lastHealthCheck;
    }

    /**
     * 메트릭 요약 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummaryDTO {
        private ApplicationMetricsDTO engStudy;
        private ApplicationMetricsDTO monitoring;
    }

    /**
     * 애플리케이션별 메트릭 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationMetricsDTO {
        private Double tps;
        private Double heapUsage;
        private Double errorRate;
        private Double responseTime;
    }

    /**
     * 에러 로그 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorLogDTO {
        private String id;
        private String timestamp;
        private String logLevel;
        private String message;
        private String application;
    }

    /**
     * 시스템 통계 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStatisticsDTO {
        private Long totalRequests;
        private Double avgResponseTime;
        private String uptime;
    }
}
