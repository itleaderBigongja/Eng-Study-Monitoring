package com.study.monitoring.studymonitoring.service;

import java.util.Map;

/**
 * ============================================================================
 * 메트릭 비즈니스 로직 서비스 인터페이스
 * ============================================================================
 *
 * 역할:
 * - 현재 메트릭 조회 비즈니스 로직 처리
 * - 응답 데이터 구조 생성
 * - PrometheusService에 실제 데이터 조회 위임
 *
 * 계층 구조:
 * Controller → MetricsService (비즈니스 로직) → PrometheusService (인프라)
 *
 * ============================================================================
 */
public interface MetricsService {

    /**
     * 현재 메트릭 조회
     *
     * 📌 사용처: MetricsController.getCurrentMetrics()
     * 📌 호출 주기: 5초마다 (프론트엔드 자동 갱신)
     *
     * 처리 흐름:
     * 1. PrometheusService로부터 각 메트릭 데이터 조회
     * 2. 응답 포맷에 맞게 데이터 구조화
     * 3. 타임스탬프 추가
     *
     * @param application 애플리케이션 이름 (eng-study, monitoring, postgres, elasticsearch 등)
     * @return 현재 메트릭 데이터 (TPS, Heap, CPU, Error Rate)
     */
    Map<String, Object> getCurrentMetrics(String application);
}