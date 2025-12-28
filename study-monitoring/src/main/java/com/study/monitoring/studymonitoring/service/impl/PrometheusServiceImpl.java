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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
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
            // 1. 쿼리 문자열을 UTF-8로 직접 인코딩 ( + -> %2B, { -> %7B 등으로 변환됨)
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // 2. URI 객체 직접 생성 (String으로 넘기면 RestTemplate이 또 건드려서 망가짐)
            // 주의: URL 파라미터 연결(?query=)은 직접 문자열로 붙여야 함
            URI uri = URI.create(prometheusUrl + "/api/v1/query?query=" + encodedQuery);

            // log.info("Request URI: {}", uri); // 디버깅용 로그

            // 3. URI 객체를 RestTemplate에 전달
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                return response;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Failed to query Prometheus (Instance): {}", e.getMessage());
            // 상세 에러 확인을 위해 스택 트레이스 출력 (개발 중에만 사용)
            // e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> queryRange(String query, long start, long end, String step) {
        try {
            // 1. 각 파라미터 인코딩
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // 2. 전체 URL 조립
            String urlString = String.format(
                    "%s/api/v1/query_range?query=%s&start=%d&end=%d&step=%s",
                    prometheusUrl, encodedQuery, start, end, step
            );

            // 3. URI 객체 생성
            URI uri = URI.create(urlString);

            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                // 안전한 형변환 및 Null 체크
                Object dataObj = response.get("data");
                if (dataObj instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    Object resultObj = data.get("result");
                    if (resultObj instanceof List) {
                        return (List<Map<String, Object>>) resultObj;
                    }
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to query Prometheus (Range): {}", e.getMessage());
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
            // ✅ [변경] ES도 DB처럼 '메모리' 그래프 위치에 '디스크 사용량(MB)'을 표시
            // elasticsearch_filesystem_data_size_bytes: 실제 데이터가 차지하는 용량
            query = "sum(elasticsearch_indices_store_size_bytes) / 1024 / 1024";
        } else if ("postgres".equals(application)) {
            // PostgreSQL: 디스크 사용량 (MB)
            query = "sum(pg_database_size_bytes) / 1024 / 1024";
        } else {
            // Spring Boot: JVM Heap (%)
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
            // ✅ [변경] ES도 DB처럼 'CPU' 그래프 위치에 '현재 활성 작업 수(Count)' 표시
            // indexing_index_current: 현재 색인 중인 문서 수
            // search_query_current: 현재 수행 중인 검색 요청 수
            // 이 둘을 합쳐서 "현재 DB가 처리 중인 작업량"으로 정의
            query = "sum(elasticsearch_indices_indexing_index_current) + sum(elasticsearch_indices_search_query_current)";
        } else if ("postgres".equals(application)) {
            // PostgreSQL: 활성 연결 수 (Active Connections)
            query = "sum(pg_stat_activity_count{state='active'})";
        } else {
            // Spring Boot: CPU 사용률 (%)
            query = String.format(
                    "process_cpu_usage{application=\"%s\"} * 100",
                    application
            );
        }
        return metricsConverter.extractValue(queryInstance(query));
    }

    // --- [신규 추가] 실시간 상태 조회 구현 ---
    @Override
    public Map<String, String> getRealTimeStatusMap() {
        Map<String, String> statusMap = new HashMap<>();
        try {
            // "up" 쿼리는 현재 살아있는 모든 타겟을 1로 리턴합니다.
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(prometheusUrl + "/api/v1/query")
                    .queryParam("query", "up")
                    .build()
                    .toUri();

            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");

                for (Map<String, Object> result : results) {
                    Map<String, String> metric = (Map<String, String>) result.get("metric");
                    List<Object> value = (List<Object>) result.get("value");

                    // 1. 애플리케이션 이름 추출 (application 라벨 우선, 없으면 job 라벨)
                    String appName = metric.getOrDefault("application", metric.getOrDefault("job", "unknown"));

                    // 2. 상태 값 추출 ("1" = UP, "0" = DOWN)
                    String statusVal = (String) value.get(1);

                    statusMap.put(appName, "1".equals(statusVal) ? "UP" : "DOWN");
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch real-time status map", e);
        }
        return statusMap;
    }

    @Override
    public Double getUptime(String application) {
        String query;

        if ("postgres".equalsIgnoreCase(application)) {
            // ✅ [수정] Postgres Uptime을 가져오는 4단계 안전장치 적용
            // 1. pg_postmaster_start_time_seconds (최신 DB 엔진 가동 시간)
            // 2. pg_start_time_seconds (구버전 호환)
            // 3. process_start_time_seconds{application="postgres"} (프로젝트 라벨 컨벤션 - 가장 유력)
            // 4. process_start_time_seconds{job="postgres"} (Job 설정 기준)
            query = "time() - (" +
                    "sum(pg_postmaster_start_time_seconds) " +
                    "or sum(pg_start_time_seconds) " +
                    "or sum(process_start_time_seconds{application=\"postgres\"}) " +
                    "or sum(process_start_time_seconds{job=\"postgres\"})" +
                    ")";
        } else {
            // 기존 로직 유지
            query = String.format("time() - process_start_time_seconds{application=\"%s\"}", application);
        }

        try {
            Map<String, Object> response = queryInstance(query);
            Double val = metricsConverter.extractValue(response);

            // 값이 유효하면 반환, 아니면 0.0 (Down)
            return (val != null && val > 0) ? val : 0.0;

        } catch (Exception e) {
            log.warn("Failed to get uptime for {}: {}", application, e.getMessage());
            return 0.0;
        }
    }
}