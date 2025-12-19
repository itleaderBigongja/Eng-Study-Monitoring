package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.converter.MetricsConverter;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusServiceImpl implements PrometheusService {

    @Value("${prometheus.url}")
    private String prometheusUrl;

    private final RestTemplate restTemplate;
    private final MetricsConverter metricsConverter;

    @Override
    public Map<String, Object> queryInstance(String query) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(prometheusUrl + "/api/v1/query")
                    .queryParam("query", query)
                    .build()
                    .toUri();

            log.debug("Prometheus query url: {}", uri);
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                return response;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Failed to query Prometheus: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> queryRange(String query, long start, long end, String step) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(prometheusUrl + "/api/v1/query_range")
                    .queryParam("query", query)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("step", step)
                    .build()
                    .toUri();

            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return (List<Map<String, Object>>) data.get("result");
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to query Prometheus range: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // ⬇️ 아래 메서드들이 핵심 수정 부분입니다! (애플리케이션 타입별 분기 처리)
    // =========================================================================

    @Override
    public Double getHeapMemoryUsage(String application) {
        String query;
        if ("elasticsearch".equals(application)) {
            // Elasticsearch: 전용 메트릭 사용
            query = "elasticsearch_jvm_memory_used_bytes{area=\"heap\"} / elasticsearch_jvm_memory_max_bytes{area=\"heap\"} * 100";
        } else if ("postgres".equals(application)) {
            // PostgreSQL: 자바가 아니므로 Heap 개념이 없음 -> 대신 물리 메모리 사용량(Resident Memory)을 바이트 단위로 반환
            // (퍼센트로 보고 싶다면 전체 RAM 크기로 나누어야 하는데, 여기선 단순히 사용량만 체크하거나 0 리턴)
            // 여기서는 시각화를 위해 '총 메모리 대비 프로세스 사용량'을 알기 어려우므로 단순히 사용 Byte를 리턴하거나 0 처리
            // 예시: 단순히 현재 사용중인 메모리(Byte)를 가져옴
            query = "process_resident_memory_bytes{application=\"postgres\"}";
        } else {
            // Spring Boot (기존)
            query = String.format(
                    "jvm_memory_used_bytes{application=\"%s\",area=\"heap\"} / " +
                            "jvm_memory_max_bytes{application=\"%s\",area=\"heap\"} * 100",
                    application, application
            );
        }
        return metricsConverter.extractValue(queryInstance(query));
    }

    @Override
    public Double getTps(String application) {
        String query;
        if ("elasticsearch".equals(application)) {
            // Elasticsearch: '인덱싱(저장) 작업 횟수'를 TPS로 간주
            query = "rate(elasticsearch_indices_indexing_index_total[1m])";
        } else if ("postgres".equals(application)) {
            // PostgreSQL: '트랜잭션 커밋 횟수'를 TPS로 간주
            query = "rate(pg_stat_database_xact_commit{application=\"postgres\"}[1m])";
        } else {
            // Spring Boot: HTTP 요청 수
            query = String.format(
                    "rate(http_server_requests_seconds_count{application=\"%s\"}[1m])",
                    application
            );
        }
        return metricsConverter.extractValue(queryInstance(query));
    }

    @Override
    public Double getErrorRate(String application) {
        String query;
        if ("elasticsearch".equals(application)) {
            // Elasticsearch: 에러율을 직접 뽑기 어려우므로 0으로 두거나, 클러스터 상태가 Red(1)인지 체크하는 로직 등으로 대체 가능
            // 여기서는 일단 0.0 반환 (복잡도 감소)
            return 0.0;
        } else if ("postgres".equals(application)) {
            // PostgreSQL: (롤백 수 / (커밋+롤백 수)) * 100 -> 트랜잭션 실패율
            // 간단하게 롤백 발생 빈도만 체크
            query = "rate(pg_stat_database_xact_rollback{application=\"postgres\"}[1m])";
        } else {
            // Spring Boot: HTTP 500번대 에러 비율
            query = String.format(
                    "rate(http_server_requests_seconds_count{application=\"%s\",status=~\"5..\"}[5m]) / " +
                            "rate(http_server_requests_seconds_count{application=\"%s\"}[5m]) * 100",
                    application, application
            );
        }
        return metricsConverter.extractValue(queryInstance(query));
    }

    @Override
    public Double getCpuUsage(String application) {
        String query;
        if ("elasticsearch".equals(application)) {
            // Elasticsearch: 프로세스 CPU 사용률 (%)
            query = "elasticsearch_process_cpu_percent";
        } else if ("postgres".equals(application)) {
            // PostgreSQL: Exporter가 CPU 정보를 직접 잘 안 줌.
            // process_cpu_seconds_total이 있다면 사용 가능하나, 없으면 0 리턴
            // 안전하게 0 리턴 (Node Exporter를 써야 정확한 OS CPU가 나옴)
            return 0.0;
        } else {
            // Spring Boot: JVM 프로세스 CPU 사용률
            query = String.format(
                    "process_cpu_usage{application=\"%s\"} * 100",
                    application
            );
        }
        return metricsConverter.extractValue(queryInstance(query));
    }
}