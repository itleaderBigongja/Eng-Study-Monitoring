package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;

/**
 * 대시보드 서비스 인터페이스
 *
 * 역할:
 * - Prometheus에서 실시간 메트릭 조회
 * - Elasticsearch에서 로그 정보 조회
 * - DB에서 프로세스 메타데이터 조회
 * - 위 데이터를 종합하여 대시보드 응답 생성
 * */
public interface DashboardService {

    /**
     * 대시보드 전체 개요 조회
     * @return 대시보드 응답 DTO( 프로세스 상태, 메트릭, 에러, 로그 통계 )
     * */
    DashboardResponseDTO getDashboardOverview();
}
