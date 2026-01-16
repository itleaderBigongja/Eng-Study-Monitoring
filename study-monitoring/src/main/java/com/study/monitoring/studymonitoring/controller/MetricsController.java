package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.model.dto.request.PrometheusQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.service.MetricsService;
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
 * ============================================================================
 * Prometheus 메트릭 API 컨트롤러
 * ============================================================================
 *
 * 역할:
 * - 실시간 메트릭 조회 API 제공 (현재 TPS, Heap, CPU, Error Rate)
 * - 프론트엔드의 MetricsPage와 연동
 *
 * ✅ [리팩토링 변경점]
 * 1. MetricsService 주입으로 비즈니스 로직 분리
 * 2. /current 엔드포인트 개선
 * 3. /range 엔드포인트 활성화 (Phase 1)
 *
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Validated
public class MetricsController {

    // ✅ [변경] MetricsService 주입 (비즈니스 로직)
    private final MetricsService metricsService;

    // ✅ [추가] PrometheusService 유지 (Range Query용)
    private final PrometheusService prometheusService;

    /**
     * 현재 메트릭 조회
     *
     * 📌 사용처: 프론트엔드 MetricsPage
     * 📌 호출 주기: 5초마다 (실시간 모니터링)
     *
     * @param application 애플리케이션 이름 (기본값: eng-study)
     * @return 현재 시점의 메트릭 데이터
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getCurrentMetrics(
            @RequestParam(defaultValue = "eng-study") String application)
    {
        try {
            log.info("📊 [Metrics API] 현재 메트릭 조회 요청 - application: {}", application);

            // ✅ Service 계층으로 위임 (비즈니스 로직 분리)
            Map<String, Object> result = metricsService.getCurrentMetrics(application);

            return ResponseEntity.ok(ApiResponseDTO.success(result));

        } catch (Exception e) {
            log.error("❌ [Metrics API] 메트릭 조회 실패 - application: {}", application, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("메트릭 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * ✅ [Phase 1 활성화] 시간 범위 메트릭 조회 (Range Query)
     *
     * 📌 사용처: 프론트엔드 통계 페이지, 실시간 페이지 히스토리
     * 📌 용도: 과거 특정 시간대의 메트릭 조회
     *
     * 요청 예시:
     * POST /api/metrics/range
     * {
     *   "query": "rate(http_server_requests_seconds_count{application=\"eng-study\"}[1m])",
     *   "start": 1700000000,
     *   "end": 1700003600,
     *   "step": "15s"
     * }
     *
     * @param request PrometheusQueryRequestDTO
     * @return 시간 범위별 메트릭 데이터
     */
    @PostMapping("/range")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> executeRangeQuery(
            @Valid @RequestBody PrometheusQueryRequestDTO request)
    {
        try {
            // 시작/종료 시간 기본값 설정 (최근 1시간)
            Long start = request.getStart() != null
                    ? request.getStart()
                    : Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond();

            Long end = request.getEnd() != null
                    ? request.getEnd()
                    : Instant.now().getEpochSecond();

            String step = request.getStep() != null ? request.getStep() : "15s";

            log.info("📈 [Metrics API] Range 쿼리 요청 - query: {}, start: {}, end: {}, step: {}",
                    request.getQuery(), start, end, step);

            // ✅ PrometheusService에서 Range Query 실행
            List<Map<String, Object>> data = prometheusService.queryRange(
                    request.getQuery(), start, end, step
            );

            Map<String, Object> response = Map.of(
                    "query", request.getQuery(),
                    "start", start,
                    "end", end,
                    "step", step,
                    "data", data
            );

            log.debug("✅ [Metrics API] Range 쿼리 완료 - 데이터 포인트 수: {}", data.size());

            return ResponseEntity.ok(ApiResponseDTO.success(response));

        } catch (Exception e) {
            log.error("❌ [Metrics API] Range 쿼리 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("Range 쿼리 실행 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * PromQL 쿼리 실행 (Instant Query)
     *
     * 용도: 고급 사용자가 직접 PromQL을 작성하여 실행
     * 현재 상태: 프론트엔드에서 호출하지 않음
     * 향후 계획: Phase 3에서 커스텀 쿼리 페이지 구현 시 활성화
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> executeQuery(
            @Valid @RequestBody PrometheusQueryRequestDTO request)
    {
        try {
            log.info("🔍 [Metrics API] PromQL 실행 요청 - query: {}", request.getQuery());

            Map<String, Object> result = prometheusService.queryInstance(request.getQuery());
            return ResponseEntity.ok(ApiResponseDTO.success(result));

        } catch (Exception e) {
            log.error("❌ [Metrics API] PromQL 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("쿼리 실행 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /** 커스텀 메트릭 에디터 미리보기( intelliSense ) 기능 */
    @GetMapping("/names") // URL: /api/metrics/names
    public ResponseEntity<ApiResponseDTO<List<String>>> getMetricNames() {
        List<String> metrics = prometheusService.getMetricNames();
        return ResponseEntity.ok(ApiResponseDTO.success(metrics));
    }
}