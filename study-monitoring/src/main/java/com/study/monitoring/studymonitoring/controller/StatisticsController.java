package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.model.dto.request.*;
import com.study.monitoring.studymonitoring.model.dto.response.*;
import com.study.monitoring.studymonitoring.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계 조회 API 컨트롤러
 **/
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Validated
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 시계열 데이터 통계 조회
     * <p>
     * GET: http://localhost:8081/api/statistics/timeseries?metricType=TPS&startTime=2025-12-01T00:00:00&endTime=2025-12-18T23:59:59&timePeriod=DAY&aggregationType=AVG
     *
     * @param request paramter1: metricType      = 메트릭유형
     *                paramter2: startTime       = 시작일자
     *                paramter3: endTime         = 종료일자
     *                paramter4: timePeriod      = 시간주기
     *                paramter5: aggregationType = 집계유형
     */
    @GetMapping("/timeseries")
    public ResponseEntity<ApiResponseDTO<StatisticsResponseDTO>> getTimeSeriesStatistics(
            @Valid @ModelAttribute StatisticsQueryRequestDTO request) {
        try {
            log.info("Fetching time series statistics: {}", request);
            StatisticsResponseDTO response = statisticsService.getTimeSeriesStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch time series statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("통계 조회 중 오류가 발생했습니다." + e.getMessage()));
        }
    }

    /**
     * 로그 통계 조회
     * <p>
     * GET: http://localhost:8081/api/statistics/logs?startTime=2025-12-01T00:00:00&endTime=2025-12-18T23:59:59&timePeriod=DAY&logLevel=ERROR
     *
     * @param request paramter1: 시작일자
     *                paramter2: 종료일자
     *                paramter3: 시간주기
     *                paramter4: 로그유형
     **/
    @GetMapping("/logs")
    public ResponseEntity<ApiResponseDTO<LogStatisticsResponseDTO>> getLogStatistics(
            @Valid @ModelAttribute LogStatisticsQueryRequestDTO request) {
        try {
            log.info("Fetching log statistics: {}", request);
            LogStatisticsResponseDTO response = statisticsService.getLogStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch log statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 접근 로그 통계 조회
     * GET: /api/statistics/access-logs?startTime=2025-12-01 00:00:00&endTime=2025-12-18 23:59:59&timePeriod=HOUR
     */
    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponseDTO<AccessLogStatisticsResponseDTO>> getAccessLogStatistics(
            @Valid @ModelAttribute AccessLogStatisticsQueryRequestDTO request) {
        try {
            log.info("Fetching access log statistics: {}", request);
            AccessLogStatisticsResponseDTO response = statisticsService.getAccessLogStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch access log statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("접근 로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 에러 로그 통계 조회
     * GET: /api/statistics/error-logs?startTime=2025-12-01 00:00:00&endTime=2025-12-18 23:59:59&timePeriod=DAY&errorType=NullPointerException
     */
    @GetMapping("/error-logs")
    public ResponseEntity<ApiResponseDTO<ErrorLogStatisticsResponseDTO>> getErrorLogStatistics(
            @Valid @ModelAttribute ErrorLogStatisticsQueryRequestDTO request) {
        try {
            log.info("Fetching error log statistics: {}", request);
            ErrorLogStatisticsResponseDTO response = statisticsService.getErrorLogStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch error log statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("에러 로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 성능 메트릭 통계 조회
     * GET: /api/statistics/performance-metrics?startTime=2025-12-01 00:00:00&endTime=2025-12-18 23:59:59&timePeriod=HOUR&metricName=cpu_usage
     */
    @GetMapping("/performance-metrics")
    public ResponseEntity<ApiResponseDTO<PerformanceMetricsStatisticsResponseDTO>> getPerformanceMetricsStatistics(
            @Valid @ModelAttribute PerformanceMetricsStatisticsQueryRequestDTO request) {
        try {
            log.info("Fetching performance metrics statistics: {}", request);
            PerformanceMetricsStatisticsResponseDTO response = statisticsService.getPerformanceMetricsStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch performance metrics statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("성능 메트릭 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 데이터베이스 로그 통계 조회
     * GET: /api/statistics/database-logs?startTime=2025-12-01 00:00:00&endTime=2025-12-18 23:59:59&timePeriod=HOUR&operation=SELECT
     */
    @GetMapping("/database-logs")
    public ResponseEntity<ApiResponseDTO<DatabaseLogStatisticsResponseDTO>> getDatabaseLogStatistics(
            @Valid @ModelAttribute DatabaseLogStatisticsQueryRequestDTO request) {
        try {
            log.info("Fetching database log statistics: {}", request);
            DatabaseLogStatisticsResponseDTO response = statisticsService.getDatabaseLogStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch database log statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("데이터베이스 로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /** 감사 로그 통계 조회
     *  GET: /api/statistics/audit-logs?startTime=2025-12-01 00:00:00&endTime=2025-12-18 23:59:59&timePeriod=DAY&eventAction=user.login */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponseDTO<AuditLogStatisticsResponseDTO>> getAuditLogStatistics(
            @Valid @ModelAttribute AuditLogStatisticsQueryRequestDTO request)
    {
        try {
            log.info("Fetching audit log statistics: {}", request);
            AuditLogStatisticsResponseDTO response = statisticsService.getAuditLogStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch audit log statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("감사 로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /** 보안 로그 통계 조회
     *  GET: /api/statistics/security-logs?startTime=2025-12-01 00:00:00&endTime=2025-12-18 23:59:59&timePeriod=HOUR&threatLevel=high */
    @GetMapping("/security-logs")
    public ResponseEntity<ApiResponseDTO<SecurityLogStatisticsResponseDTO>> getSecurityLogStatistics(
            @Valid @ModelAttribute SecurityLogStatisticsQueryRequestDTO request)
    {
        try {
            log.info("Fetching security log statistics: {}", request);
            SecurityLogStatisticsResponseDTO response = statisticsService.getSecurityLogStatistics(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch security log statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("보안 로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
