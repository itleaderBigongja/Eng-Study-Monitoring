package com.study.monitoring.studymonitoring.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import com.study.monitoring.studymonitoring.util.ElasticsearchQueryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 서비스 구현
 * 주요 개선사항:
 * 1. ElasticsearchQueryUtil 활용
 * 2. 에러 처리 강화
 * 3. null 안전성 개선
 * 4. 로깅 추가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> searchLogs(
            String indexPattern,
            String keyword,
            String logLevel,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int from,
            int size)
    {
        try {
            log.debug("Searching logs: index={}, keyword={}, logLevel={}, startDate={}, endDate={}, from={}, size={}",
                    indexPattern, keyword, logLevel, startDate, endDate, from, size);

            // 1. Bool 쿼리 빌드
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 키워드 검색 (Full-text Search)
            if (keyword != null && !keyword.isEmpty()) {
                boolQuery.must(ElasticsearchQueryUtil.buildMultiFieldSearchQuery(keyword));
            }

            // 로그 레벨 필터
            if (logLevel != null && !logLevel.isEmpty()) {
               boolQuery.must(ElasticsearchQueryUtil.buildLogLevelQuery(logLevel));
            }

            // 날짜 범위 필터
            if (startDate != null && endDate != null) {
                boolQuery.must(ElasticsearchQueryUtil.buildDateRangeQuery(startDate, endDate));
            } else if (startDate != null) {
                // 시작 날짜만 있는 경우 (이후 모든 로그)
                boolQuery.must(ElasticsearchQueryUtil.buildDateRangeQueryFrom(startDate));
            } else if (endDate != null) {
                // 종료 날짜만 있는 경우 (이전 모든 로그)
                boolQuery.must(ElasticsearchQueryUtil.buildDateRangeQueryTo(endDate));
            }

            // 2. Elasticsearch 검색 실행
            SearchResponse<Map> response = elasticsearchClient.search(
                    s -> s.index(indexPattern)
                            .from(from)
                            .size(size)
                            .query(boolQuery.build()._toQuery())
                            .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc))),
                    Map.class
            );

            // 3. 결과 변환
            List<Map<String, Object>> logs = response.hits().hits().stream()
                    .map(this::convertHitToMap)
                    .collect(Collectors.toList());

            // 4. 응답 구성
            Map<String, Object> result = new HashMap<>();
            result.put("total", response.hits().total() != null ? response.hits().total().value() : 0);
            result.put("logs", logs);
            result.put("took", response.took());

            log.debug("Found {} logs", result.get("total"));
            return result;

        } catch (Exception e) {
            log.error("Failed to search logs: indexPattern={}", indexPattern, e);
            return createErrorResponse("로그 검색 실패: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Long> countByLogLevel(String indexPattern) {
        try {
            log.debug("Counting logs by level: index={}", indexPattern);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0)
                            .aggregations("by_log_level", Aggregation.of(a -> a
                                    .terms(t -> t
                                            .field("log_level.keyword").size(10)))), Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_log_level") != null) {
                response.aggregations().get("by_log_level").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()));
            }
            log.debug("Log level counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by log level: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> getRecentErrors(int limit) {
        try {
            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                            .index("application-logs-*,error-logs-*") // 전체 로그 대상
                            .size(limit)
                            .query(q -> q.terms(t -> t
                                    .field("log_level.keyword")
                                    .terms(v -> v.value(List.of(
                                            FieldValue.of("ERROR"),
                                            FieldValue.of("CRITICAL"),
                                            FieldValue.of("FATAL")
                                    )))
                            ))
                            .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc))),
                    Map.class
            );

            return response.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> map = this.convertHitToMap(hit);
                        String realLevel = resolveLogLevel(map); // ✅ 여기도 적용!

                        map.put("level", realLevel);
                        map.put("logLevel", realLevel);
                        map.put("log_level", realLevel);

                        return map;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching recent errors", e);
            return Collections.emptyList();
        }
    }

    // 시간대 별 분포 - Application Logs
    @Override
    public List<Map<String, Object>> getLogDistributionByTime(
            String indexPattern,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String timePeriod,
            String logLevel)
    {
        try {
            log.info("Querying log distribution: {} ~ {}, period={}, logLevel={}",
                    startTime, endTime, timePeriod, logLevel);

            String interval = calculateInterval(timePeriod);
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(startTime, endTime);
            boolQuery.must(timeRangeQuery);

            if (logLevel != null && !logLevel.isEmpty() && !"undefined".equals(logLevel)) {
                boolQuery.must(ElasticsearchQueryUtil.buildLogLevelQuery(logLevel));
            }

            // ✅ epoch milliseconds로 변환
            long startEpochMs = startTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
            long endEpochMs = endTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern)
                            .size(0)
                            .query(boolQuery.build()._toQuery())
                            .aggregations("logs_over_time",
                                    Aggregation.of(a -> a
                                            .dateHistogram(dh -> dh
                                                    .field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .timeZone("Asia/Seoul")
                                                    .format("yyyy-MM-dd HH:mm:ss")
                                                    .minDocCount(0)
                                                    .extendedBounds(b -> b
                                                            // ✅ 수정: epoch milliseconds 사용
                                                            .min(FieldDateMath.of(f -> f.value((double) startEpochMs)))
                                                            .max(FieldDateMath.of(f -> f.value((double) endEpochMs)))
                                                    )
                                            )
                                    )
                            ),
                    Void.class
            );

            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("logs_over_time") != null) {
                response.aggregations().get("logs_over_time")
                        .dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("count", bucket.docCount());
                            distribution.add(entry);
                        });
            }
            log.info("Distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get log distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> countByHttpMethod(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by HTTP method: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(s ->
                            s.index(indexPattern).size(0)
                                    .query(timeRangeQuery)
                                    .aggregations("by_method", Aggregation.of(a ->
                                            // ✅ 수정: http.method -> http.method.keyword (nested object 내 keyword 필드)
                                            a.terms(t -> t.field("http.method.keyword").size(10))
                                    )),
                    Void.class
            );

            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_method") != null) {
                response.aggregations().get("by_method").sterms().buckets().array()
                        .forEach(bucket -> counts.put(bucket.key().stringValue(), bucket.docCount()));
            }
            log.debug("HTTP method counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by HTTP method: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countByStatusCode(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by status code: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(s ->
                            s.index(indexPattern).size(0)   // 통계 결과(집계)만 필요할 때 사용
                            .query(timeRangeQuery).aggregations("by_status", Aggregation.of(a ->
                                    a.terms(t ->
                                    t.field("http.status_code").size(20))   // 상위 20개의 상태 코드만 가져오겠다.
                            )),
                    Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_status") != null) {
                response.aggregations().get("by_status").lterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                String.valueOf(bucket.key()),   // 상태 코드 값(예: 200)
                                bucket.docCount()));            // 상태 코드가 나타난 횟수
            }
            log.debug("Status code counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by status code: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Double getAverageResponseTime(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Getting average response time: index={}, start={}, end={}", indexPattern, start, end);
            Query timeResponseQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end); // 특정 시간 범위에 해당하는 데이터만 필터링
            SearchResponse<Void> response = elasticsearchClient.search(s ->
                            s.index(indexPattern).size(0).query(timeResponseQuery)
                                    .aggregations("avg_response_time", Aggregation.of(a ->
                                            a.avg(avg -> avg.field("http.response_time_ms")))),
                    Void.class
            );

            if (response.aggregations() != null && response.aggregations().get("avg_response_time") != null) {
                Double avgValue = response.aggregations().get("avg_response_time").avg().value();
                log.debug("Average response time: {} ms", avgValue);
                return avgValue != null ? avgValue : 0.0;
            }

            return 0.0;
        } catch (Exception e) {
            log.error("Failed to get average response time indexPattern={}", indexPattern, e);
            return 0.0;
        }
    }

    // 시간대별 분포 - Access Logs
    @Override
    public List<Map<String, Object>> getAccessLogDistributionByTime(
            String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying access log distribution: {} ~ {}, period={}", start, end, timePeriod);

            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            // ✅ epoch milliseconds로 변환
            long startEpochMs = start.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
            long endEpochMs = end.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("access_over_time", Aggregation.of(
                                    a -> a.dateHistogram(
                                            dh -> dh.field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .timeZone("Asia/Seoul")
                                                    .format("yyyy-MM-dd HH:mm:ss")
                                                    .minDocCount(0)
                                                    .extendedBounds(b -> b
                                                            .min(FieldDateMath.of(f -> f.value((double) startEpochMs)))
                                                            .max(FieldDateMath.of(f -> f.value((double) endEpochMs)))
                                                    )
                                    ).aggregations("avg_response_time", Aggregation.of(
                                            sub -> sub.avg(avg -> avg.field("http.response_time_ms")))
                                    ).aggregations("error_count", Aggregation.of(sub ->
                                            sub.filter(f -> f.range(
                                                    r -> r.field("http.status_code")
                                                            .gte(co.elastic.clients.json.JsonData.of(500)))
                                            )
                                    ))
                            )),
                    Void.class
            );

            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("access_over_time") != null) {
                response.aggregations().get("access_over_time").dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("requestCount", bucket.docCount());

                            Double avgResponseTime = bucket.aggregations().get("avg_response_time").avg().value();
                            entry.put("avgResponseTime", avgResponseTime != null ? avgResponseTime : 0.0);

                            Long errorCount = bucket.aggregations().get("error_count").filter().docCount();
                            entry.put("errorCount", errorCount);
                            distribution.add(entry);
                        });
            }
            log.info("Access log distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get access log distribution", e);
            return Collections.emptyList();
        }
    }

    // ============================================
    // 🔄 error-logs 통계용 메서드
    // ============================================
    @Override
    public Map<String, Long> countByErrorType(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by error type: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_error_type", Aggregation.of(
                                    // ✅ 수정: .keyword 제거 또는 둘 다 시도
                                    a -> a.terms(t -> t
                                            .field("error.type.keyword")  // 1순위: keyword 필드
                                            .size(20)
                                            .missing("UNKNOWN")           // null 처리
                                    )
                            )), Void.class
            );

            Map<String, Long> counts = new HashMap<>();

            // ✅ 응답이 비어있으면 .keyword 없이 재시도
            if (response.aggregations() != null &&
                    response.aggregations().get("by_error_type") != null &&
                    !response.aggregations().get("by_error_type").sterms().buckets().array().isEmpty()) {

                response.aggregations().get("by_error_type").sterms().buckets().array()
                        .forEach(bucket -> {
                            String key = bucket.key().stringValue();
                            long count = bucket.docCount();
                            log.info("✅ Error type: {}, count: {}", key, count);
                            counts.put(key, count);
                        });
            } else {
                // ✅ Fallback: .keyword 없이 재시도
                log.warn("⚠️ error.type.keyword not found, trying error.type");
                response = elasticsearchClient.search(
                        s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                                .aggregations("by_error_type", Aggregation.of(
                                        a -> a.terms(t -> t
                                                .field("error.type")  // keyword 없이
                                                .size(20)
                                                .missing("UNKNOWN")
                                        )
                                )), Void.class
                );

                if (response.aggregations() != null &&
                        response.aggregations().get("by_error_type") != null) {
                    response.aggregations().get("by_error_type").sterms().buckets().array()
                            .forEach(bucket -> counts.put(bucket.key().stringValue(), bucket.docCount()));
                }
            }

            log.info("📊 Final error type counts: {}", counts);
            return counts;

        } catch (Exception e) {
            log.error("❌ Failed to count by error type: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countBySeverity(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by severity: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            // ✅ 수정: severity 대신 log_level 사용 (Logstash가 표준화한 필드)
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern)
                            .size(0).query(timeRangeQuery)
                            .aggregations("by_severity", Aggregation.of(
                                    a -> a.terms(t -> t
                                            .field("log_level.keyword")  // ← 변경!
                                            .size(10)
                                            .missing("UNKNOWN")
                                    )
                            )), Void.class
            );

            Map<String, Long> counts = new HashMap<>();

            if (response.aggregations() != null &&
                    response.aggregations().get("by_severity") != null &&
                    !response.aggregations().get("by_severity").sterms().buckets().array().isEmpty()) {

                response.aggregations().get("by_severity").sterms().buckets().array()
                        .forEach(bucket -> {
                            String key = bucket.key().stringValue();
                            long count = bucket.docCount();
                            log.info("✅ Severity: {}, count: {}", key, count);
                            counts.put(key, count);
                        });
            } else {
                // ✅ Fallback: .keyword 없이 재시도
                log.warn("⚠️ log_level.keyword not found, trying log_level");
                response = elasticsearchClient.search(
                        s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                                .aggregations("by_severity", Aggregation.of(
                                        a -> a.terms(t -> t.field("log_level").size(10))
                                )), Void.class
                );

                if (response.aggregations() != null &&
                        response.aggregations().get("by_severity") != null) {
                    response.aggregations().get("by_severity").sterms().buckets().array()
                            .forEach(bucket -> counts.put(bucket.key().stringValue(), bucket.docCount()));
                }
            }

            log.info("📊 Final severity counts: {}", counts);
            return counts;

        } catch (Exception e) {
            log.error("❌ Failed to count by severity: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    // 시간대별 분포 - Error Logs
    @Override
    public List<Map<String, Object>> getErrorLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying error log distribution: {} ~ {}, period={}", start, end, timePeriod);
            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("errors_over_time", Aggregation.of(
                                    a -> a.dateHistogram(
                                            dh -> dh.field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .format("yyyy-MM-dd HH:mm:ss")
                                    ).aggregations("error_type_breakdown", Aggregation.of(
                                            // ✅ 수정: error.type -> error.type.keyword
                                            sub -> sub.terms(t -> t.field("error.type.keyword").size(5))
                                    ))
                            )),
                    Void.class
            );
            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("errors_over_time") != null) {
                response.aggregations().get("errors_over_time")
                        .dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("errorCount", bucket.docCount());

                            // 에러 타입별 분포
                            Map<String, Long> errorTypeBreakdown = new HashMap<>();
                            bucket.aggregations().get("error_type_breakdown")
                                    .sterms().buckets().array()
                                    .forEach(typeBucket -> errorTypeBreakdown.put(
                                            typeBucket.key().stringValue(),
                                            typeBucket.docCount()));
                            entry.put("errorTypeBreakdown", errorTypeBreakdown);
                            distribution.add(entry);
                        });
            }
            log.info("Error log distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get error log distribution: indexPattern={}", indexPattern, e);
            return Collections.emptyList();
        }
    }

    // ============================================
    // 🔄 performance-metrics 통계용 메서드
    // ============================================
    @Override
    public Map<String, Double> getSystemMetricsAggregation(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Getting system metrics aggregation: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("avg_cpu", Aggregation.of(
                                    a -> a.avg(avg -> avg.field("system.cpu_usage"))
                            ))
                            .aggregations("max_cpu", Aggregation.of(
                                    a -> a.max(max -> max.field("system.cpu_usage"))
                            ))
                            .aggregations("avg_memory", Aggregation.of(
                                    a -> a.avg(avg -> avg.field("system.memory_usage"))
                            ))
                            .aggregations("max_memory", Aggregation.of(
                                    a -> a.max(max -> max.field("system.memory_usage"))
                            ))
                            .aggregations("avg_disk", Aggregation.of(
                                    a -> a.avg(avg -> avg.field("system.disk_usage"))
                            )), Void.class
            );
            Map<String, Double> metrics = new HashMap<>();
            if (response.aggregations() != null) {
                metrics.put("avg_cpu", getAggregationValue(response, "avg_cpu"));
                metrics.put("max_cpu", getAggregationValue(response, "max_cpu"));
                metrics.put("avg_memory", getAggregationValue(response, "avg_memory"));
                metrics.put("max_memory", getAggregationValue(response, "max_memory"));
                metrics.put("avg_disk", getAggregationValue(response, "avg_disk"));
            }
            log.debug("System metrics: {}", metrics);
            return metrics;
        } catch (Exception e) {
            log.error("Failed to get system metrics aggregation: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Double> getJvmMetricsAggregation(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Getting JVM metrics aggregation: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
        s -> s.index(indexPattern).size(0).query(timeRangeQuery).aggregations("avg_heap", Aggregation.of(
                a -> a.avg(avg -> avg.field("jvm.heap_used")))).aggregations("max_heap", Aggregation.of(
                    a -> a.max(max -> max.field("jvm.heap_used")))).aggregations("total_gc_count", Aggregation.of(
                        a -> a.sum(sum -> sum.field("jvm.gc_count")))).aggregations("total_gc_time", Aggregation.of(
                            a -> a.sum(sum -> sum.field("jvm.gc_time")))).aggregations("avg_thread_count", Aggregation.of(
                                a -> a.avg(avg -> avg.field("jvm.thread_count")))),
                    Void.class
            );
            Map<String, Double> metrics = new HashMap<>();
            if (response.aggregations() != null) {
                metrics.put("avg_heap", getAggregationValue(response, "avg_heap"));
                metrics.put("max_heap", getAggregationValue(response, "max_heap"));
                metrics.put("total_gc_count", getAggregationValue(response, "total_gc_count"));
                metrics.put("total_gc_time", getAggregationValue(response, "total_gc_time"));
                metrics.put("avg_thread_count", getAggregationValue(response, "avg_thread_count"));
            }
            log.debug("JVM metrics: {}", metrics);
            return metrics;
        } catch (Exception e) {
            log.error("Failed to get JVM metrics aggregation: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    // 시간대별 분포 - Performance Metrics
    @Override
    public List<Map<String, Object>> getPerformanceMetricsDistributionByTime(
            String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying performance metrics distribution: {} ~ {}, period={}", start, end, timePeriod);

            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            // ✅ epoch milliseconds로 변환
            long startEpochMs = start.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
            long endEpochMs = end.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("metrics_over_time", Aggregation.of(
                                    a -> a.dateHistogram(
                                            dh -> dh.field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .timeZone("Asia/Seoul")
                                                    .format("yyyy-MM-dd HH:mm:ss")
                                                    .minDocCount(0)
                                                    .extendedBounds(b -> b
                                                            .min(FieldDateMath.of(f -> f.value((double) startEpochMs)))
                                                            .max(FieldDateMath.of(f -> f.value((double) endEpochMs)))
                                                    )
                                    ).aggregations("avg_cpu_usage", Aggregation.of(
                                            sub -> sub.avg(avg -> avg.field("system.cpu_usage")))
                                    ).aggregations("avg_memory_usage", Aggregation.of(
                                            sub -> sub.avg(avg -> avg.field("system.memory_usage")))
                                    ).aggregations("avg_heap_usage", Aggregation.of(
                                            sub -> sub.avg(avg -> avg.field("jvm.heap_used"))))
                            )),
                    Void.class
            );

            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("metrics_over_time") != null) {
                response.aggregations().get("metrics_over_time").dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("cpuUsage", getBucketAggregationValue(bucket, "avg_cpu_usage"));
                            entry.put("memoryUsage", getBucketAggregationValue(bucket, "avg_memory_usage"));
                            entry.put("heapUsage", getBucketAggregationValue(bucket, "avg_heap_usage"));
                            distribution.add(entry);
                        });
            }
            log.info("Performance metrics distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get performance metrics distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> countByOperation(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by operation: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_operation", Aggregation.of(
                                    // ✅ 수정: operation -> operation.keyword
                                    a -> a.terms(t -> t.field("operation.keyword").size(10))
                            )),
                    Void.class
            );

            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_operation") != null) {
                response.aggregations().get("by_operation").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
            }
            log.debug("Operation counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by operation: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countByTable(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by table: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_table", Aggregation.of(
                                    // ✅ 수정: table -> table.keyword
                                    a -> a.terms(t -> t.field("table.keyword").size(20))
                            )),
                    Void.class
            );

            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_table") != null) {
                response.aggregations().get("by_table").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount())
                        );
            }
            log.debug("Table counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by table: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Object> getQueryPerformanceStats(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Getting query performance stats: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("avg_duration", Aggregation.of(
                                    a -> a.avg(avg -> avg.field("query.duration_ms")) // 필드명 확인 필요 (아래 설명 참조)
                            ))
                            .aggregations("max_duration", Aggregation.of(a -> a
                                    .max(max -> max.field("query.duration_ms"))
                            ))
                            .aggregations("slow_queries", Aggregation.of(a -> a
                                    .filter(f -> f
                                            .range(r -> r
                                                    .field("query.duration_ms")
                                                    .gte(co.elastic.clients.json.JsonData.of(1000))
                                            )))), Void.class
            );
            Map<String, Object> stats = new HashMap<>();
            if (response.aggregations() != null) {
                // 1. 평균값 가져오기
                Aggregate avgAggr = response.aggregations().get("avg_duration");
                double avg = (avgAggr != null && avgAggr.isAvg()) ? avgAggr.avg().value() : 0.0;

                // 2. 최대값 가져오기
                Aggregate maxAggr = response.aggregations().get("max_duration");
                double max = (maxAggr != null && maxAggr.isMax()) ? maxAggr.max().value() : 0.0;

                // 만약 값이 무한대(Infinity)나 NaN이면 0으로 보정 (안전장치)
                if (!Double.isFinite(avg)) avg = 0.0;
                if (!Double.isFinite(max)) max = 0.0;

                stats.put("avgDuration", avg);
                stats.put("maxDuration", max);

                // 3. 슬로우 쿼리 개수 (여기가 에러 발생 지점)
                Aggregate slowAggr = response.aggregations().get("slow_queries");
                long slowCount = 0;
                if (slowAggr != null && slowAggr.isFilter()) {
                    slowCount = slowAggr.filter().docCount();
                }
                stats.put("slowQueryCount", slowCount);

                // 4. 전체 쿼리 수
                long totalCount = response.hits().total() != null ? response.hits().total().value() : 0;
                stats.put("totalQueryCount", totalCount);
            } else {
                // 응답이 비어있을 경우 기본값
                stats.put("avgDuration", 0.0);
                stats.put("maxDuration", 0.0);
                stats.put("slowQueryCount", 0L);
                stats.put("totalQueryCount", 0L);
            }

            log.debug("Query performance stats: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("Failed to get query performance stats: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    // 시간대별 분포 - Database Logs
    @Override
    public List<Map<String, Object>> getDatabaseLogDistributionByTime(
            String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying database log distribution: {} ~ {}, period={}", start, end, timePeriod);

            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            // ✅ epoch milliseconds로 변환
            long startEpochMs = start.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
            long endEpochMs = end.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("db_logs_over_time", Aggregation.of(
                                    a -> a.dateHistogram(
                                            dh -> dh.field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .timeZone("Asia/Seoul")
                                                    .format("yyyy-MM-dd HH:mm:ss")
                                                    .minDocCount(0)
                                                    .extendedBounds(b -> b
                                                            .min(FieldDateMath.of(f -> f.value((double) startEpochMs)))
                                                            .max(FieldDateMath.of(f -> f.value((double) endEpochMs)))
                                                    )
                                    ).aggregations("avg_duration", Aggregation.of(
                                            sub -> sub.avg(avg -> avg.field("query.duration_ms")))
                                    ).aggregations("slow_query_count", Aggregation.of(
                                            sub -> sub.filter(f -> f.range(
                                                    r -> r.field("query.duration_ms")
                                                            .gte(co.elastic.clients.json.JsonData.of(1000))))))
                            )),
                    Void.class
            );

            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("db_logs_over_time") != null) {
                response.aggregations().get("db_logs_over_time")
                        .dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("queryCount", bucket.docCount());
                            entry.put("avgDuration", getBucketAggregationValue(bucket, "avg_duration"));
                            entry.put("slowQueryCount", bucket.aggregations().get("slow_query_count").filter().docCount());
                            distribution.add(entry);
                        });
            }
            log.info("Database log distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get database log distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> countByEventAction(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by event action: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_action", Aggregation.of(
                                    // ✅ 수정: event.action.keyword (nested 구조)
                                    a -> a.terms(t -> t.field("event.action.keyword").size(20))
                            )),
                    Void.class
            );

            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_action") != null) {
                response.aggregations().get("by_action").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
            }
            log.debug("Event action counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to get event action", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countByCategory(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by category: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_category", a -> a.terms(
                                    // ✅ 수정: event.category.keyword
                                    t -> t.field("event.category.keyword").size(10)
                            )),
                    Void.class
            );

            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_category") != null) {
                List<StringTermsBucket> buckets = response.aggregations().get("by_category").sterms().buckets().array();
                for (StringTermsBucket bucket : buckets) {
                    counts.put(bucket.key().stringValue(), bucket.docCount());
                }
            }
            log.debug("Category counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by category", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countByEventResult(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by event result: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_result", Aggregation.of(
                                    // ✅ 수정: event.result.keyword
                                    a -> a.terms(t -> t.field("event.result.keyword").size(5))
                            )),
                    Void.class
            );

            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_result") != null) {
                response.aggregations().get("by_result").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
            }
            log.debug("Event result counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by event result", e);
            return Collections.emptyMap();
        }
    }

    // 시간대별 분포 - Audit Logs
    @Override
    public List<Map<String, Object>> getAuditLogDistributionByTime(
            String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying audit log distribution: {} ~ {}, period={}", start, end, timePeriod);

            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            // ✅ epoch milliseconds로 변환
            long startEpochMs = start.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
            long endEpochMs = end.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("audit_over_time", Aggregation.of(
                                    a -> a.dateHistogram(
                                            dh -> dh.field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .timeZone("Asia/Seoul")
                                                    .format("yyyy-MM-dd HH:mm:ss")
                                                    .minDocCount(0)
                                                    .extendedBounds(b -> b
                                                            .min(FieldDateMath.of(f -> f.value((double) startEpochMs)))
                                                            .max(FieldDateMath.of(f -> f.value((double) endEpochMs)))
                                                    )
                                    ).aggregations("success_count", Aggregation.of(
                                            sub -> sub.filter(f -> f.term(t -> t.field("event.result.keyword").value("success"))))
                                    ).aggregations("failure_count", Aggregation.of(
                                            sub -> sub.filter(f -> f.term(t -> t.field("event.result.keyword").value("failure")))))
                            )),
                    Void.class
            );

            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("audit_over_time") != null) {
                response.aggregations().get("audit_over_time")
                        .dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("totalEvents", bucket.docCount());

                            Long successCount = bucket.aggregations().get("success_count").filter().docCount();
                            Long failureCount = bucket.aggregations().get("failure_count").filter().docCount();
                            entry.put("successEvents", successCount);
                            entry.put("failureEvents", failureCount);
                            distribution.add(entry);
                        });
            }
            log.info("Audit log distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get audit log distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> countByThreatLevel(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by threat level: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_threat_level", Aggregation.of(
                                    // ✅ 수정: security.threat_level.keyword
                                    a -> a.terms(t -> t
                                            .field("security.threat_level.keyword")
                                            .size(10)
                                            .missing("unknown")
                                    )
                            )), Void.class
            );

            Map<String, Long> counts = new HashMap<>();

            if (response.aggregations() != null &&
                    response.aggregations().get("by_threat_level") != null &&
                    !response.aggregations().get("by_threat_level").sterms().buckets().array().isEmpty()) {

                response.aggregations().get("by_threat_level").sterms().buckets().array()
                        .forEach(bucket -> {
                            String key = bucket.key().stringValue();
                            long count = bucket.docCount();
                            log.info("✅ Threat level: {}, count: {}", key, count);
                            counts.put(key, count);
                        });
            } else {
                // ✅ Fallback: .keyword 없이 재시도
                log.warn("⚠️ security.threat_level.keyword not found, trying without .keyword");
                response = elasticsearchClient.search(
                        s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                                .aggregations("by_threat_level", Aggregation.of(
                                        a -> a.terms(t -> t
                                                .field("security.threat_level")
                                                .size(10)
                                                .missing("unknown")
                                        )
                                )), Void.class
                );

                if (response.aggregations() != null &&
                        response.aggregations().get("by_threat_level") != null) {
                    response.aggregations().get("by_threat_level").sterms().buckets().array()
                            .forEach(bucket -> counts.put(bucket.key().stringValue(), bucket.docCount()));
                }
            }

            log.info("📊 Final threat level counts: {}", counts);
            return counts;

        } catch (Exception e) {
            log.error("❌ Failed to count by threat level: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countByAttackType(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by attack type: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("by_attack_type", Aggregation.of(
                                    // ✅ 수정: attack.type.keyword → security.attack_type.keyword
                                    a -> a.terms(t -> t
                                            .field("security.attack_type.keyword")  // ← 변경!
                                            .size(20)
                                            .missing("unknown")
                                    )
                            )), Void.class
            );

            Map<String, Long> counts = new HashMap<>();

            if (response.aggregations() != null &&
                    response.aggregations().get("by_attack_type") != null &&
                    !response.aggregations().get("by_attack_type").sterms().buckets().array().isEmpty()) {

                response.aggregations().get("by_attack_type").sterms().buckets().array()
                        .forEach(bucket -> {
                            String key = bucket.key().stringValue();
                            long count = bucket.docCount();
                            log.info("✅ Attack type: {}, count: {}", key, count);
                            counts.put(key, count);
                        });
            } else {
                // ✅ Fallback: .keyword 없이 재시도
                log.warn("⚠️ security.attack_type.keyword not found, trying without .keyword");
                response = elasticsearchClient.search(
                        s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                                .aggregations("by_attack_type", Aggregation.of(
                                        a -> a.terms(t -> t
                                                .field("security.attack_type")
                                                .size(20)
                                                .missing("unknown")
                                        )
                                )), Void.class
                );

                if (response.aggregations() != null &&
                        response.aggregations().get("by_attack_type") != null) {
                    response.aggregations().get("by_attack_type").sterms().buckets().array()
                            .forEach(bucket -> counts.put(bucket.key().stringValue(), bucket.docCount()));
                }
            }

            log.info("📊 Final attack type counts: {}", counts);
            return counts;

        } catch (Exception e) {
            log.error("❌ Failed to count by attack type: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> getBlockStatistics(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Getting block statistics: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            // ✅ 방법 1: boolean true로 검색 (데이터가 boolean일 때)
                            .aggregations("blocked_attacks_bool", Aggregation.of(
                                    a -> a.filter(f -> f.term(t -> t.field("blocked").value(true)))
                            ))
                            // ✅ 방법 2: String "true"로 검색 (데이터가 문자열일 때)
                            .aggregations("blocked_attacks_string", Aggregation.of(
                                    a -> a.filter(f -> f.term(t -> t.field("blocked.keyword").value("true")))
                            ))
                            // ✅ 방법 3: String "false"로 검색
                            .aggregations("allowed_attacks_string", Aggregation.of(
                                    a -> a.filter(f -> f.term(t -> t.field("blocked.keyword").value("false")))
                            ))
                            // ✅ 방법 4: boolean false로 검색
                            .aggregations("allowed_attacks_bool", Aggregation.of(
                                    a -> a.filter(f -> f.term(t -> t.field("blocked").value(false)))
                            )),
                    Void.class
            );

            Map<String, Long> stats = new HashMap<>();

            // 🛡️ [수정] NPE 방지: aggregations() 자체가 null이거나, 각 항목이 null인지 체크
            if (response.aggregations() != null) {
                long totalAttacks = response.hits().total() != null ? response.hits().total().value() : 0;

                // Helper 메서드나 삼항 연산자로 안전하게 추출
                long blockedBool = getDocCount(response.aggregations().get("blocked_attacks_bool"));
                long blockedString = getDocCount(response.aggregations().get("blocked_attacks_string"));
                long allowedBool = getDocCount(response.aggregations().get("allowed_attacks_bool"));
                long allowedString = getDocCount(response.aggregations().get("allowed_attacks_string"));

                long blockedAttacks = Math.max(blockedBool, blockedString);
                long allowedAttacks = Math.max(allowedBool, allowedString);

                log.info("📊 Block stats - Total: {}, Blocked: {}, Allowed: {}", totalAttacks, blockedAttacks, allowedAttacks);

                stats.put("totalAttacks", totalAttacks);
                stats.put("blockedAttacks", blockedAttacks);
                stats.put("allowedAttacks", allowedAttacks);
            } else {
                stats.put("totalAttacks", 0L);
                stats.put("blockedAttacks", 0L);
                stats.put("allowedAttacks", 0L);
            }

            return stats;

        } catch (Exception e) {
            log.error("❌ Failed to get block statistics: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    // 💡 안전하게 docCount를 꺼내는 헬퍼 메서드 (클래스 내부에 추가하세요)
    private long getDocCount(Aggregate aggregate) {
        if (aggregate != null && aggregate.isFilter()) {
            return aggregate.filter().docCount();
        }
        return 0L;
    }

    // 시간대별 분포 - Security Logs
    @Override
    public List<Map<String, Object>> getSecurityLogDistributionByTime(
            String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying security log distribution: {} ~ {}, period={}", start, end, timePeriod);

            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            long startEpochMs = start.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
            long endEpochMs = end.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("security_over_time", Aggregation.of(
                                    a -> a.dateHistogram(
                                            dh -> dh.field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .timeZone("Asia/Seoul")
                                                    .format("yyyy-MM-dd HH:mm:ss")
                                                    .minDocCount(0)
                                                    .extendedBounds(b -> b
                                                            .min(FieldDateMath.of(f -> f.value((double) startEpochMs)))
                                                            .max(FieldDateMath.of(f -> f.value((double) endEpochMs)))
                                                    )
                                    ).aggregations("blocked_count_bool", Aggregation.of(
                                            sub -> sub.filter(f -> f.term(t -> t.field("blocked").value(true)))
                                    )).aggregations("blocked_count_string", Aggregation.of(
                                            sub -> sub.filter(f -> f.term(t -> t.field("blocked.keyword").value("true")))
                                    )).aggregations("threat_level_breakdown", Aggregation.of(
                                            sub -> sub.terms(t -> t.field("security.threat_level.keyword").size(5))
                                    ))
                            )),
                    Void.class
            );

            List<Map<String, Object>> distribution = new ArrayList<>();

            // ✅ null 체크 개선
            if (response.aggregations() == null || response.aggregations().get("security_over_time") == null) {
                log.warn("No aggregations found in response");
                return distribution;
            }

            // ✅ 타입을 명시적으로 처리
            var securityOverTimeAgg = response.aggregations().get("security_over_time");
            if (securityOverTimeAgg == null || !securityOverTimeAgg.isDateHistogram()) {
                log.warn("security_over_time aggregation is not a date histogram");
                return distribution;
            }

            securityOverTimeAgg.dateHistogram().buckets().array()
                    .forEach(bucket -> {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("timestamp", bucket.keyAsString());
                        entry.put("attackCount", bucket.docCount());

                        // ✅ aggregations null 체크 추가
                        if (bucket.aggregations() != null) {
                            // boolean과 String 중 큰 값 사용
                            Long blockedBool = 0L;
                            Long blockedString = 0L;

                            var blockedBoolAgg = bucket.aggregations().get("blocked_count_bool");
                            if (blockedBoolAgg != null && blockedBoolAgg.isFilter()) {
                                blockedBool = blockedBoolAgg.filter().docCount();
                            }

                            var blockedStringAgg = bucket.aggregations().get("blocked_count_string");
                            if (blockedStringAgg != null && blockedStringAgg.isFilter()) {
                                blockedString = blockedStringAgg.filter().docCount();
                            }

                            Long blockedCount = Math.max(blockedBool, blockedString);
                            entry.put("blockedCount", blockedCount);

                            // threat_level_breakdown 처리
                            Map<String, Long> threatLevelBreakdown = new HashMap<>();
                            var threatLevelAgg = bucket.aggregations().get("threat_level_breakdown");

                            if (threatLevelAgg != null && threatLevelAgg.isSterms()) {
                                threatLevelAgg.sterms().buckets().array()
                                        .forEach(threatBucket ->
                                                threatLevelBreakdown.put(
                                                        threatBucket.key().stringValue(),
                                                        threatBucket.docCount()
                                                )
                                        );
                            }

                            entry.put("threatLevelBreakdown", threatLevelBreakdown);
                        } else {
                            // aggregations가 없는 경우 기본값
                            entry.put("blockedCount", 0L);
                            entry.put("threatLevelBreakdown", new HashMap<String, Long>());
                        }

                        distribution.add(entry);
                    });

            log.info("Security log distribution result: {} time buckets", distribution.size());
            return distribution;

        } catch (Exception e) {
            log.error("Failed to get security log distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public PageResponseDTO<Map<String, Object>> searchErrorLogs(String type, int page, int size) {
        String indexName;

        if ("SYSTEM".equalsIgnoreCase(type)) {
            indexName = "error-logs-*";
        } else {
            indexName = "application-logs-*";
        }

        int currentPage = Math.max(1, page);
        int from = (currentPage - 1) * size;

        try {
            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .from(from)
                            .size(size)
                            .query(q -> {
                                if ("SYSTEM".equalsIgnoreCase(type)) {
                                    return q.matchAll(m -> m);
                                } else {
                                    // ERROR, CRITICAL, FATAL 모두 조회
                                    return q.terms(t -> t
                                            .field("log_level.keyword")
                                            .terms(v -> v.value(List.of(
                                                    FieldValue.of("ERROR"),
                                                    FieldValue.of("CRITICAL"),
                                                    FieldValue.of("FATAL")
                                            )))
                                    );
                                }
                            })
                            .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc))),
                    Map.class
            );

            // ✅ [수정됨] 로직 간소화
            // convertHitToMap 내부에서 이미 determineLogLevel을 통해
            // 500=ERROR, 503=CRITICAL 로직을 수행했으므로, 여기서는 변환만 하면 됩니다.
            List<Map<String, Object>> content = response.hits().hits().stream()
                    .map(this::convertHitToMap)
                    .collect(Collectors.toList());

            long totalElements = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = (int) Math.ceil((double) totalElements / size);

            return PageResponseDTO.<Map<String, Object>>builder()
                    .content(content)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .currentPage(currentPage)
                    .size(size)
                    .build();

        } catch (Exception e) {
            log.error("Error searching logs type={}", type, e);
            return PageResponseDTO.<Map<String, Object>>builder()
                    .content(Collections.emptyList())
                    .build();
        }
    }

    // [신규 추가] DashboardService에서 가져온 레벨 판단 로직
    private String resolveLogLevel(Map<String, Object> logMap) {
        // 1. MDC 확인 (가장 확실한 방법)
        if (logMap.containsKey("mdc")) {
            Object mdcObj = logMap.get("mdc");
            if (mdcObj instanceof Map) {
                Map<String, Object> mdc = (Map<String, Object>) mdcObj;
                // MDC 내부에 severity나 log_level 키가 CRITICAL이면 격상
                if ("CRITICAL".equalsIgnoreCase((String) mdc.get("severity")) ||
                        "CRITICAL".equalsIgnoreCase((String) mdc.get("log_level"))) {
                    return "CRITICAL";
                }
            }
        }

        // 2. 메시지 텍스트 분석 (임시 방편이자 강력한 강제 수단)
        String message = (String) logMap.getOrDefault("message", "");
        if (message != null && (message.contains("Critical") || message.contains("🚨"))) {
            return "CRITICAL";
        }

        // 3. 위 조건에 안 걸리면 원래 DB에 있던 레벨 반환 (없으면 ERROR)
        String originalLevel = (String) logMap.getOrDefault("logLevel",
                (String) logMap.getOrDefault("level", "ERROR"));

        return originalLevel;
    }

    // timePeriod → Elasticsearch interval 변환
    private String calculateInterval(String timePeriod) {
        return switch (timePeriod.toUpperCase()) {
            case "MINUTE" -> "1m";
            case "HOUR" -> "1h";
            case "DAY" -> "1d";
            case "WEEK" -> "7d";
            case "MONTH" -> "30d";
            default -> "1h";
        };
    }

    /**
     * Elasticsearch Hit을 Map으로 변환
     *
     * @param hit Hit 객체
     * @return Map
     */
    private Map<String, Object> convertHitToMap(Hit<Map> hit) {
        Map<String, Object> result = new HashMap<>();

        // 문서 ID 및 인덱스 추가
        result.put("_id", hit.id());
        result.put("_index", hit.index());

        // 소스 데이터
        Map<String, Object> source = hit.source();
        if (source == null) {
            return result;
        }

        // 공통 필드: @timestamp, application
        result.put("@timestamp", source.get("@timestamp"));
        result.put("application", source.get("application"));

        // 원본의 MDC가 있다면 복사 (Keyword 체크 등을 위해)
        if (source.containsKey("mdc")) {
            result.put("mdc", source.get("mdc"));
        }

        // ✅ 인덱스 타입별 필드 매핑
        String indexName = hit.index();

        // -------------------------------------------------------
        // 인덱스별 매핑 로직 (기존 코드와 동일하되 필요한 부분만 정리)
        // -------------------------------------------------------
        if (indexName.startsWith("application-logs")) {
            result.putAll(source);

        } else if (indexName.startsWith("access-logs")) {
            Map<String, Object> http = (Map<String, Object>) source.get("http");
            if (http != null) {
                result.put("http", http);
                // 메시지 필드가 없으면 생성
                String msg = String.format("%s %s - Status: %s",
                        http.get("method"), http.get("url"), http.get("status_code"));
                result.put("message", msg);
            }
            if (source.containsKey("client")) result.put("client", source.get("client"));

        } else if (indexName.startsWith("error-logs")) {
            Map<String, Object> error = (Map<String, Object>) source.get("error");
            if (error != null) {
                // severity를 log_level 후보로 저장
                result.put("log_level", error.get("severity"));
                result.put("logger_name", "ErrorLog");
                result.put("message", error.get("type") + ": " + error.get("message"));
                result.put("stack_trace", error.get("stack_trace"));
                result.put("error", error);
            }
            if (source.containsKey("source")) result.put("source", source.get("source"));

        } else if (indexName.startsWith("performance-metrics")) {
            // performance-metrics: 성능 메트릭 (수정됨)
            result.put("log_level", "INFO");

            // 1. 메서드 실행 시간 로그인지 확인 (class, method, execution_time_ms 필드 존재 여부)
            if (source.containsKey("method") && source.containsKey("execution_time_ms")) {
                // Logger Name: 클래스 이름 사용 (없으면 기본값)
                Object className = source.get("class");
                result.put("logger_name", className != null ? className : "PerformanceLog");

                // Message: "Method Execution: checkLoginId - 4208ms" 형태로 가공
                String message = String.format("Method Execution: %s - %sms",
                        source.get("method"),
                        source.get("execution_time_ms")
                );
                result.put("message", message);

                // 상세 데이터 원본도 포함 (프론트엔드 정렬/필터링용)
                result.put("class", className);
                result.put("method", source.get("method"));
                result.put("execution_time_ms", source.get("execution_time_ms"));

            } else {
                // 2. 시스템/JVM 메트릭 (기존 로직 - 혹시 시스템 로그가 들어올 경우를 대비해 유지)
                result.put("logger_name", "SystemMetrics");

                Map<String, Object> system = (Map<String, Object>) source.get("system");
                Map<String, Object> jvm = (Map<String, Object>) source.get("jvm");

                StringBuilder sb = new StringBuilder("System Metrics");
                if (system != null) {
                    sb.append(String.format(" - CPU: %s%%", system.get("cpu_usage")));
                    result.put("system", system);
                }
                if (jvm != null) {
                    result.put("jvm", jvm);
                }

                // 원본 메시지가 "Performance Data" 처럼 단순하면 상세 정보를, 아니면 원본 메시지를 사용
                String originalMsg = (String) source.get("message");
                if (originalMsg != null && !originalMsg.equals("Performance Data")) {
                    result.put("message", originalMsg);
                } else {
                    result.put("message", sb.toString());
                }
            }

        } else if (indexName.startsWith("database-logs")) {
            // database-logs: 데이터베이스 로그

            // 1. 기존처럼 구조화된 쿼리 객체가 있는지 확인
            Map<String, Object> query = (Map<String, Object>) source.get("query");

            if (query != null) {
                // [Case A] 구조화된 로그가 들어온 경우 (기존 로직 유지)
                result.put("log_level", "INFO");
                result.put("logger_name", "DatabaseLog");

                String message = String.format("%s - %s (Duration: %sms)",
                        source.get("operation"),
                        source.get("table"),
                        query.get("duration_ms")
                );
                result.put("message", message);
                result.put("query", query);
                result.put("stack_trace", query.get("sql"));
                result.put("operation", source.get("operation"));
                result.put("table", source.get("table"));

            } else {
                // ✅ [Case B] 우리가 만든 Interceptor 로그 (일반 텍스트 메시지) 처리
                // 구조화된 'query' 객체가 없다면, 원본 'message' 필드를 그대로 가져옵니다.

                // 로그 레벨 가져오기 (없으면 INFO)
                Object logLevel = source.get("log_level");
                result.put("log_level", logLevel != null ? logLevel : "INFO");

                // 로거 이름 가져오기 (없으면 DatabaseLog)
                Object loggerName = source.get("logger_name");
                result.put("logger_name", loggerName != null ? loggerName : "DatabaseLog");

                // ★ 핵심: Interceptor가 만든 "SQL: [...]" 문자열을 그대로 전달
                result.put("message", source.get("message"));

                // 스택 트레이스 정보가 있다면 추가
                if (source.containsKey("stack_trace")) {
                    result.put("stack_trace", source.get("stack_trace"));
                }
            }

        } else if (indexName.startsWith("audit-logs")) {
            // audit-logs: 감사 로그 (수정됨)

            // 1. 기본 설정 (데이터에 레벨이 없으므로 INFO로 고정)
            result.put("log_level", "INFO");
            result.put("logger_name", "AuditLog");

            // 2. 데이터 추출
            Map<String, Object> user = (Map<String, Object>) source.get("user");
            Map<String, Object> resource = (Map<String, Object>) source.get("resource");
            String originalMessage = (String) source.get("message");

            // 3. 메시지 재구성 (누가, 무엇을 했는지 명확하게 표시)
            // 예: "User registration completed by test001 (Resource: 테스터)"
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(originalMessage != null ? originalMessage : "Audit Event");

            if (user != null && user.get("login_id") != null) {
                messageBuilder.append(" by ").append(user.get("login_id"));
            }

            if (resource != null && resource.get("name") != null) {
                messageBuilder.append(" (Resource: ").append(resource.get("name")).append(")");
            }

            result.put("message", messageBuilder.toString());

            // 4. 상세 정보 담기 (프론트엔드 상세 모달용)
            if (user != null) {
                result.put("user", user);
            }
            if (resource != null) {
                result.put("resource", resource);
            }

            // 기존 'event' 객체가 있다면 같이 넣어줌 (하위 호환성)
            if (source.containsKey("event")) {
                result.put("event", source.get("event"));
            }

        } else if (indexName.startsWith("security-logs")) {
            // =================================================
            // [SEC] 보안 로그 (지능형 분석 적용)
            // =================================================

            // 1. 구조화된 보안 이벤트 객체(security, attack)가 있는지 확인 (WAF 등 연동 시)
            Map<String, Object> security = (Map<String, Object>) source.get("security");
            Map<String, Object> attack = (Map<String, Object>) source.get("attack");

            if (security != null && attack != null) {
                // [Case A] 구조화된 위협 로그 처리
                String threatLevel = (String) security.get("threat_level");

                // Threat Level -> Log Level 매핑
                String logLevel;
                switch (threatLevel != null ? threatLevel.toLowerCase() : "low") {
                    case "critical": logLevel = "FATAL"; break;
                    case "high":     logLevel = "ERROR"; break;
                    case "medium":   logLevel = "WARN";  break;
                    default:         logLevel = "INFO";  break;
                }

                result.put("log_level", logLevel);
                result.put("logger_name", "SecurityEvent");
                result.put("message", String.format("[%s] Security Alert: %s (Blocked: %s)",
                        threatLevel, attack.get("type"), source.get("blocked")));

                result.put("security", security);
                result.put("attack", attack);

            } else {
                // [Case B] 일반 Spring Security 텍스트 로그 분석
                String rawMessage = (String) source.get("message");
                Object originalLevelObj = source.get("level");
                String level = originalLevelObj != null ? originalLevelObj.toString() : "INFO";

                // 로거 이름 정리 (패키지명 단축)
                String loggerName = "SecurityLog";
                if (source.get("logger") != null) {
                    String fullLogger = source.get("logger").toString();
                    loggerName = fullLogger.contains(".")
                            ? fullLogger.substring(fullLogger.lastIndexOf(".") + 1)
                            : fullLogger;
                }

                // --- 🔍 메시지 분석 및 레벨/유형 재정의 ---
                String securityType = "General Event";

                if (rawMessage != null) {
                    // 1. 로그인 실패
                    if (rawMessage.contains("Bad credentials") ||
                            rawMessage.contains("password does not match") ||
                            rawMessage.contains("User not found") ||
                            rawMessage.contains("Authentication failed")) {

                        securityType = "Login Failure";
                        level = "WARN"; // 격상

                        // 2. 권한 없음 (해킹 시도 의심)
                    } else if (rawMessage.contains("Access is denied") ||
                            rawMessage.contains("AccessDeniedException") ||
                            rawMessage.contains("AnonymouseAuthenticationToken")) {

                        securityType = "Access Denied";
                        level = "ERROR"; // 격상

                        // 3. CSRF 공격
                    } else if (rawMessage.contains("Invalid CSRF") ||
                            rawMessage.contains("Missing CSRF")) {

                        securityType = "CSRF Warning";
                        level = "ERROR"; // 격상

                        // 4. 세션 만료
                    } else if (rawMessage.contains("Session") && rawMessage.contains("expired")) {

                        securityType = "Session Expired";
                        level = "WARN";
                    }
                }
                // ---------------------------------------

                result.put("log_level", level);
                result.put("logger_name", loggerName);
                result.put("message", rawMessage);
                result.put("security_type", securityType); // 프론트엔드 표시용 유형

                if (source.containsKey("tags")) {
                    result.put("tags", source.get("tags"));
                }
            }

        } else {
            result.putAll(source);
        }

        // 모든 매핑이 끝난 후, 최종적으로 레벨을 '판결'합니다.
        String realLevel = determineLogLevel(result);

        // 판결된 레벨을 모든 관련 필드에 덮어씁니다.
        result.put("log_level", realLevel);
        result.put("logLevel", realLevel);
        result.put("level", realLevel);

        // 로거 이름이 비어있다면 인덱스 기반으로 기본값 설정 (옵션)
        if (!result.containsKey("logger_name")) {
            if (indexName.startsWith("access")) result.put("logger_name", "AccessLog");
            else if (indexName.startsWith("security")) result.put("logger_name", "SecurityLog");
            else result.put("logger_name", "SystemLog");
        }

        return result;
    }

    /**
     * 로그 레벨 최종 판정 (Smart Logic 적용)
     * - 500(코드에러) vs 503/504(서버장애) 구분
     * - 치명적인 에러 키워드 감지
     */
    private String determineLogLevel(Map<String, Object> doc) {
        String index = (String) doc.get("_index");
        String message = (String) doc.getOrDefault("message", "");

        // 메시지가 null일 경우 방어 로직
        if (message == null) message = "";

        // ==========================================
        // 1. [Access Logs] HTTP 상태 코드 정밀 분석
        // ==========================================
        if (index != null && index.startsWith("access-logs")) {
            int status = extractHttpStatusCode(doc); // (기존에 존재하는 헬퍼 메서드 활용)

            // [CRITICAL] 인프라 장애 / 서비스 불능
            // 503: Service Unavailable (서버 과부하, 배포 중)
            // 504: Gateway Timeout (DB나 백엔드 응답 없음)
            if (status == 503 || status == 504) {
                return "CRITICAL";
            }

            // [ERROR] 백엔드 코드 버그 / 내부 에러
            // 500: Internal Server Error (NPE, 로직 오류)
            // 502: Bad Gateway
            if (status >= 500) {
                return "ERROR";
            }

            // [WARN] 클라이언트 과실
            if (status >= 400) {
                return "WARN";
            }

            return "INFO";
        }

        // ==========================================
        // 2. [All Logs] 치명적인 키워드 검사 (강제 승격)
        // ==========================================
        // 로그 레벨이 뭐든 간에, 이 단어들이 보이면 무조건 CRITICAL로 간주합니다.
        if (message.contains("OutOfMemory") ||
                message.contains("StackOverflow") ||
                message.contains("Deadlock") ||
                message.contains("Connection refused") ||
                message.contains("Fatal") ||
                message.contains("CRITICAL") ||  // 대소문자 구분 없이 체크하려면 toUpperCase() 사용 권장
                message.contains("🚨")) {
            return "CRITICAL";
        }

        // ==========================================
        // 3. [Security Logs] 위협 수준 기반
        // ==========================================
        if (index != null && index.startsWith("security-logs")) {
            String threatLevel = extractThreatLevel(doc);
            if (threatLevel != null) {
                return switch (threatLevel.toLowerCase()) {
                    case "critical" -> "CRITICAL";
                    case "high" -> "ERROR";
                    case "medium" -> "WARN";
                    default -> "INFO";
                };
            }
        }

        // ==========================================
        // 4. [Audit Logs] 실패 여부 기반
        // ==========================================
        if (index != null && index.startsWith("audit-logs")) {
            String eventResult = extractEventResult(doc);
            // 로그인 실패 등은 WARN 처리가 적절할 수 있으나 비즈니스 요건에 따라 ERROR 유지
            return "failure".equalsIgnoreCase(eventResult) ? "ERROR" : "INFO";
        }

        // ==========================================
        // 5. 기본 반환 (원본 데이터의 레벨 존중)
        // ==========================================
        String[] levelFields = {"log_level", "logLevel", "level", "severity"};
        for (String field : levelFields) {
            Object value = doc.get(field);
            if (value != null && !value.toString().isEmpty() && !"null".equals(value.toString())) {
                String level = value.toString().toUpperCase();
                // 표준 레벨 패턴이면 그대로 반환
                if (level.matches("CRITICAL|FATAL|ERROR|WARN|INFO|DEBUG|TRACE")) {
                    return level;
                }
            }
        }

        return "INFO";
    }

    private int extractHttpStatusCode(Map<String, Object> doc) {
        if (doc.get("http") instanceof Map) {
            Map<String, Object> http = (Map<String, Object>) doc.get("http");
            Object statusCode = http.get("status_code");
            if (statusCode instanceof Number) {
                return ((Number) statusCode).intValue();
            }
        }
        return 0;
    }

    private String extractThreatLevel(Map<String, Object> doc) {
        if (doc.get("security") instanceof Map) {
            Map<String, Object> security = (Map<String, Object>) doc.get("security");
            return (String) security.get("threat_level");
        }
        return null;
    }

    private String extractEventResult(Map<String, Object> doc) {
        if (doc.get("event") instanceof Map) {
            Map<String, Object> event = (Map<String, Object>) doc.get("event");
            return (String) event.get("result");
        }
        return null;
    }

    private String extractErrorSeverity(Map<String, Object> doc) {
        if (doc.get("error") instanceof Map) {
            Map<String, Object> error = (Map<String, Object>) doc.get("error");
            return (String) error.get("severity");
        }
        return null;
    }

    private boolean hasError(Map<String, Object> doc) {
        return (doc.get("error") instanceof Map) || doc.containsKey("stack_trace");
    }

    /**
     * 에러 응답 생성
     * @param errorMessage 에러 메시지
     * @return Map
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("total", 0);
        errorResponse.put("logs", Collections.emptyList());
        errorResponse.put("error", errorMessage);
        return errorResponse;
    }

    private Double getAggregationValue(SearchResponse<Void> response, String aggName) {
        try {
            if (response.aggregations() != null && response.aggregations().get(aggName) != null) {
                co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg =
                        response.aggregations().get(aggName);

                if (agg.isAvg()) {
                    return agg.avg().value();
                } else if (agg.isMax()) {
                    return agg.max().value();
                } else if (agg.isSum()) {
                    return agg.sum().value();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get aggregation value: {}", aggName);
        }
        return 0.0;
    }

    private Double getBucketAggregationValue(
            co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket bucket,
            String aggName) {
        try {
            if (bucket.aggregations() != null && bucket.aggregations().get(aggName) != null) {
                co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg =
                        bucket.aggregations().get(aggName);

                if (agg.isAvg()) {
                    return agg.avg().value();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get bucket aggregation value: {}", aggName);
        }
        return 0.0;
    }
}