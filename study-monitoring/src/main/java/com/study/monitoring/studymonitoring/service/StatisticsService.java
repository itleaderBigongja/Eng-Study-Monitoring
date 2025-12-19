package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.request.LogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.StatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.LogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.StatisticsResponseDTO;

/**
 * 신규: 통합 통계 조회 서비스
 *
 * 역할:
 * - 30일 이내: Prometheus 조회
 * - 30일 이후: PostgreSQL 조회
 * - 두 데이터를 병합하여 반환
 * */
public interface StatisticsService {

    /**
     * 시계열 데이터 통계 조회
     *
     * @param request 통계 조회 조건
     * @return StatisticsResponseDTO
     */
    StatisticsResponseDTO getTimeSeriesStatistics(StatisticsQueryRequestDTO request);

    /**
     * 로그 통계 조회
     *
     * @param request 로그 통계 조회 조건
     * @return LogStatisticsResponseDTO
     */
    LogStatisticsResponseDTO getLogStatistics(LogStatisticsQueryRequestDTO request);
}
