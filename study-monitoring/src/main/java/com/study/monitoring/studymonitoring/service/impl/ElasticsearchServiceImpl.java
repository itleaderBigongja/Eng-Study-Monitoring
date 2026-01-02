package com.study.monitoring.studymonitoring.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AverageAggregation;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

            // ✅ [추가] 날짜 범위 필터
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
            log.debug("Fetching recent errors: limit={}", limit);

            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                            // [수정 1] 인덱스 이름 변경: 데이터가 있는 'application-logs-*' 사용
                            .index("application-logs-*")
                            .size(limit)
                            // [수정 2] 쿼리 필드 변경: 'level' -> 'log_level.keyword' (제공된 매핑 기준)
                            .query(q -> q
                                    .term(t -> t
                                            .field("log_level.keyword") // 매핑에 정의된 keyword 필드 사용
                                            .value("ERROR")
                                    )
                            )
                            // [수정 3] 정렬 기준
                            .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc))),
                    Map.class
            );

            List<Map<String, Object>> errors = response.hits().hits().stream()
                    .map(this::convertHitToMap)
                    .collect(Collectors.toList());

            log.debug("Found {} recent errors", errors.size());
            return errors;
        } catch (Exception e) {
            log.error("Failed to get recent errors", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getLogDistributionByTime(
            String indexPattern,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String timePeriod,
            String logLevel)
    {
        try {
            log.info("Querying Elasticsearch distribution: {} ~ {}, period={}, logLevel={}", startTime, endTime, timePeriod, logLevel);
            String interval = calculateInterval(timePeriod);
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(startTime, endTime);
            boolQuery.must(timeRangeQuery);


            if (logLevel != null && !logLevel.isEmpty() && !"undefined".equals(logLevel)) {
                boolQuery.must(ElasticsearchQueryUtil.buildLogLevelQuery(logLevel));
            }

            // [핵심 수정] Elasticsearch 포맷과 동일한 포맷터 정의
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // [핵심 수정] LocalDateTime을 해당 포맷의 문자열로 변환
            String minBound = startTime.format(formatter);
            String maxBound = endTime.format(formatter);

            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern)
                            .size(0)
                            .query(boolQuery.build()._toQuery())
                            .aggregations("logs_over_time",
                                    Aggregation.of(a -> a
                                            .dateHistogram(dh -> dh
                                                    .field("@timestamp")
                                                    .fixedInterval(fi -> fi.time(interval))
                                                    .format("yyyy-MM-dd HH:mm:ss") // 이 포맷과 아래 extendedBounds 형식이 일치해야 함
                                                    .minDocCount(0)
                                                    .extendedBounds(b -> b
                                                            // [수정] .toString() 대신 포맷팅된 문자열 사용
                                                            .min(FieldDateMath.of(f -> f.expr(minBound)))
                                                            .max(FieldDateMath.of(f -> f.expr(maxBound)))
                                                    )
                                            )
                                    )
                            ), Void.class
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
            log.error("Failed to get log distribution: indexPattern={}, startTime={}, endTime={}", indexPattern, startTime, endTime, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> countByHttpMethod(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by HTTP method: index={}, start={}, end={}", indexPattern, start, end);
            // 날짜 범위 쿼리
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(s ->
                            s.index(indexPattern).size(0)
                            .query(timeRangeQuery).aggregations("by_method", Aggregation.of(a ->
                                            a.terms(t ->
                                            t.field("http.method").size(10))
                            )),
                    Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_method") != null) {
                response.aggregations().get("by_method").sterms().buckets().array()
                        .forEach(bucket ->
                                counts.put(bucket.key().stringValue(), bucket.docCount()));
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

    @Override
    public List<Map<String, Object>> getAccessLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying access log distribution: {} ~ {}, period={}", start, end, timePeriod);
            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery).aggregations("access_over_time", Aggregation.of(
                           a -> a.dateHistogram(
                                   dh -> dh.field("@timestamp").fixedInterval(
                                           fi -> fi.time(interval)).format("yyyy-MM-dd HH:mm:ss")).aggregations("avg_response_time", Aggregation.of(
                                                   sub -> sub.avg(avg -> avg.field("http.response_time_ms")))
                                   ).aggregations("error_count", Aggregation.of(sub ->
                                           sub.filter(
                                                   f -> f.range(
                                                           r -> r.field("http.status_code").gte(co.elastic.clients.json.JsonData.of(500)))
                                           )
                                        )
                                    )
                                )
                        ), Void.class);
            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("access_over_time") != null) {
                response.aggregations().get("access_over_time").dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("requestCount", bucket.docCount());

                            // 평균 응답시간
                            Double avgResponseTime = bucket.aggregations().get("avg_response_time").avg().value();
                            entry.put("avgResponseTime", avgResponseTime != null ? avgResponseTime : 0.0);

                            // 에러 수
                            Long errorCount = bucket.aggregations().get("error_count").filter().docCount();
                            entry.put("errorCount", errorCount);
                            distribution.add(entry);
                        });
            }
            log.info("Access log distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get access log distribution: indexPattern={}", indexPattern, e);
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
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery).aggregations("by_error_type", Aggregation.of(
                            a -> a.terms(
                                    t -> t.field("error.type").size(20)))), Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_error_type") != null) {
                response.aggregations().get("by_error_type").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
            }
            log.debug("Error type counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by error type: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countBySeverity(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by severity: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern)
                            .size(0).query(timeRangeQuery).aggregations("by_severity", Aggregation.of(
                                    a -> a.terms(
                                            t -> t.field("error.severity").size(10))
                            )), Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_severity") != null) {
                response.aggregations().get("by_severity").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
            }
            log.debug("Severity counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by severity: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> getErrorLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying error log distribution: {} ~ {}, period={}", start, end, timePeriod);
            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                s -> s.index(indexPattern).size(0).query(timeRangeQuery).aggregations("errors_over_time", Aggregation.of(
                        a -> a.dateHistogram(
                            dh -> dh.field("@timestamp").fixedInterval(
                                fi -> fi.time(interval)).format("yyyy-MM-dd HH:mm:ss")).aggregations("error_type_breakdown", Aggregation.of(
                                    sub -> sub.terms(t -> t.field("error.type").size(5))
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

    @Override
    public List<Map<String, Object>> getPerformanceMetricsDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying performance metrics distribution: {} ~ {}, period={}", start, end, timePeriod);
            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
        s -> s.index(indexPattern).size(0).query(timeRangeQuery).aggregations("metrics_over_time", Aggregation.of(
            a -> a.dateHistogram(
                dh -> dh.field("@timestamp").fixedInterval(
                    fi -> fi.time(interval)).format("yyyy-MM-dd HH:mm:ss")).aggregations("avg_cpu_usage", Aggregation.of(
                        sub -> sub.avg(avg -> avg.field("system.cpu_usage")))).aggregations("avg_memory_usage", Aggregation.of(
                            sub -> sub.avg(avg -> avg.field("system.memory_usage")))).aggregations("avg_heap_usage", Aggregation.of(
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
            log.error("Failed to get performance metrics distribution: indexPattern={}", indexPattern, e);
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
                            .aggregations("by_operation",
                                Aggregation.of(a -> a.terms(t -> t.field("operation").size(10))
                            )), Void.class
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
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery).aggregations("by_table",
                            Aggregation.of(a -> a.terms(t -> t.field("table").size(20)))), Void.class
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
                                    a -> a.avg(avg -> avg.field("query.duration_ms"))
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
                stats.put("avgDuration", getAggregationValue(response, "avg_duration"));
                stats.put("maxDuration", getAggregationValue(response, "max_duration"));
                stats.put("slowQueryCount", response.aggregations().get("slow_queries").filter().docCount());
                stats.put("totalQueryCount", response.hits().total().value());
            }
            log.debug("Query performance stats: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("Failed to get query performance stats: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> getDatabaseLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod)
    {
        try {
            log.info("Querying database log distribution: {} ~ {}, period={}", start, end, timePeriod);
            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                    s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("db_logs_over_time",
                                Aggregation.of(
                                a -> a.dateHistogram(
                                    dh -> dh.field("@timestamp").fixedInterval(
                                        fi -> fi.time(interval)).format("yyyy-MM-dd HH:mm:ss"))
                                    .aggregations("avg_duration", Aggregation.of(sub -> sub
                                            .avg(avg -> avg.field("query.duration_ms"))
                                    ))
                                    .aggregations("slow_query_count", Aggregation.of(sub -> sub
                                        .filter(f -> f
                                            .range(r -> r
                                                .field("query.duration_ms")
                                                .gte(co.elastic.clients.json.JsonData.of(1000))))))
                            )), Void.class
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
            log.error("Failed to get database log distribution: indexPattern={}", indexPattern, e);
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
                        a -> a.terms(t -> t.field("event.action").size(20)))), Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_action") != null) {
                response.aggregations().get("by_action").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));;
            }
            log.debug("Event action counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to get event action: index={}, start={}, end={}", indexPattern, start, end, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> countByCategory(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Counting by category: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index(indexPattern)
                    .size(0)
                    .query(timeRangeQuery)
                    .aggregations("by_category", a -> a
                            .terms(t -> t
                                    // [수정] .keyword 제거! (이미 keyword 타입이므로 본래 필드명 사용)
                                    .field("event.category")
                                    .size(10)
                            )
                    ), Void.class
            );

            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_category") != null) {
                List<StringTermsBucket> buckets = response.aggregations().get("by_category").sterms().buckets().array();
                for (StringTermsBucket bucket : buckets) {
                    counts.put(bucket.key().stringValue(), bucket.docCount());
                }
            }
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
                            a -> a.terms(
                                    t -> t.field("event.result").size(5)))), Void.class
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
            log.error("Failed to count by event result: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> getAuditLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end,String timePeriod) {
        try {
            log.info("Querying audit log distribution: {} ~ {}, period={}", start, end, timePeriod);
            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
        s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                    .aggregations("audit_over_time", Aggregation.of(
                a -> a.dateHistogram(
                    dh -> dh.field("@timestamp").fixedInterval(
                            fi -> fi.time(interval)).format("yyyy-MM-dd HH:mm:ss"))
                                    .aggregations("success_count", Aggregation.of(
                                sub -> sub.filter(
                                        f -> f.term(t -> t.field("event.result").value("success")))))
                                    .aggregations("failure_count", Aggregation.of(
                                sub -> sub.filter(
                                    f -> f.term(
                                            t -> t.field("event.result").value("failure")))))
                            )), Void.class
            );
            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("audit_over_time") != null) {
                response.aggregations().get("audit_over_time")
                        .dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("totalEvents", bucket.docCount());

                            // 성공/실패 카운트
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
            log.error("Failed to get audit log distribution: indexPattern={}", indexPattern, e);
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
                a -> a.terms(
                        t -> t.field("security.threat_level").size(10)))), Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_threat_level") != null) {
                response.aggregations().get("by_threat_level").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
            }
            log.debug("Threat level counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by threat level: indexPattern={}", indexPattern, e);
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
                .aggregations("by_attack_type",
                    Aggregation.of(a -> a.terms(
                t -> t.field("attack.type").size(20)))), Void.class
            );
            Map<String, Long> counts = new HashMap<>();
            if (response.aggregations() != null && response.aggregations().get("by_attack_type") != null) {
                response.aggregations().get("by_attack_type").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
            }
            log.debug("Attack type counts: {}", counts);
            return counts;
        } catch (Exception e) {
            log.error("Failed to count by attack type: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> getBlockStatistics(String indexPattern, LocalDateTime start, LocalDateTime end) {
        try {
            log.debug("Getting block statistics: index={}, start={}, end={}", indexPattern, start, end);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
        s -> s.index(indexPattern).size(0).query(timeRangeQuery).aggregations("blocked_attacks", Aggregation.of(
            a -> a.filter(
                    f -> f.term(
                            t -> t.field("blocked").value(true)))))
                            .aggregations("allowed_attacks", Aggregation.of(
                            a -> a.filter(f ->
                                        f.term(t -> t.field("blocked").value(false))))), Void.class
            );
            Map<String, Long> stats = new HashMap<>();
            if (response.aggregations() != null) {
                long totalAttacks = response.hits().total() != null ? response.hits().total().value() : 0;
                long blockedAttacks = response.aggregations().get("blocked_attacks").filter().docCount();
                long allowedAttacks = response.aggregations().get("allowed_attacks").filter().docCount();

                stats.put("totalAttacks", totalAttacks);
                stats.put("blockedAttacks", blockedAttacks);
                stats.put("allowedAttacks", allowedAttacks);
            }
            log.debug("Block statistics: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("Failed to get block statistics: indexPattern={}", indexPattern, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> getSecurityLogDistributionByTime(String indexPattern, LocalDateTime start, LocalDateTime end, String timePeriod) {
        try {
            log.info("Querying security log distribution: {} ~ {}, period={}", start, end, timePeriod);
            String interval = calculateInterval(timePeriod);
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(start, end);
            SearchResponse<Void> response = elasticsearchClient.search(
                s -> s.index(indexPattern).size(0).query(timeRangeQuery)
                            .aggregations("security_over_time", Aggregation.of(
                        a -> a.dateHistogram(
                            dh -> dh.field("@timestamp").fixedInterval(
                                    fi -> fi.time(interval)).format("yyyy-MM-dd HH:mm:ss"))
                            .aggregations("blocked_count", Aggregation.of(
                        sub -> sub.filter(
                            f -> f.term(
                                    t -> t.field("blocked").value(true)))))
                            .aggregations("threat_level_breakdown", Aggregation.of(
                        sub -> sub.terms(
                            t -> t.field("security.threat_level").size(5)))))), Void.class
            );
            List<Map<String, Object>> distribution = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("security_over_time") != null) {
                response.aggregations().get("security_over_time")
                        .dateHistogram().buckets().array()
                        .forEach(bucket -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("timestamp", bucket.keyAsString());
                            entry.put("attackCount", bucket.docCount());

                            // 차단된 공격 수
                            Long blockedCount = bucket.aggregations().get("blocked_count").filter().docCount();
                            entry.put("blockedCount", blockedCount);

                            // 위협 레벨별 분포
                            Map<String, Long> threatLevelBreakdown = new HashMap<>();
                            bucket.aggregations().get("threat_level_breakdown")
                                    .sterms().buckets().array()
                                    .forEach(threatBucket -> threatLevelBreakdown.put(
                                            threatBucket.key().stringValue(),
                                            threatBucket.docCount()
                                    ));
                            entry.put("threatLevelBreakdown", threatLevelBreakdown);
                            distribution.add(entry);
                        });
            }
            log.info("Security log distribution result: {} time buckets", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get security log distribution: indexPattern={}", indexPattern, e);
            return Collections.emptyList();
        }
    }

    @Override
    public PageResponseDTO<Map<String, Object>> searchErrorLogs(String type, int page, int size) {
        String indexName;

        // 탭에 따라 인덱스 결정
        if ("SYSTEM".equalsIgnoreCase(type)) {
            indexName = "error-logs-*"; // 에러 전용 로그 인덱스
        } else {
            indexName = "application-logs-*"; // 일반 애플리케이션 로그 인덱스
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
                                    // error-logs-* 인덱스는 모든 데이터가 에러이므로 별도 필터 없이 전체 조회
                                    // (필요하다면 severity가 CRITICAL/FATAL인 것만 필터링 가능)
                                    return q.matchAll(m -> m);
                                } else {
                                    // application-logs-* 인덱스는 log_level이 ERROR인 것만 조회
                                    return q.term(t -> t.field("log_level.keyword").value("ERROR"));
                                }
                            })
                            .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc))),
                    Map.class
            );

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

        // ✅ 인덱스 타입별 필드 매핑
        String indexName = hit.index();

        if (indexName.startsWith("application-logs")) {
            // application-logs: 표준 로그 필드
            result.put("log_level", source.get("log_level"));
            result.put("logger_name", source.get("logger_name"));
            result.put("message", source.get("message"));
            result.put("stack_trace", source.get("stack_trace"));
            result.put("thread_name", source.get("thread_name"));

        } else if (indexName.startsWith("access-logs")) {
            // access-logs: HTTP 접근 로그
            Map<String, Object> http = (Map<String, Object>) source.get("http");
            if (http != null) {
                result.put("log_level", "INFO"); // 기본 레벨
                result.put("logger_name", "AccessLog");

                // HTTP 정보를 메시지로 구성
                String message = String.format("%s %s - Status: %s, Response Time: %sms",
                        http.get("method"),
                        http.get("url"),
                        http.get("status_code"),
                        http.get("response_time_ms")
                );
                result.put("message", message);

                // 원본 HTTP 데이터도 포함
                result.put("http", http);
            }

            Map<String, Object> client = (Map<String, Object>) source.get("client");
            if (client != null) {
                result.put("client", client);
            }

        } else if (indexName.startsWith("error-logs")) {
            // error-logs: 에러 로그
            Map<String, Object> error = (Map<String, Object>) source.get("error");
            if (error != null) {
                result.put("log_level", error.get("severity")); // severity를 log_level로 매핑
                result.put("logger_name", "ErrorLog");
                result.put("message", error.get("type") + ": " + error.get("message"));
                result.put("stack_trace", error.get("stack_trace"));
                result.put("error", error);
            }

            Map<String, Object> sourceInfo = (Map<String, Object>) source.get("source");
            if (sourceInfo != null) {
                result.put("source", sourceInfo);
            }

        } else if (indexName.startsWith("performance-metrics")) {
            // performance-metrics: 성능 메트릭
            result.put("log_level", "INFO");
            result.put("logger_name", "PerformanceMetrics");

            Map<String, Object> system = (Map<String, Object>) source.get("system");
            Map<String, Object> jvm = (Map<String, Object>) source.get("jvm");

            StringBuilder message = new StringBuilder("Performance Metrics - ");
            if (system != null) {
                message.append(String.format("CPU: %.1f%%, Memory: %.1f%%",
                        system.get("cpu_usage"),
                        system.get("memory_usage")
                ));
                result.put("system", system);
            }
            if (jvm != null) {
                message.append(String.format(", Heap: %s bytes", jvm.get("heap_used")));
                result.put("jvm", jvm);
            }

            result.put("message", message.toString());

        } else if (indexName.startsWith("database-logs")) {
            // database-logs: 데이터베이스 로그
            result.put("log_level", "INFO");
            result.put("logger_name", "DatabaseLog");

            Map<String, Object> query = (Map<String, Object>) source.get("query");
            if (query != null) {
                String message = String.format("%s - %s (Duration: %sms)",
                        source.get("operation"),
                        source.get("table"),
                        query.get("duration_ms")
                );
                result.put("message", message);
                result.put("query", query);
                result.put("stack_trace", query.get("sql")); // SQL을 스택 트레이스 영역에 표시
            }

            result.put("operation", source.get("operation"));
            result.put("table", source.get("table"));

        } else if (indexName.startsWith("audit-logs")) {
            // audit-logs: 감사 로그
            Map<String, Object> event = (Map<String, Object>) source.get("event");
            Map<String, Object> user = (Map<String, Object>) source.get("user");

            if (event != null) {
                String result_status = (String) event.get("result");
                result.put("log_level", "success".equals(result_status) ? "INFO" : "WARN");
                result.put("logger_name", "AuditLog");

                String message = String.format("User: %s, Action: %s, Result: %s",
                        user != null ? user.get("login_id") : "unknown",
                        event.get("action"),
                        event.get("result")
                );
                result.put("message", message);
                result.put("event", event);
            }

            if (user != null) {
                result.put("user", user);
            }

        } else if (indexName.startsWith("security-logs")) {
            // security-logs: 보안 로그
            Map<String, Object> security = (Map<String, Object>) source.get("security");
            Map<String, Object> attack = (Map<String, Object>) source.get("attack");
            Map<String, Object> attacker = (Map<String, Object>) source.get("attacker");

            if (security != null && attack != null) {
                String threatLevel = (String) security.get("threat_level");

                // threat_level을 log_level로 매핑
                String logLevel;
                switch (threatLevel != null ? threatLevel.toLowerCase() : "low") {
                    case "critical":
                        logLevel = "FATAL";
                        break;
                    case "high":
                        logLevel = "ERROR";
                        break;
                    case "medium":
                        logLevel = "WARN";
                        break;
                    default:
                        logLevel = "INFO";
                        break;
                }

                result.put("log_level", logLevel);
                result.put("logger_name", "SecurityLog");

                String message = String.format("Security Event - Type: %s, Threat Level: %s, Blocked: %s",
                        attack.get("type"),
                        threatLevel,
                        source.get("blocked")
                );
                result.put("message", message);

                result.put("security", security);
                result.put("attack", attack);

                if (attacker != null) {
                    result.put("attacker", attacker);
                }
            }

        } else {
            // 알 수 없는 인덱스: 원본 데이터 그대로 반환
            result.putAll(source);
            result.put("log_level", "INFO");
            result.put("logger_name", "Unknown");
            result.put("message", source.toString());
        }

        return result;
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