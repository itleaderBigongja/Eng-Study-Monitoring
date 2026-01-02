package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ElasticsearchService {

    /**
     * 로그 검색 (날짜 필터 추가)
     *
     * @param indexPattern 인덱스 패턴
     * @param keyword 검색 키워드
     * @param logLevel 로그 레벨
     * @param startDate 시작 날짜 (옵션)
     * @param endDate 종료 날짜 (옵션)
     * @param from 페이지 시작
     * @param size 페이지 크기
     * @return 검색 결과 Map
     */
    Map<String, Object> searchLogs(
            String indexPattern,
            String keyword,
            String logLevel,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int from,
            int size
    );

    Map<String, Long> countByLogLevel(String indexPattern);

    List<Map<String, Object>> getRecentErrors(int limit);

    List<Map<String, Object>> getLogDistributionByTime(
            String indexPattern,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String timePeriod,
            String logLevel
    );

    Map<String, Long> countByHttpMethod(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Long> countByStatusCode(String indexPattern, LocalDateTime start, LocalDateTime end);

    Double getAverageResponseTime(String indexPattern, LocalDateTime start, LocalDateTime end);

    List<Map<String, Object>> getAccessLogDistributionByTime(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end,
            String timePeriod
    );

    /**
     * 에러 로그를 검색하고 페이지네이션된 결과를 반환합니다.
     * @param type 에러 타입 (예: "NullPointerException" 등, 없으면 전체)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 에러 로그 목록과 페이징 정보
     */
    PageResponseDTO<Map<String, Object>> searchErrorLogs(String type, int page, int size);

    // ============================================
    // error-logs 통계용 메서드
    // ============================================
    Map<String, Long> countByErrorType(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Long> countBySeverity(String indexPattern, LocalDateTime start, LocalDateTime end);

    List<Map<String, Object>> getErrorLogDistributionByTime(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end,
            String timePeriod
    );

    // ============================================
    // performance-metrics 통계용 메서드
    // ============================================
    Map<String, Double> getSystemMetricsAggregation(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end
    );

    Map<String, Double> getJvmMetricsAggregation(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Map<String, Object>> getPerformanceMetricsDistributionByTime(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end,
            String timePeriod
    );

    // ============================================
    // database-logs 통계용 메서드
    // ============================================
    Map<String, Long> countByOperation(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Long> countByTable(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Object> getQueryPerformanceStats(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Map<String, Object>> getDatabaseLogDistributionByTime(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end,
            String timePeriod
    );

    // ============================================
    // audit-logs 통계용 메서드
    // ============================================
    Map<String, Long> countByEventAction(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Long> countByCategory(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Long> countByEventResult(String indexPattern, LocalDateTime start, LocalDateTime end);

    List<Map<String, Object>> getAuditLogDistributionByTime(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end,
            String timePeriod
    );

    Map<String, Long> countByThreatLevel(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Long> countByAttackType(String indexPattern, LocalDateTime start, LocalDateTime end);

    Map<String, Long> getBlockStatistics(String indexPattern, LocalDateTime start, LocalDateTime end);

    List<Map<String, Object>> getSecurityLogDistributionByTime(
            String indexPattern,
            LocalDateTime start,
            LocalDateTime end,
            String timePeriod
    );
}