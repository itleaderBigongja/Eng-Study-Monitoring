package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.converter.LogsConverter;
import com.study.monitoring.studymonitoring.model.dto.request.LogSearchRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.LogSearchResponseDTO;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 로그 검색 API 컨트롤러
 *
 * 역할:
 * - Elasticsearch 로그 검색 기능 제공
 * - 로그 레벨, 키워드, 시간 범위 필터링
 *
 * 엔드포인트:
 * - GET /api/logs/search: 로그 검색
 * - GET /api/logs/errors: 에러 로그 조회
 * - GET /api/logs/stats: 로그 통계
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Validated
public class LogsController {

    private final ElasticsearchService elasticsearchService;
    private final LogsConverter logsConverter;

    /**
     * 로그 검색 (날짜 필터 추가)
     *
     * 요청:
     * - index: 인덱스 패턴 (application-logs-*, access-logs-*, error-logs-*)
     * - keyword: 검색 키워드 (옵션)
     * - logLevel: 로그 레벨 (옵션: INFO, WARN, ERROR)
     * - startDate: 시작 날짜 (옵션: yyyy-MM-ddTHH:mm:ss)
     * - endDate: 종료 날짜 (옵션: yyyy-MM-ddTHH:mm:ss)
     * - from: 페이지 시작 (기본: 0)
     * - size: 페이지 크기 (기본: 50)
     *
     * @param request LogSearchRequestDTO
     * @return ApiResponseDTO<LogSearchResponseDTO>
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<LogSearchResponseDTO>> searchLogs(
            @Valid @ModelAttribute LogSearchRequestDTO request) {

        try {
            log.info("Searching logs: index={}, keyword={}, logLevel={}, startDate={}, endDate={}, from={}, size={}",
                    request.getIndex(), request.getKeyword(), request.getLogLevel(),
                    request.getStartDate(), request.getEndDate(),
                    request.getFrom(), request.getSize());

            // ✅ 날짜 파라미터 파싱
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;

            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                startTime = LocalDateTime.parse(request.getStartDate());
            }

            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                endTime = LocalDateTime.parse(request.getEndDate());
            }

            // Elasticsearch에서 로그 검색
            var esData = elasticsearchService.searchLogs(
                    request.getIndex(),
                    request.getKeyword(),
                    request.getLogLevel(),
                    startTime,
                    endTime,
                    request.getFrom(),
                    request.getSize()
            );

            // Converter를 통한 DTO 변환
            LogSearchResponseDTO response = logsConverter.toSearchDTO(
                    esData,
                    request.getFrom(),
                    request.getSize()
            );

            return ResponseEntity.ok(ApiResponseDTO.success(response));

        } catch (Exception e) {
            log.error("Failed to search logs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("로그 검색 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 최근 에러 로그 조회
     *
     * @param limit 조회 개수 (기본: 20)
     * @return ApiResponseDTO<Map>
     */
    @GetMapping("/errors")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getRecentErrors(
            @RequestParam(defaultValue = "20") int limit) {

        try {
            log.info("Fetching recent errors: limit={}", limit);

            var errors = elasticsearchService.getRecentErrors(limit);

            return ResponseEntity.ok(ApiResponseDTO.success(Map.of(
                    "total", errors.size(),
                    "errors", errors
            )));

        } catch (Exception e) {
            log.error("Failed to fetch recent errors", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("에러 로그 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 로그 통계 조회
     *
     * @param index 인덱스 패턴
     * @return ApiResponseDTO<Map>
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getLogStats(
            @RequestParam(defaultValue = "application-logs-*") String index) {

        try {
            log.info("Fetching log stats: index={}", index);

            var stats = elasticsearchService.countByLogLevel(index);

            return ResponseEntity.ok(ApiResponseDTO.success(Map.of(
                    "index", index,
                    "stats", stats
            )));

        } catch (Exception e) {
            log.error("Failed to fetch log stats", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용 가능한 인덱스 패턴 목록 조회
     *
     * @return ApiResponseDTO<List<Map>>
     */
    @GetMapping("/indices")
    public ResponseEntity<ApiResponseDTO<List<Map<String, String>>>> getAvailableIndices() {
        try {
            List<Map<String, String>> indices = List.of(
                    Map.of("value", "application-logs-*", "label", "애플리케이션 로그"),
                    Map.of("value", "access-logs-*", "label", "접근 로그"),
                    Map.of("value", "error-logs-*", "label", "에러 로그"),
                    Map.of("value", "performance-metrics-*", "label", "성능 메트릭"),
                    Map.of("value", "database-logs-*", "label", "DB 로그"),
                    Map.of("value", "audit-logs-*", "label", "감사 로그"),
                    Map.of("value", "security-logs-*", "label", "보안 로그")
            );

            return ResponseEntity.ok(ApiResponseDTO.success(indices));

        } catch (Exception e) {
            log.error("Failed to fetch available indices", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.fail("인덱스 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}