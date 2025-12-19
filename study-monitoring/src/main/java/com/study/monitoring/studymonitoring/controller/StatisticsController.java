package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.model.dto.request.LogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.StatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.LogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.StatisticsResponseDTO;
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
     *
     * GET: http://localhost:8081/api/statistics/timeseries?metricType=TPS&startTime=2025-12-01T00:00:00&endTime=2025-12-18T23:59:59&timePeriod=DAY&aggregationType=AVG
     * @param request
     * paramter1: metricType      = 메트릭유형
     * paramter2: startTime       = 시작일자
     * paramter3: endTime         = 종료일자
     * paramter4: timePeriod      = 시간주기
     * paramter5: aggregationType = 집계유형
     */
    @GetMapping("/timeseries")
    public ResponseEntity<ApiResponseDTO<StatisticsResponseDTO>> getTimeSeriesStatistics(
            @Valid @ModelAttribute StatisticsQueryRequestDTO request)
    {
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
     *
     * GET: http://localhost:8081/api/statistics/logs?startTime=2025-12-01T00:00:00&endTime=2025-12-18T23:59:59&timePeriod=DAY&logLevel=ERROR
     * @param request
     * paramter1: 시작일자
     * paramter2: 종료일자
     * paramter3: 시간주기
     * paramter4: 로그유형
     **/
    @GetMapping("/logs")
    public ResponseEntity<ApiResponseDTO<LogStatisticsResponseDTO>> getLogStatistics(
            @Valid @ModelAttribute LogStatisticsQueryRequestDTO request)
    {
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
}
