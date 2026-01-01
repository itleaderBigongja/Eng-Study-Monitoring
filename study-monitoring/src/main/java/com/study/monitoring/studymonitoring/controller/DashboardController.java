// 경로 : /Monitering/study-monitoring/src/main/java/com/study/monitoring/studymonitoring/controller/DashboardController.java
package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.converter.MetricsConverter;
import com.study.monitoring.studymonitoring.converter.ProcessConverter;
import com.study.monitoring.studymonitoring.model.dto.request.MetricsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import com.study.monitoring.studymonitoring.service.MonitoringService;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 API 컨트롤러
 *
 * 주요 변경사항
 * - 실시간 메트릭은 Prometheus에서 직접 조회
 * - DB 저장 없이 조회만 수행
 * - 빠른 응답 속도 보장
 * */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Validated
public class DashboardController {

    private final PrometheusService prometheusService;
    private final ElasticsearchService elasticsearchService;
    private final MonitoringService monitoringService;
    private final ProcessConverter processConverter;
    private final MetricsConverter metricsConverter;

    /**
     * 대시보드 전체 현황 조회
     *
     * 데이터 소스:
     * - 프로세스 현황: PostgreSQL( MONITORING_PROCESS )
     * - 메트릭 요약: Prometheus( 실시간 조회 )
     * - 최근 에러: Elasticsearch( 로그 데이터 )
     * - 로그 카운트: Elasticsearch( 집계 )
     * - 시스템 통계: PostgreSQL( MONITORING_STATISTICS )
     *
     * @return ApiResponseDTO<DashboardResponseDTO>
     **/

