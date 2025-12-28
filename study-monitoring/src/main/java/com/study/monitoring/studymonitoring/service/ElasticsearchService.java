package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 로그 검색 서비스 인터페이스
 *
 * 역할:
 * - 로그 데이터 검색 및 집계
 * - 키워드, 로그 레벨, 시간 범위 기반 필터링
 * - 통계 및 에러 로그 조회
 */
public interface ElasticsearchService {

    /**
     * 로그 검색
     *
     * @param indexPattern 인덱스 패턴 (예: "application-logs-*")
     * @param keyword 검색 키워드
     * @param logLevel 로그 레벨 (INFO, WARN, ERROR)
     * @param from 시작 페이지
     * @param size 페이지 크기
     * @return 검색 결과 Map (logs, total 등 포함)
     */
    Map<String, Object> searchLogs(
            String indexPattern,
            String keyword,
            String logLevel,
            int from,
            int size
    );

    /**
     * 로그 레벨별 카운트 집계
     *
     * @param indexPattern 인덱스 패턴
     * @return 로그 레벨별 카운트 Map (예: INFO=100, ERROR=5)
     */
    Map<String, Long> countByLogLevel(String indexPattern);

    /**
     * 최근 에러 로그 조회
     *
     * @param limit 조회 개수
     * @return 에러 로그 리스트
     */
    List<Map<String, Object>> getRecentErrors(int limit);

    /**
     * 시간대별 로그 분포
     *
     * @param indexPattern 인덱스 패턴
     * @param startTime 시작일자
     * @param endTime 종료일자
     * @param timePeriod 시간주기
     * @param logLevel 로그 레벨(null일 경우 전체)
     * @return 시간대별 카운트 리스트
     */
    List<Map<String, Object>> getLogDistributionByTime(
            String indexPattern,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String timePeriod,
            String logLevel
    );

    // ============================================
    // access-logs 통계용 메서드
    // ============================================
    /** HTTP 메서드별 카운트 조회
     *  @return {"GET": 1000, "POST": 500, "PUT": 200} */
    Map<String, Long> countByHttpMethod(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** HTTP 상태코드별 카운트 조회
     *  @return {"200": 8000, "404": 100, "500": 50} */
    Map<String, Long> countByStatusCode(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 평균 응답시간 조회 (ms) */
    Double getAverageResponseTime(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 시간대별 접근 로그 분포 조회 */
    List<Map<String, Object>> getAccessLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod);

    // ============================================
    // error-logs 통계용 메서드
    // ============================================
    /** 에러 타입별 카운트 조회
     *  @return {"NullPointerException": 50, "SQLException": 30} */
    Map<String, Long> countByErrorType(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 심각도별 카운트 조회
     *  @return {"ERROR": 100, "CRITICAL": 20, "FATAL": 5} */
    Map<String, Long> countBySeverity(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 시간대별 에러 로그 분포 조회 */
    List<Map<String, Object>> getErrorLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod);

    // ============================================
    // performance-metrics 통계용 메서드
    // ============================================
    /** 시스템 메트릭 집계 조회 (CPU, Memory, Disk)
     *  @return {"avg_cpu": 45.5, "max_cpu": 89.2, "avg_memory": 68.5, ...} */
    Map<String, Double> getSystemMetricsAggregation(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** JVM 메트릭 집계 조회 (Heap, GC, Thread) */
    Map<String, Double> getJvmMetricsAggregation(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 시간대별 성능 메트릭 분포 조회 */
    List<Map<String, Object>> getPerformanceMetricsDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod);

    // ============================================
    // database-logs 통계용 메서드
    // ============================================
    /** Operation별 카운트 조회 (SELECT, INSERT, UPDATE, DELETE) */
    Map<String, Long> countByOperation(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 테이블별 쿼리 수 조회 */
    Map<String, Long> countByTable(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 쿼리 성능 지표 조회 (평균/최대 실행시간, 느린 쿼리 수) */
    Map<String, Object> getQueryPerformanceStats(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 시간대별 데이터베이스 로그 분포 조회 */
    List<Map<String, Object>> getDatabaseLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod);

    // ============================================
    // audit-logs 통계용 메서드
    // ============================================
    /** 이벤트 액션별 카운트 조회 */
    Map<String, Long> countByEventAction(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 카테고리별 카운트 조회 */
    Map<String, Long> countByCategory(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 이벤트 결과별 카운트 조회 (success/failure) */
    Map<String, Long> countByEventResult(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 시간대별 감사 로그 분포 조회 */
    List<Map<String, Object>> getAuditLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod);

    // ============================================
    // security-logs 통계용 메서드
    // ============================================
    /** 위협 레벨별 카운트 조회 */
    Map<String, Long> countByThreatLevel(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 공격 타입별 카운트 조회 */
    Map<String, Long> countByAttackType(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 차단 통계 조회 (전체/차단/허용 공격 수) */
    Map<String, Long> getBlockStatistics(String indexPattern, LocalDateTime start, LocalDateTime end);

    /** 시간대별 보안 로그 분포 조회 */
    List<Map<String, Object>> getSecurityLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod);

    /** ElasticsearchService 대시보드에서 최신 에러 목록에서 error-logs 인덱스 목록 보여주기
     *  type 파라미터 추가 ("APP" or "SYSTEM") */
    PageResponseDTO<Map<String, Object>> searchErrorLogs(String type, int page, int size);
}
