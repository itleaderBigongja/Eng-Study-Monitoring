package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.request.*;
import com.study.monitoring.studymonitoring.model.dto.response.*;

/**
 * 통합 통계 조회 서비스
 *
 * 7개 Elasticsearch 인덱스 통계 지원:
 * - application-logs
 * - access-logs
 * - error-logs
 * - performance-metrics
 * - database-logs
 * - audit-logs
 * - security-logs
 */
public interface StatisticsService {

    /**
     * 시계열 데이터 통계 조회 (Prometheus + PostgreSQL)
     */
    StatisticsResponseDTO getTimeSeriesStatistics(StatisticsQueryRequestDTO request);

    /**
     * 애플리케이션 로그 통계 조회 (Elasticsearch)
     * 인덱스: application-logs-*
     */
    LogStatisticsResponseDTO getLogStatistics(LogStatisticsQueryRequestDTO request);

    /**
     * 접근 로그 통계 조회 (Elasticsearch)
     * 인덱스: access-logs-*
     */
    AccessLogStatisticsResponseDTO getAccessLogStatistics(AccessLogStatisticsQueryRequestDTO request);

    /**
     * 에러 로그 통계 조회 (Elasticsearch)
     * 인덱스: error-logs-*
     */
    ErrorLogStatisticsResponseDTO getErrorLogStatistics(ErrorLogStatisticsQueryRequestDTO request);

    /**
     * 성능 메트릭 통계 조회 (Elasticsearch)
     * 인덱스: performance-metrics-*
     */
    PerformanceMetricsStatisticsResponseDTO getPerformanceMetricsStatistics(
            PerformanceMetricsStatisticsQueryRequestDTO request);

    /**
     * 데이터베이스 로그 통계 조회 (Elasticsearch)
     * 인덱스: database-logs-*
     */
    DatabaseLogStatisticsResponseDTO getDatabaseLogStatistics(DatabaseLogStatisticsQueryRequestDTO request);

    /**
     * 감사 로그 통계 조회 (Elasticsearch)
     * 인덱스: audit-logs-*
     */
    AuditLogStatisticsResponseDTO getAuditLogStatistics(AuditLogStatisticsQueryRequestDTO request);

    /**
     * 보안 로그 통계 조회 (Elasticsearch)
     * 인덱스: security-logs-*
     */
    SecurityLogStatisticsResponseDTO getSecurityLogStatistics(SecurityLogStatisticsQueryRequestDTO request);
}