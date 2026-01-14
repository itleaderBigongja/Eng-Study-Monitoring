package com.study.monitoring.studymonitoring.service;

import java.util.Map;

public interface HealthCheckService {
    /** 전체 시스템 상태 조회 */
    Map<String, Object> getOverallHealth();

    /** Elasticsearch 상태 조회 */
    Map<String, Object> getElasticsearchHealth();

    /** Database 상태 조회 */
    Map<String, Object> getDatabaseHealth();

    /** Prometheus 상태 조회 */
    Map<String, Object> getPrometheusHealth();
}