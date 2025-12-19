package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.converter.MetricsConverter;
import com.study.monitoring.studymonitoring.model.dto.request.PrometheusQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Prometheus 메트릭 API 컨트롤러
 *
 * 역할:
 * - Prometheus 메트릭 조회 API 제공
 * - 커스텀 PromQL 쿼리 실행
 *
 * 엔드포인트
 * - POST /api/metrics/query: PromQL 쿼리 실행
 * - GET /api/metrics/current: 현재 메트릭 조회
 * - GET /api/metrics/range: 시간 범위 메트릭 조회
 **/

@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Validated      // 입력 검증 활성화
public class MetricsController {

    // Prometheus 의존성 주입
    private final PrometheusService prometheusService;

    // Converter 주입
    private final MetricsConverter metricsConverter;

    /**
     * PromQL 쿼리 실행 (Instant Query)
     *
     * 요청:
     * {
     *   "query": "jvm_memory_used_bytes{area=\"heap\"}"
     * }
     *
     * @param request PrometheusQueryRequestDTO
     * @return ApiResponseDTO<Map>
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> executeQuery(
            @Valid
            @RequestBody PrometheusQueryRequestDTO request)
    {
        try {
            log.info("Execute PromOL query: {}", request.getQuery());

            // Prometheus 쿼리 실행
            Map<String, Object> result = prometheusService.queryInstance(request.getQuery());
            return ResponseEntity.ok(ApiResponseDTO.success(result));
        }catch (Exception e) {
            log.error("Failed to execute query", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("쿼리 실행 중 오류가 발생 했습니다: " + e.getMessage()));
        }
    }

    /**
     * 현재 메트릭 조회
     * @param application 애플리케이션 이름
     * @return ApiResponseDTO<Map>
     **/
    @GetMapping("/current")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getCurrentMetrics(
            @RequestParam(defaultValue = "eng-study") String application)
    {
        try {
            log.info("Fetching current metrics for: {}", application);

            Map<String, Object> metrics = Map.of(
                    "tps", prometheusService.getTps(application),
                    "heapUsage", prometheusService.getHeapMemoryUsage(application),
                    "errorRate", prometheusService.getErrorRate(application),
                    "cpuUsage", prometheusService.getCpuUsage(application),
                    "timestamp", Instant.now().toEpochMilli()
            );

            return ResponseEntity.ok(ApiResponseDTO.success(Map.of(
                    "application", application,
                    "metrics", metrics
            )));
        } catch (Exception e) {
            log.error("Failed to fetch current metrics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("메트릭 조회 중 오류가 발생했습니다." + e.getMessage()));
        }
    }

    /**
     * 시간 범위 메트릭 조회( Range Query )
     * 요청:
     * {
     *   "query": "rate(http_server_request_seconds_count[1m]",
     *   "start": 1700000000,  // 옵션
     *   "end": 1700003600,    // 옵션
     *   "step": "15s"
     * }
     *
     * @param request PrometheusQueryRequestDTO
     * @return ApiResponseDTO<Map>
     **/
    @PostMapping("/range")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> executeRangeQuery(
            @Valid @RequestBody PrometheusQueryRequestDTO request)
    {
        try {
            // 시작/종료 시간 기본값 설정( 최근 1시간 )
            Long start = request.getStart() != null
                    ? request.getStart()
                    : Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond();

            Long end = request.getEnd() != null
                    ? request.getEnd()
                    : Instant.now().getEpochSecond();

            log.info("Execute PromQL range query: query={}, start={}, end={}, step={}",
                    request.getQuery(), start, end, request.getStep());

            Map<String, Object> result = Map.of(
                    "query", request.getQuery(),
                    "start", start,
                    "end", end,
                    "step", request.getStep()
            );

            return ResponseEntity.ok(ApiResponseDTO.success(result));
        } catch (Exception e) {
            log.error("Failed to execute range query", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("Range 쿼리 실행 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
