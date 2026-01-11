package com.study.monitoring.studymonitoring.util;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;

import java.time.LocalDateTime;

/** * Elasticsearch 쿼리 유틸리티 클래스
 * (Java 21 Refactoring: Switch Expression, var, isBlank 적용)
 */
public class ElasticsearchQueryUtil {

    /**
     * ✅ 로그 레벨 쿼리 빌더
     * Java 21의 Switch Rule을 사용하여 가독성 향상
     */
    public static Query buildLogLevelQuery(String logLevel) {
        if (logLevel == null || logLevel.isBlank()) { // Java 11+ isBlank()
            return null;
        }

        var upperLevel = logLevel.toUpperCase(); // var 사용
        var boolQuery = new BoolQuery.Builder().minimumShouldMatch("1");

        // 1. 공통 텍스트 필드 검색 (log_level, logLevel, level)
        boolQuery.should(s -> s.term(t -> t.field("log_level.keyword").value(upperLevel)));
        boolQuery.should(s -> s.term(t -> t.field("logLevel.keyword").value(upperLevel)));
        boolQuery.should(s -> s.term(t -> t.field("level.keyword").value(upperLevel)));

        // 2. 레벨별 조건 분기 (Enhanced Switch)
        switch (upperLevel) {
            case "INFO" -> {
                // Access Logs: 200~399
                boolQuery.should(s -> s.bool(b -> b
                        .should(sh -> sh.range(r -> r.field("http.status_code")
                                .gte(JsonData.of(200)).lte(JsonData.of(399))))
                ));
                // Security: low
                boolQuery.should(s -> s.term(t -> t.field("security.threat_level.keyword").value("low")));
                // Audit: success
                boolQuery.should(s -> s.term(t -> t.field("event.result.keyword").value("success")));
            }

            case "WARN" -> {
                // Access Logs: 400~499
                boolQuery.should(s -> s.range(r -> r.field("http.status_code")
                        .gte(JsonData.of(400)).lte(JsonData.of(499))));
                // Security: medium
                boolQuery.should(s -> s.term(t -> t.field("security.threat_level.keyword").value("medium")));
            }

            case "ERROR" -> {
                applyErrorCommonCriteria(boolQuery, upperLevel); // 공통 에러 로직 분리
                // Security: high
                boolQuery.should(s -> s.term(t -> t.field("security.threat_level.keyword").value("high")));
            }

            case "CRITICAL", "FATAL" -> { // Multi-label case
                applyErrorCommonCriteria(boolQuery, upperLevel);
                // Security: critical (CRITICAL/FATAL 전용)
                boolQuery.should(s -> s.term(t -> t.field("security.threat_level.keyword").value("critical")));
            }

            default -> {
                // 정의되지 않은 레벨은 기본 텍스트 필드 검색만 수행 (No-op)
            }
        }

        return boolQuery.build()._toQuery();
    }

    /**
     * ERROR, CRITICAL, FATAL의 공통 조건을 처리하는 헬퍼 메서드
     */
    private static void applyErrorCommonCriteria(BoolQuery.Builder boolQuery, String levelVal) {
        // 1. Error Logs: severity 필드
        boolQuery.should(s -> s.term(t -> t.field("error.severity.keyword").value(levelVal)));

        // 2. Access Logs: 500~599
        boolQuery.should(s -> s.range(r -> r.field("http.status_code")
                .gte(JsonData.of(500)).lte(JsonData.of(599))));

        // 3. Audit Logs: failure
        boolQuery.should(s -> s.term(t -> t.field("event.result.keyword").value("failure")));
    }

    // -------------------------------------------------------------------------
    // 아래는 기존 로직과 동일하나 var 및 isBlank 적용
    // -------------------------------------------------------------------------

    public static Query buildMatchQuery(String field, String keyword) {
        return MatchQuery.of(m -> m.field(field).query(keyword))._toQuery();
    }

    public static Query buildMultiFieldSearchQuery(String keyword) {
        return MultiMatchQuery.of(m -> m.query(keyword).fields("*").lenient(true))._toQuery();
    }

    public static Query buildDateRangeQuery(LocalDateTime from, LocalDateTime to) {
        return RangeQuery.of(r -> r.field("@timestamp")
                .gte(JsonData.of(from.toString()))
                .lte(JsonData.of(to.toString())))._toQuery();
    }

    public static Query buildDateRangeQueryFrom(LocalDateTime from) {
        return RangeQuery.of(r -> r.field("@timestamp").gte(JsonData.of(from.toString())))._toQuery();
    }

    public static Query buildDateRangeQueryTo(LocalDateTime to) {
        return RangeQuery.of(r -> r.field("@timestamp").lte(JsonData.of(to.toString())))._toQuery();
    }

    public static Query buildApplicationQuery(String application) {
        return TermQuery.of(t -> t.field("application.keyword").value(application))._toQuery();
    }

    public static Query buildLogSearchQuery(
            String keyword,
            String logLevel,
            String application,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        var boolQuery = new BoolQuery.Builder(); // var 사용

        if (keyword != null && !keyword.isBlank()) {
            boolQuery.must(buildMultiFieldSearchQuery(keyword));
        }

        if (logLevel != null && !logLevel.isBlank()) {
            boolQuery.must(buildLogLevelQuery(logLevel));
        }

        if (application != null && !application.isBlank()) {
            boolQuery.must(buildApplicationQuery(application));
        }

        if (startDate != null && endDate != null) {
            boolQuery.must(buildDateRangeQuery(startDate, endDate));
        } else if (startDate != null) {
            boolQuery.must(buildDateRangeQueryFrom(startDate));
        } else if (endDate != null) {
            boolQuery.must(buildDateRangeQueryTo(endDate));
        }

        return boolQuery.build()._toQuery();
    }
}