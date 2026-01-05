package com.study.monitoring.studymonitoring.util;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Elasticsearch 쿼리 유틸리티 클래스 */
public class ElasticsearchQueryUtil {

    private static final DateTimeFormatter INDEX_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    /** 특정 월 인덱스 이름 생성 */
    public static String getMonthIndex(String indexType, YearMonth yearMonth) {
        return indexType + "-" + yearMonth.format(INDEX_MONTH_FORMATTER);
    }

    /**
     * ✅ 범용 로그 레벨 쿼리 빌더
     * 사용자가 선택한 Level(INFO, ERROR 등)을 각 인덱스의 특징에 맞게 변환하여 검색합니다.
     */
    public static Query buildLogLevelQuery(String logLevel) {
        if (logLevel == null || logLevel.trim().isEmpty()) {
            return null;
        }

        String upperLevel = logLevel.toUpperCase(); // INFO, WARN, ERROR

        // BoolQuery의 Should(OR) 조건을 사용하여 어떤 인덱스든 걸리게 함
        BoolQuery.Builder boolQuery = new BoolQuery.Builder().minimumShouldMatch("1");

        // 1. [공통] 텍스트 기반 레벨 필드 (application-logs, error-logs 등)
        // 사용자가 "ERROR" 선택 시 -> log_level="ERROR" OR error.severity="ERROR"
        boolQuery.should(s -> s.term(t -> t.field("log_level.keyword").value(upperLevel)));
        boolQuery.should(s -> s.term(t -> t.field("error.severity").value(upperLevel))); // error-logs

        // -------------------------------------------------------
        // 2. [매핑] 의미 기반 검색 (인덱스별 특수 필드 매핑)
        // -------------------------------------------------------

        switch (upperLevel) {
            case "INFO":
                // Access Log: 200번대 상태 코드
                boolQuery.should(s -> s.range(r -> r.field("http.status_code").gte(JsonData.of(200)).lte(JsonData.of(299))));
                // Security Log: 위협 레벨 low
                boolQuery.should(s -> s.term(t -> t.field("security.threat_level").value("low")));
                // Audit Log: 성공(success)
                boolQuery.should(s -> s.term(t -> t.field("event.result").value("success")));
                break;

            case "WARN":
                // Access Log: 400번대 상태 코드 (Client Error)
                boolQuery.should(s -> s.range(r -> r.field("http.status_code").gte(JsonData.of(400)).lte(JsonData.of(499))));
                // Security Log: 위협 레벨 medium
                boolQuery.should(s -> s.term(t -> t.field("security.threat_level").value("medium")));
                break;

            case "ERROR":
            case "FATAL":
            case "CRITICAL":
                // Access Log: 500번대 상태 코드 (Server Error)
                boolQuery.should(s -> s.range(r -> r.field("http.status_code").gte(JsonData.of(500)).lte(JsonData.of(599))));
                // Security Log: 위협 레벨 high, critical
                boolQuery.should(s -> s.terms(t -> t.field("security.threat_level")
                        .terms(tt -> tt.value(java.util.List.of(FieldValue.of("high"), FieldValue.of("critical"))))));
                // Audit Log: 실패(failure)
                boolQuery.should(s -> s.term(t -> t.field("event.result").value("failure")));
                break;

            case "DEBUG":
                // Security Log: debug 레벨 (있다면)
                boolQuery.should(s -> s.term(t -> t.field("security.threat_level").value("debug")));
                break;
        }

        return boolQuery.build()._toQuery();
    }

    /** Match 쿼리 (Full-text Search) */
    public static Query buildMatchQuery(String field, String keyword) {
        return MatchQuery.of(m -> m
                .field(field)
                .query(keyword)
        )._toQuery();
    }

    /**
     * [NEW] 모든 필드 검색 (Multi-match Query)
     * 숫자 필드에 문자를 검색해도 에러가 나지 않도록 lenient(true) 설정
     */
    public static Query buildMultiFieldSearchQuery(String keyword) {
        return MultiMatchQuery.of(m -> m
                .query(keyword)       // 검색어
                .fields("*")          // ✅ 모든 필드 검색
                .lenient(true)        // ✅ 숫자 필드에 문자 검색 시 에러 무시 (필수!)
        )._toQuery();
    }

    /** ✅ 날짜 범위 쿼리 (시작 ~ 종료) */
    public static Query buildDateRangeQuery(LocalDateTime from, LocalDateTime to) {
        return RangeQuery.of(r -> r
                .field("@timestamp")
                .gte(JsonData.of(from.toString()))
                .lte(JsonData.of(to.toString()))
        )._toQuery();
    }

    /** ✅ 날짜 범위 쿼리 (시작부터 현재까지) */
    public static Query buildDateRangeQueryFrom(LocalDateTime from) {
        return RangeQuery.of(r -> r
                .field("@timestamp")
                .gte(JsonData.of(from.toString()))
        )._toQuery();
    }

    /** ✅ 날짜 범위 쿼리 (과거부터 종료까지) */
    public static Query buildDateRangeQueryTo(LocalDateTime to) {
        return RangeQuery.of(r -> r
                .field("@timestamp")
                .lte(JsonData.of(to.toString()))
        )._toQuery();
    }

    /** 애플리케이션 필터 쿼리 */
    public static Query buildApplicationQuery(String application) {
        return TermQuery.of(t -> t
                .field("application.keyword")
                .value(application)
        )._toQuery();
    }

    /** Bool 쿼리 빌더 생성 */
    public static BoolQuery.Builder boolQueryBuilder() {
        return new BoolQuery.Builder();
    }

    /** 로그 검색용 복합 쿼리 빌더 (수정됨) */
    public static Query buildLogSearchQuery(
            String keyword,
            String logLevel,
            String application,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        BoolQuery.Builder boolQuery = boolQueryBuilder();

        //  키워드 검색: "message" 단일 필드 -> 모든 필드(*) 검색으로 변경
        if (keyword != null && !keyword.isEmpty()) {
            boolQuery.must(buildMultiFieldSearchQuery(keyword));
        }

        // 로그 레벨 필터
        if (logLevel != null && !logLevel.isEmpty()) {
            boolQuery.must(buildLogLevelQuery(logLevel));
        }

        // 애플리케이션 필터
        if (application != null && !application.isEmpty()) {
            boolQuery.must(buildApplicationQuery(application));
        }

        // 날짜 범위 필터
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