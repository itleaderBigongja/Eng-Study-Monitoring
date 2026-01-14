package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    /**
     * 전체 헬스체크
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(healthCheckService.getOverallHealth());
    }

    /**
     * Elasticsearch 연결 확인
     */
    @GetMapping("/elasticsearch")
    public ResponseEntity<Map<String, Object>> checkElasticsearchEndpoint() {
        return ResponseEntity.ok(healthCheckService.getElasticsearchHealth());
    }

    /**
     * Database 연결 확인
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> checkDatabaseEndpoint() {
        return ResponseEntity.ok(healthCheckService.getDatabaseHealth());
    }

    /**
     * Prometheus 연결 확인
     */
    @GetMapping("/prometheus")
    public ResponseEntity<Map<String, Object>> checkPrometheusEndpoint() {
        return ResponseEntity.ok(healthCheckService.getPrometheusHealth());
    }
}