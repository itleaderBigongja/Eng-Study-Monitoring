package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.converter.MetricsConverter;
import com.study.monitoring.studymonitoring.converter.ProcessConverter;
import com.study.monitoring.studymonitoring.mapper.ProcessMapper;
import com.study.monitoring.studymonitoring.model.dto.request.MetricsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;
import com.study.monitoring.studymonitoring.model.vo.ProcessVO;
import com.study.monitoring.studymonitoring.service.DashboardService;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import com.study.monitoring.studymonitoring.service.MonitoringService;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final MonitoringService monitoringService; // getProcessStatus용

    private final ProcessConverter processConverter;
    private final MetricsConverter metricsConverter;

    /**
     * 1. 대시보드 전체 개요
     */
    @Override
    public DashboardResponseDTO getDashboardOverview() {
        log.info("Fetching dashboard overview");

        // 1. 프로세스 목록 (DB + Prometheus)
        List<ProcessVO> processMetadata = processMapper.getAllProcesses();
        List<DashboardResponseDTO.ProcessStatusDTO> processes = processMetadata.stream()
                .map(this::enrichProcessWithMetrics)
                .collect(Collectors.toList());

        // 2. 메트릭 요약
        DashboardResponseDTO.MetricsSummaryDTO metricsSummary = buildMetricsSummary();

        // 3. 최근 에러 (Elasticsearch) - ✅ 공통 매핑 로직 사용
        List<DashboardResponseDTO.ErrorLogDTO> recentErrors = elasticsearchService.getRecentErrors(10).stream()
                .map(logMap -> mapToErrorLogDTO(logMap, "APP")) // 최근 에러는 기본적으로 APP으로 간주하거나 필요시 로직 추가
                .collect(Collectors.toList());

        // 4. 로그 카운트
        Map<String, Long> logCounts = elasticsearchService.countByLogLevel("application-logs-*");

        // 5. 시스템 통계
        DashboardResponseDTO.SystemStatisticsDTO statistics = calculateSystemStatistics(processes);

        return new DashboardResponseDTO(processes, metricsSummary, recentErrors, logCounts, statistics);
    }

    /**
     * 2. 실시간 메트릭 조회 (Controller에서 이동됨)
     */
    @Override
    public MetricsResponseDTO getMetrics(MetricsQueryRequestDTO request) {
        long end = Instant.now().getEpochSecond();
        long start = Instant.now().minus(request.getHours(), ChronoUnit.HOURS).getEpochSecond();

        // Query 생성 로직을 Service 내부 private 메서드로 처리
        String query = buildPrometheusQuery(request.getApplication(), request.getMetric());

        List<Map<String, Object>> data = prometheusService.queryRange(query, start, end, "15s");
        return metricsConverter.toDTO(data, request.getApplication(), request.getMetric(), start, end);
    }

    /**
     * 3. 프로세스 현황 조회 (Controller에서 이동됨)
     */
    @Override
    public Map<String, Object> getProcessStatus() {
        List<DashboardResponseDTO.ProcessStatusDTO> processes = processConverter.toDTOList(
                monitoringService.getAllProcesses()
        );
        Map<String, Long> summary = monitoringService.getProcessSummary();
        return Map.of("processes", processes, "summary", summary);
    }

    /**
     * 4. 에러 로그 조회 (Controller에서 이동됨)
     */
    @Override
    public PageResponseDTO<DashboardResponseDTO.ErrorLogDTO> getErrorLogs(String type, int page, int size) {
        // 1. ES 서비스 호출 (여기서 이미 CRITICAL 판정이 완료된 데이터가 옴)
        PageResponseDTO<Map<String, Object>> result = elasticsearchService.searchErrorLogs(type, page, size);

        // 2. DTO 변환 (단순 매핑)
        List<DashboardResponseDTO.ErrorLogDTO> dtos = result.getContent().stream()
                .map(logMap -> mapToErrorLogDTO(logMap, type))
                .collect(Collectors.toList());

        return PageResponseDTO.<DashboardResponseDTO.ErrorLogDTO>builder()
                .content(dtos)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getCurrentPage())
                .size(result.getSize())
                .build();
    }

    // =================================================================================
    // ✅ Private Helper Methods (비즈니스 로직 집중화)
    // =================================================================================

    /**
     * Elasticsearch Map 데이터를 ErrorLogDTO로 변환하는 공통 로직
     * (기존 Controller에 있던 복잡한 SYSTEM/APP 분기 처리를 여기서 담당)
     */
    private DashboardResponseDTO.ErrorLogDTO mapToErrorLogDTO(Map<String, Object> logMap, String type) {
        String id = (String) logMap.get("_id");
        String timestamp = (String) logMap.get("@timestamp");
        String message = (String) logMap.get("message");

        // 1. 레벨은 ElasticsearchService에서 이미 계산해준 값을 우선 사용
        String level = (String) logMap.getOrDefault("level", "INFO");

        // 2. [복구됨] Application 이름 찾기 로직 (이게 없어서 목록이 비어 보였던 것)
        String applicationName = (String) logMap.get("application");

        // SYSTEM 로그거나, application 필드가 최상위에 없는 경우 깊숙이 찾기
        if (applicationName == null || "SYSTEM".equalsIgnoreCase(type)) {
            // fields.application 확인
            if (logMap.get("fields") instanceof Map) {
                Map<String, Object> fields = (Map<String, Object>) logMap.get("fields");
                if (fields.get("application") != null) {
                    applicationName = (String) fields.get("application");
                }
            }
            // agent.name 확인
            else if (logMap.get("agent") instanceof Map) {
                Map<String, Object> agent = (Map<String, Object>) logMap.get("agent");
                if (agent.get("name") != null) {
                    applicationName = (String) agent.get("name");
                }
            }
        }

        return new DashboardResponseDTO.ErrorLogDTO(id, timestamp, level, message, applicationName);
    }

    private DashboardResponseDTO.ProcessStatusDTO enrichProcessWithMetrics(ProcessVO metadata) {
        try {
            String appName = metadata.getProcessName();
            Double cpuUsage = prometheusService.getCpuUsage(appName);
            Double memoryUsage = prometheusService.getHeapMemoryUsage(appName);
            Double uptimeSeconds = prometheusService.getUptime(appName);
            String uptime = formatUptime(uptimeSeconds != null ? uptimeSeconds : 0.0);

            String status = determineStatus(cpuUsage, memoryUsage, metadata.getStatus());

            return new DashboardResponseDTO.ProcessStatusDTO(
                    metadata.getProcessId(), metadata.getProcessName(), metadata.getProcessType(),
                    status,
                    cpuUsage != null ? cpuUsage : 0.0,
                    memoryUsage != null ? memoryUsage : 0.0,
                    uptime,
                    LocalDateTime.now().toString()
            );
        } catch (Exception e) {
            log.warn("Metrics enrichment failed for {}", metadata.getProcessName());
            return new DashboardResponseDTO.ProcessStatusDTO(
                    metadata.getProcessId(), metadata.getProcessName(), metadata.getProcessType(),
                    "UNKNOWN", 0.0, 0.0, "Down", LocalDateTime.now().toString()
            );
        }
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

    private String determineStatus(Double cpuUsage, Double memoryUsage, String baseStatus) {
        if (cpuUsage != null && cpuUsage > 90) return "WARNING";
        if (memoryUsage != null && memoryUsage > 90) return "WARNING";
        return baseStatus != null ? baseStatus : "UNKNOWN";
    }

    private DashboardResponseDTO.MetricsSummaryDTO buildMetricsSummary() {
        return new DashboardResponseDTO.MetricsSummaryDTO(
                buildApplicationMetrics("eng-study"),
                buildApplicationMetrics("monitoring")
        );
    }

    private DashboardResponseDTO.ApplicationMetricsDTO buildApplicationMetrics(String appName) {
        try {
            return new DashboardResponseDTO.ApplicationMetricsDTO(
                    prometheusService.getTps(appName),
                    prometheusService.getHeapMemoryUsage(appName),
                    prometheusService.getErrorRate(appName),
                    null
            );
        } catch (Exception e) {
            return new DashboardResponseDTO.ApplicationMetricsDTO(0.0, 0.0, 0.0, null);
        }
    }

    private DashboardResponseDTO.SystemStatisticsDTO calculateSystemStatistics(List<DashboardResponseDTO.ProcessStatusDTO> processes) {
        try {
            Double sysUptime = prometheusService.getUptime("monitoring");
            return new DashboardResponseDTO.SystemStatisticsDTO(
                    0L, 0.0, formatUptime(sysUptime != null ? sysUptime : 0.0)
            );
        } catch (Exception e) {
            return new DashboardResponseDTO.SystemStatisticsDTO(0L, 0.0, "N/A");
        }
    }

    private String buildPrometheusQuery(String application, String metric) {
        return switch (metric.toLowerCase()) {
            case "tps" -> String.format("rate(http_server_requests_seconds_count{application=\"%s\"}[1m])", application);
            case "heap" -> String.format("jvm_memory_used_bytes{application=\"%s\",area=\"heap\"} / jvm_memory_max_bytes{application=\"%s\",area=\"heap\"} * 100", application, application);
            case "error_rate" -> String.format("rate(http_server_requests_seconds_count{application=\"%s\",status=~\"5..\"}[5m]) / rate(http_server_requests_seconds_count{application=\"%s\"}[5m]) * 100", application, application);
            default -> throw new IllegalArgumentException("Unknown metric: " + metric);
        };
    }
}