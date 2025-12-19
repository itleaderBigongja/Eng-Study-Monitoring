package com.study.monitoring.studymonitoring.service;

import java.util.List;
import java.util.Map;

/**
 * Prometheus 메트릭 수집 서비스 인터페이스
 * * 역할:
 * - Prometheus HTTP API를 통한 메트릭 데이터 조회
 * - JVM, TPS, Error Rate 등 핵심 지표 제공
 */
public interface PrometheusService {

    /**
     * 현재 시점의 메트릭 값 조회(Instance Query)
     * @param query PromQL 쿼리
     * @return 메트릭 데이터 Map
     **/
    Map<String, Object> queryInstance(String query);

    /**
     * 시간 범위의 메트릭 조회( Range Query )
     *
     * @param query PromQL 쿼리
     * @param start 시작 시간(Unix timestamp)
     * @param end   종료 시간(Unix timestamp)
     * @param step  데이터 간격(예: "15s")
     * @return 시계열 데이터 리스트
     **/
    List<Map<String, Object>> queryRange(String query, long start, long end, String step);

    /**
     * JVM Heap 메모리 사용률 조회
     * @param application 애플리케이션 이름
     * @return Heap 사용률(%)
     **/
    Double getHeapMemoryUsage(String application);

    /**
     * TPS( Transactions Per Second ) 조회
     *
     * @param application 애플리케이션 이름
     * @return TPS 값
     **/
    Double getTps(String application);

    /**
     * HTTP 에러율 조회
     *
     * @param application 애플리케이션 이름
     * @return 에러율(%)
     **/
    Double getErrorRate(String application);

    /**
     * CPU 사용률
     *
     * @param application
     * @return CPU(%)
     **/
    Double getCpuUsage(String application);
}
