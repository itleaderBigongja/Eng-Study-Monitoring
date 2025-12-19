package com.study.monitoring.studymonitoring.service;

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
}
