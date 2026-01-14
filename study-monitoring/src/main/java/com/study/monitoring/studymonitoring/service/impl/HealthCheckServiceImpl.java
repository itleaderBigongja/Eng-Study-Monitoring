package com.study.monitoring.studymonitoring.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse; // ✅ 추가된 구체적인 타입
import com.study.monitoring.studymonitoring.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private final ElasticsearchClient elasticsearchClient;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${prometheus.url}")
    private String prometheusUrl;

    @Override
    public Map<String, Object> getOverallHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("elasticsearch", getElasticsearchHealth());
        health.put("database", getDatabaseHealth());
        health.put("prometheus", getPrometheusHealth());
        return health;
    }

    @Override
    public Map<String, Object> getElasticsearchHealth() {
        Map<String, Object> status = new HashMap<>();

        try {
            // 🚩 [변경 포인트] var 대신 정확한 타입(InfoResponse) 명시
            InfoResponse info = elasticsearchClient.info();

            status.put("status", "UP");
            status.put("cluster_name", info.clusterName());
            status.put("version", info.version().number());

            // Map.of는 Java 9+ 기능 (타입 추론이 쉬움)
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

    @Override
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> status = new HashMap<>();

        try {
            String version = jdbcTemplate.queryForObject(
                    "SELECT version()",
                    String.class
            );

            Integer count = 0;
            try {
                count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM monitoring_process",
                        Integer.class
                );
            } catch (Exception e) {
                log.warn("Table monitoring_process check failed: {}", e.getMessage());
            }

            status.put("status", "UP");
            status.put("database", "PostgreSQL");
            status.put("version", version);
            status.put("details", Map.of("process_count", count != null ? count : 0));

            log.info("Database health check: OK");

        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("Database health check failed", e);
        }

        return status;
    }

    @Override
    public Map<String, Object> getPrometheusHealth() {
        Map<String, Object> status = new HashMap<>();

        try {
            String url = prometheusUrl + "/api/v1/status/config";

            // 🚩 [변경 포인트] Raw type Map 대신 제네릭 명시 (IDE 경고 방지)
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

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