// study-monitoring/src/main/java/com/study/monitoring/studymonitoring/service/impl/DashboardServiceImpl.java
package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.mapper.ProcessMapper;
import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;
import com.study.monitoring.studymonitoring.model.vo.ProcessVO;
import com.study.monitoring.studymonitoring.service.DashboardService;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProcessMapper processMapper;
    private final PrometheusService prometheusService;
    private final ElasticsearchService elasticsearchService;

    @Override
    public DashboardResponseDTO getDashboardOverview() {
        log.info("Fetching dashboard overview with real-time metrics");

        // 1. DB에서 프로세스 메타데이터만 조회
        List<ProcessVO> processMetadata = processMapper.getAllProcesses();

        // 2. Prometheus에서 실시간 메트릭 조회 및 병합
        List<DashboardResponseDTO.ProcessStatusDTO> processes = processMetadata.stream()
                .map(this::enrichProcessWithMetrics)
                .collect(Collectors.toList());

        // 3. 애플리케이션별 메트릭 요약 (Prometheus에서 실시간 조회)
        DashboardResponseDTO.MetricsSummaryDTO metricsSummary = buildMetricsSummary();

        // 4. 최근 에러 (Elasticsearch에서 조회)
        List<DashboardResponseDTO.ErrorLogDTO> recentErrors = getRecentErrors();

        // 5. 로그 레벨별 카운트 (Elasticsearch에서 조회)
        Map<String, Long> logCounts = elasticsearchService.countByLogLevel("application-logs-*");

        // 6. 시스템 통계 (Prometheus 기반 계산)
        DashboardResponseDTO.SystemStatisticsDTO statistics = calculateSystemStatistics(processes);

        return new DashboardResponseDTO(
                processes,
                metricsSummary,
                recentErrors,
                logCounts,
                statistics
        );
    }

    /**
     * 프로세스 메타데이터에 Prometheus 실시간 메트릭 병합
     */
    private DashboardResponseDTO.ProcessStatusDTO enrichProcessWithMetrics(ProcessVO metadata) {
        try {
            String appName = metadata.getProcessName();
            Double cpuUsage = prometheusService.getCpuUsage(appName);
            Double memoryUsage = prometheusService.getHeapMemoryUsage(appName);

            // ✅ [수정] Double로 받아서 String으로 포맷팅
            Double uptimeSeconds = prometheusService.getUptime(appName);
            String uptime = formatUptime(uptimeSeconds);

            String status = determineStatus(cpuUsage, memoryUsage, metadata.getStatus());

            return new DashboardResponseDTO.ProcessStatusDTO(
                    metadata.getProcessId(),
                    metadata.getProcessName(),
                    metadata.getProcessType(),
                    status,
                    cpuUsage != null ? cpuUsage : 0.0,
                    memoryUsage != null ? memoryUsage : 0.0,
                    uptime,
                    LocalDateTime.now().toString()
            );

        } catch (Exception e) {
            log.error("Failed to enrich process metrics for {}: {}", metadata.getProcessName(), e.getMessage());
            return new DashboardResponseDTO.ProcessStatusDTO(
                    metadata.getProcessId(), metadata.getProcessName(), metadata.getProcessType(),
                    "ERROR", 0.0, 0.0, "Unknown", LocalDateTime.now().toString()
            );
        }
    }

    /**
     * 애플리케이션별 메트릭 요약 (Prometheus에서 실시간 조회)
     */
    private DashboardResponseDTO.MetricsSummaryDTO buildMetricsSummary() {
        try {
            return new DashboardResponseDTO.MetricsSummaryDTO(
                    buildApplicationMetrics("eng-study"),
                    buildApplicationMetrics("monitoring")
            );
        } catch (Exception e) {
            log.error("Failed to build metrics summary", e);
            return new DashboardResponseDTO.MetricsSummaryDTO(
                    new DashboardResponseDTO.ApplicationMetricsDTO(0.0, 0.0, 0.0, null),
                    new DashboardResponseDTO.ApplicationMetricsDTO(0.0, 0.0, 0.0, null)
            );
        }
    }

    private DashboardResponseDTO.ApplicationMetricsDTO buildApplicationMetrics(String appName) {
        try {
            Double tps = prometheusService.getTps(appName);
            Double heapUsage = prometheusService.getHeapMemoryUsage(appName);
            Double errorRate = prometheusService.getErrorRate(appName);

            return new DashboardResponseDTO.ApplicationMetricsDTO(
                    tps,
                    heapUsage,
                    errorRate,
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to get metrics for {}: {}", appName, e.getMessage());
            return new DashboardResponseDTO.ApplicationMetricsDTO(0.0, 0.0, 0.0, null);
        }
    }

    /**
     * 최근 에러 조회 (Elasticsearch)
     */
    private List<DashboardResponseDTO.ErrorLogDTO> getRecentErrors() {
        try {
            List<Map<String, Object>> errors = elasticsearchService.getRecentErrors(10);
            return errors.stream()
                    .map(log -> new DashboardResponseDTO.ErrorLogDTO(
                            (String) log.get("_id"),
                            (String) log.get("@timestamp"),
                            (String) log.get("log_level"),
                            (String) log.get("message"),
                            (String) log.get("application")
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get recent errors", e);
            return new ArrayList<>();
        }
    }

    /**
     * 시스템 통계 계산 (실시간)
     */
    private DashboardResponseDTO.SystemStatisticsDTO calculateSystemStatistics(
            List<DashboardResponseDTO.ProcessStatusDTO> processes) {

        try {
            // Prometheus에서 전체 요청 수 조회 (간단한 예시)
            // 실제로는 더 정교한 쿼리 필요
            Long totalRequests = 0L;
            Double avgResponseTime = 0.0;

            // 가장 오래된 프로세스의 uptime 반환
            String maxUptime = processes.stream()
                    .map(DashboardResponseDTO.ProcessStatusDTO::getUptime)
                    .max(String::compareTo)
                    .orElse("0s");

            return new DashboardResponseDTO.SystemStatisticsDTO(
                    totalRequests,
                    avgResponseTime,
                    maxUptime
            );
        } catch (Exception e) {
            log.error("Failed to calculate system statistics", e);
            return new DashboardResponseDTO.SystemStatisticsDTO(0L, 0.0, "N/A");
        }
    }

    /**
     * Uptime 계산
     */
    private String calculateUptime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Unknown";
        }

        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%dd %dh", days, hours);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * 상태 판단 (메트릭 기반)
     */
    private String determineStatus(Double cpuUsage, Double memoryUsage, String baseStatus) {
        // Prometheus 메트릭을 기반으로 상태 판단
        if (cpuUsage != null && cpuUsage > 90) {
            return "WARNING";
        }
        if (memoryUsage != null && memoryUsage > 90) {
            return "WARNING";
        }

        // DB의 기본 상태 사용 (RUNNING, STOPPED 등)
        return baseStatus != null ? baseStatus : "UNKNOWN";
    }

    private String formatUptime(double seconds) {
        if (seconds <= 0) return "Down";

        long s = (long) seconds;
        long days = s / (24 * 3600);
        long hours = (s % (24 * 3600)) / 3600;
        long minutes = (s % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        sb.append(minutes).append("m");

        return sb.toString().trim();
    }
}