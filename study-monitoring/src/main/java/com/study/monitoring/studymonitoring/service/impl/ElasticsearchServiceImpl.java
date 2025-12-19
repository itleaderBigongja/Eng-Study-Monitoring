package com.study.monitoring.studymonitoring.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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
 * Elasticsearch 서비스 구현 (개선 버전)
 *
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
            int from,
            int size
    ) {
        try {
            log.debug("Searching logs: index={}, keyword={}, logLevel={}, from={}, size={}",
                    indexPattern, keyword, logLevel, from, size);

            // 1. Bool 쿼리 빌드
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 키워드 검색 (Full-text Search)
            if (keyword != null && !keyword.isEmpty()) {
                boolQuery.must(ElasticsearchQueryUtil.buildMatchQuery("message", keyword));
            }

            // 로그 레벨 필터
            if (logLevel != null && !logLevel.isEmpty()) {
                boolQuery.must(ElasticsearchQueryUtil.buildLogLevelQuery(logLevel));
            }

            // 2. Elasticsearch 검색 실행
            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                            .index(indexPattern)
                            .from(from)
                            .size(size)
                            .query(boolQuery.build()._toQuery())
                            .sort(so -> so
                                    .field(f -> f
                                            .field("@timestamp")
                                            .order(SortOrder.Desc)
                                    )
                            ),
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

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                            .index(indexPattern)
                            .size(0)
                            .aggregations("by_log_level", Aggregation.of(a -> a
                                    .terms(t -> t
                                            .field("log_level.keyword")  // ✅ .keyword 필드 사용
                                            .size(10)
                                    )
                            )),
                    Void.class
            );

            Map<String, Long> counts = new HashMap<>();

            if (response.aggregations() != null &&
                    response.aggregations().get("by_log_level") != null) {

                response.aggregations().get("by_log_level").sterms().buckets().array()
                        .forEach(bucket -> counts.put(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ));
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
                            .index("error-logs-*")
                            .size(limit)
                            .query(ElasticsearchQueryUtil.buildLogLevelQuery("ERROR"))
                            .sort(so -> so
                                    .field(f -> f
                                            .field("@timestamp")
                                            .order(SortOrder.Desc)
                                    )
                            ),
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
            log.info("Querying Elasticsearch distribution: {} ~ {}, period={}, logLevel={}",
                    startTime, endTime, timePeriod, logLevel);

            // timePeriod에 따른 interval 결정
            String interval = calculateInterval(timePeriod);

            // Bool 쿼리 생성
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 날짜 범위 쿼리 생성
            Query timeRangeQuery = ElasticsearchQueryUtil.buildDateRangeQuery(startTime, endTime);

            // logLevel 필터링 추가
            if (logLevel != null && !logLevel.isEmpty()) {
                boolQuery.must(ElasticsearchQueryUtil.buildLogLevelQuery(logLevel));
            }

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                            .index(indexPattern)
                            .size(0)
                            .query(boolQuery.build()._toQuery())
                            .aggregations("logs_over_time", Aggregation.of(a -> a
                                    .dateHistogram(dh -> dh
                                            .field("@timestamp")
                                            .fixedInterval(fi -> fi.time(interval))    // 동적 interval
                                            .format("yyyy-MM-dd HH:mm:ss")              // 날짜 포맷 지정
                                    )
                            )),
                    Void.class
            );

            List<Map<String, Object>> distribution = new ArrayList<>();

            if (response.aggregations() != null &&
                    response.aggregations().get("logs_over_time") != null) {

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
            log.error("Failed to get log distribution: indexPattern={}, startTime={}, endTime={}",
                    indexPattern, startTime, endTime, e);
            return Collections.emptyList();
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

        // 소스 데이터 추가
        if (hit.source() != null) {
            result.putAll(hit.source());
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
}