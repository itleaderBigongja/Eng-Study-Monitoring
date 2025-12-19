package com.study.monitoring.studymonitoring.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final ElasticsearchClient elasticsearchClient;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${prometheus.url}")
    private String prometheusUrl;

    /**
     * 전체 헬스체크
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("elasticsearch", checkElasticsearch());
        health.put("database", checkDatabase());
        health.put("prometheus", checkPrometheus());

        return ResponseEntity.ok(health);
    }

    /**
     * Elasticsearch 연결 확인
     */
    @GetMapping("/elasticsearch")
    public ResponseEntity<Map<String, Object>> checkElasticsearchEndpoint() {
        return ResponseEntity.ok(checkElasticsearch());
    }

    /**
     * Database 연결 확인
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> checkDatabaseEndpoint() {
        return ResponseEntity.ok(checkDatabase());
    }

    /**
     * ✅ 추가: Prometheus 연결 확인
     */
    @GetMapping("/prometheus")
    public ResponseEntity<Map<String, Object>> checkPrometheusEndpoint() {
        return ResponseEntity.ok(checkPrometheus());
    }

    /**
     * Elasticsearch 상태 확인 내부 메서드
     */
    private Map<String, Object> checkElasticsearch() {
        Map<String, Object> status = new HashMap<>();

        try {
            var info = elasticsearchClient.info();

            status.put("status", "UP");
            status.put("cluster_name", info.clusterName());
            status.put("version", info.version().number());
            status.put("details", Map.of(
                    "cluster_uuid", info.clusterUuid(),
                    "tagline", info.tagline()
            ));

            log.info("Elasticsearch health check: OK");

        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("Elasticsearch health check failed", e);
        }

        return status;
    }

    /**
     * Database 상태 확인 내부 메서드
     */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> status = new HashMap<>();

        try {
            String version = jdbcTemplate.queryForObject(
                    "SELECT version()",
                    String.class
            );

            status.put("status", "UP");
            status.put("database", "PostgreSQL");
            status.put("version", version);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM monitoring_process",
                    Integer.class
            );

            status.put("details", Map.of(
                    "process_count", count != null ? count : 0
            ));

            log.info("Database health check: OK");

        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("Database health check failed", e);
        }

        return status;
    }

    /**
     * Prometheus 상태 확인 내부 메서드
     */
    private Map<String, Object> checkPrometheus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // ✅ Prometheus API 호출
            String url = prometheusUrl + "/api/v1/status/config";
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                status.put("status", "UP");
                status.put("url", prometheusUrl);
                status.put("api_version", "v1");
            } else {
                status.put("status", "DOWN");
                status.put("error", "Invalid response from Prometheus");
            }

            log.info("Prometheus health check: OK");

        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("url", prometheusUrl);
            status.put("error", e.getMessage());
            log.error("Prometheus health check failed", e);
        }

        return status;
    }
}