    @GetMapping("/overview")
    public ResponseEntity<ApiResponseDTO<DashboardResponseDTO>> getDashboardOverview() {
        try {
            // 1. 프로세스 목록 조회
            List<DashboardResponseDTO.ProcessStatusDTO> processes = processConverter.toDTOList(
                    monitoringService.getAllProcesses());

            // 2. 실시간 생존 여부 조회
            Map<String, String> realTimeStatus = prometheusService.getRealTimeStatusMap();

            // 3. 프로세스 정보 업데이트
            for (DashboardResponseDTO.ProcessStatusDTO process : processes) {
                String appName = process.getProcessName();
                String currentStatus = realTimeStatus.getOrDefault(appName, "DOWN");
                process.setStatus(currentStatus);

                // ✅ [수정] Double(초)을 가져와서 -> String(시간 문자열)으로 변환하여 저장
                Double uptimeSeconds = prometheusService.getUptime(appName);
                process.setUptime(formatUptime(uptimeSeconds));

                if ("UP".equalsIgnoreCase(currentStatus)) {
                    process.setCpuUsage(prometheusService.getCpuUsage(appName));
                    process.setMemoryUsage(prometheusService.getHeapMemoryUsage(appName));
                } else {
                    process.setCpuUsage(0.0);
                    process.setMemoryUsage(0.0);
                }
            }

            // 4. 메트릭 요약
            DashboardResponseDTO.MetricsSummaryDTO metricsSummary = new DashboardResponseDTO.MetricsSummaryDTO(
                    new DashboardResponseDTO.ApplicationMetricsDTO(
                            prometheusService.getTps("eng-study"),
                            prometheusService.getHeapMemoryUsage("eng-study"),
                            prometheusService.getErrorRate("eng-study"),
                            null
                    ),
                    new DashboardResponseDTO.ApplicationMetricsDTO(
                            prometheusService.getTps("monitoring"),
                            prometheusService.getHeapMemoryUsage("monitoring"),
                            prometheusService.getErrorRate("monitoring"),
                            null
                    )
            );

            // 5. 최근 에러
            List<DashboardResponseDTO.ErrorLogDTO> recentErrors = elasticsearchService.getRecentErrors(10).stream()
                    .map(log -> new DashboardResponseDTO.ErrorLogDTO(
                            (String) log.get("_id"),
                            (String) log.get("@timestamp"),
                            (String) log.get("log_level"),
                            (String) log.get("message"),
                            (String) log.get("application")
                    )).toList();

            // 6. 로그 카운트 & 시스템 통계
            Map<String, Long> logCounts = elasticsearchService.countByLogLevel("application-logs-*");
            Map<String, Object> stats = monitoringService.getSystemStatistics();

            // ✅ [수정] 시스템 Uptime도 변환 필요
            Double sysUptimeSeconds = prometheusService.getUptime("monitoring");
            String systemRealTimeUptime = formatUptime(sysUptimeSeconds);

            DashboardResponseDTO.SystemStatisticsDTO statistics = new DashboardResponseDTO.SystemStatisticsDTO(
                    (Long) stats.get("totalRequest"),
                    (Double) stats.get("avgResponseTime"),
                    systemRealTimeUptime
            );

            return ResponseEntity.ok(ApiResponseDTO.success(new DashboardResponseDTO(
                    processes, metricsSummary, recentErrors, logCounts, statistics
            )));
        } catch (Exception e) {
            log.error("Failed to fetch dashboard overview", e);
            return ResponseEntity.internalServerError().body(ApiResponseDTO.fail("대시보드 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 실시간 메트릭 조회(차트용 데이터)
     * - Prometheus에서 직접 조회( DB 저장 없음 )
     * - 15초 간격 데이터 제공
     * - 빠른 응답 속도
     *
     * @param request MetricsQueryRequestDTO
     * @return ApiResponseDTO<MetricsResponseDTO>
     **/
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponseDTO<MetricsResponseDTO>> getMetrics(
            @Valid @ModelAttribute MetricsQueryRequestDTO request)
    {
        try {
            log.info("Fetching metrics: app={}, metric={}, hours={}",
                    request.getApplication(),
                    request.getMetric(),
                    request.getHours());

            // 1. 시간 범위 계산
            long end = Instant.now().getEpochSecond();
            long start = Instant.now().minus(request.getHours(), ChronoUnit.HOURS).getEpochSecond();

            // 2. PromQL 쿼리 생성
            String query = buildPrometheusQuery(request.getApplication(), request.getMetric());

            // 3. Prometheus에서 데이터 조회(DB 저장 없음)
            List<Map<String, Object>> data = prometheusService.queryRange(query, start, end, "15s");

            // 4. DTO 변환
            MetricsResponseDTO response = metricsConverter.toDTO(
                    data, request.getApplication(), request.getMetric(), start, end);

            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch metrics", e);
            return ResponseEntity.internalServerError().body(ApiResponseDTO.fail("메트릭 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 프로세스 현황 조회
     * @return ApiResponseDTO<Map>
     * */
    @GetMapping("/processes")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getPRocesses() {
        try {
            log.info("Fetching process status");
            List<DashboardResponseDTO.ProcessStatusDTO> processes = processConverter.toDTOList(
                    monitoringService.getAllProcesses()
            );

            Map<String, Long> summary = monitoringService.getProcessSummary();
            return ResponseEntity.ok(ApiResponseDTO.success(Map.of(
                    "processes", processes,
                    "summary", summary
            )));
        } catch (Exception e) {
            log.error("Failed to fetch process", e);
            return ResponseEntity.internalServerError().body(ApiResponseDTO.fail("프로세스 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * PromQL 쿼리 빌드 헬퍼
     */
    private String buildPrometheusQuery(String application, String metric) {
        return switch (metric.toLowerCase()) {
            case "tps" -> String.format(
                    "rate(http_server_requests_seconds_count{application=\"%s\"}[1m])",
                    application
            );
            case "heap" -> String.format(
                    "jvm_memory_used_bytes{application=\"%s\",area=\"heap\"} / " +
                            "jvm_memory_max_bytes{application=\"%s\",area=\"heap\"} * 100",
                    application, application
            );
            case "error_rate" -> String.format(
                    "rate(http_server_requests_seconds_count{application=\"%s\",status=~\"5..\"}[5m]) / " +
                            "rate(http_server_requests_seconds_count{application=\"%s\"}[5m]) * 100",
                    application, application
            );
            default -> throw new IllegalArgumentException("Unknown metric: " + metric);
        };
    }

    private String formatUptime(Double seconds) {
        if (seconds == null || seconds <= 0) return "Down";
        long s = seconds.longValue();
        long days = s / (24 * 3600);
        long hours = (s % (24 * 3600)) / 3600;
        long minutes = (s % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        sb.append(minutes).append("m");
        return sb.toString().trim();
    }

    /**
     * 에러 로그 목록 조회 (페이징)
     * GET /api/dashboard/errors?page=1&size=5
     */
    @GetMapping("/errors")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<DashboardResponseDTO.ErrorLogDTO>>> getErrorLogs(
            @RequestParam(defaultValue = "APP") String type, // [추가] 탭 구분용
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        try {
            PageResponseDTO<Map<String, Object>> result = elasticsearchService.searchErrorLogs(type, page, size);

            // [중요] 인덱스별로 필드 구조가 다르므로 매핑 로직 분기
            List<DashboardResponseDTO.ErrorLogDTO> dtos = result.getContent().stream()
                    .map(log -> {
                        if ("SYSTEM".equalsIgnoreCase(type)) {
                            // [수정] System/DB Error 매핑 로직 강화

                            // 1. 에러 상세 객체 가져오기
                            Map<String, Object> errorObj = (Map<String, Object>) log.get("error");

                            // 2. 메시지 추출
                            String errorMsg = errorObj != null ? (String) errorObj.get("message") : (String) log.get("message");

                            // 3. 심각도 추출 (없으면 CRITICAL로 가정)
                            String severity = errorObj != null && errorObj.get("severity") != null
                                    ? (String) errorObj.get("severity")
                                    : "CRITICAL";

                            // 4. [핵심] Application 식별 (우선순위에 따라 필드 탐색)
                            String applicationName = "System"; // 기본값

                            // 시도 1: 별도 fields 객체 안에 application이 있는 경우 (Logstash/Filebeat 설정)
                            Map<String, Object> fields = (Map<String, Object>) log.get("fields");
                            if (fields != null && fields.get("application") != null) {
                                applicationName = (String) fields.get("application");
                            }
                            // 시도 2: 최상위에 application 필드가 있는 경우
                            else if (log.get("application") != null) {
                                applicationName = (String) log.get("application");
                            }
                            // 시도 3: agent.name (수집기 이름) 사용
                            else if (log.get("agent") != null) {
                                Map<String, Object> agent = (Map<String, Object>) log.get("agent");
                                applicationName = (String) agent.get("name");
                            }
                            // 시도 4: error.type 사용 (예: java.lang.OutOfMemoryError)
                            else if (errorObj != null && errorObj.get("type") != null) {
                                applicationName = (String) errorObj.get("type");
                            }

                            return new DashboardResponseDTO.ErrorLogDTO(
                                    (String) log.get("_id"),
                                    (String) log.get("@timestamp"),
                                    severity,
                                    errorMsg,
                                    applicationName // [변경] 찾아낸 애플리케이션 이름 주입
                            );
                        } else {
                            // APP 로그는 기존 그대로
                            return new DashboardResponseDTO.ErrorLogDTO(
                                    (String) log.get("_id"),
                                    (String) log.get("@timestamp"),
                                    (String) log.get("log_level"),
                                    (String) log.get("message"),
                                    (String) log.get("application")
                            );
                        }
                    }).toList();

            // Response 생성 로직 (기존 동일)
            PageResponseDTO<DashboardResponseDTO.ErrorLogDTO> response = PageResponseDTO.<DashboardResponseDTO.ErrorLogDTO>builder()
                    .content(dtos)
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .currentPage(result.getCurrentPage())
                    .size(result.getSize())
                    .build();

            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            // ... 에러 처리
            return ResponseEntity.internalServerError().body(ApiResponseDTO.fail(e.getMessage()));
        }
    }
}
