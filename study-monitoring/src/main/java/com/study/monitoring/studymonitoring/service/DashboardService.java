package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.request.MetricsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;

import java.util.Map;

/**
 * 대시보드 서비스 인터페이스
 * * 역할:
 * - Prometheus, Elasticsearch, DB 데이터를 종합하여 비즈니스 로직 처리
 * - Controller와 데이터 소스 간의 결합도 제거
 */
public interface DashboardService {

    /**
     * 대시보드 전체 개요 조회
     */
    DashboardResponseDTO getDashboardOverview();

    /**
     * 실시간 메트릭 상세 조회 (차트용)
     * [이동됨] Controller -> Service
     */
    MetricsResponseDTO getMetrics(MetricsQueryRequestDTO request);

    /**
     * 프로세스 현황 및 요약 조회
     * [이동됨] Controller -> Service
     */
    Map<String, Object> getProcessStatus();

    /**
     * 에러 로그 목록 조회 (페이징 포함)
     * [이동됨] Controller -> Service
     */
    PageResponseDTO<DashboardResponseDTO.ErrorLogDTO> getErrorLogs(String type, int page, int size);
}