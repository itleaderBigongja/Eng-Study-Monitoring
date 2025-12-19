package com.study.monitoring.studymonitoring.util;


import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch 쿼리 빌더 유틸리티
 *
 * 역할:
 * - 자주 사용하는 Elasticsearch 쿼리 빌드
 * - 인덱스 패턴 생성
 * - 날짜 범위 쿼리 헬퍼
 */
public class ElasticsearchQueryUtil {

    private static final DateTimeFormatter INDEX_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 현재 월 인덱스명 생성
     *
     * @param indexType 인덱스 타입 (예: application-logs)
     * @return 인덱스명 (예: application-logs-2025-12)
     */
    public static String getCurrentMonthIndex(String indexType) {
        String currentMonth = YearMonth.now().format(INDEX_MONTH_FORMATTER);
        return indexType + "-" + currentMonth;
    }

    /**
     * 특정 월 인덱스명 생성
     *
     * @param indexType 인덱스 타입
     * @param yearMonth 년월
     * @return 인덱스명
     */
    public static String getMonthIndex(String indexType, YearMonth yearMonth) {
        return indexType + "-" + yearMonth.format(INDEX_MONTH_FORMATTER);
    }

    /**
     * 여러 월 인덱스 패턴 생성
     *
     * @param indexType 인덱스 타입
     * @param months 조회할 개월 수
     * @return 인덱스 배열 (예: ["logs-2025-12", "logs-2025-11"])
     */
    public static String[] getRecentMonthsIndices(String indexType, int months) {
        List<String> indices = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 0; i < months; i++) {
            indices.add(getMonthIndex(indexType, current.minusMonths(i)));
        }

        return indices.toArray(new String[0]);
    }

    /**
     * 로그 레벨 Term 쿼리 생성
     *
     * @param logLevel 로그 레벨
     * @return Query
     */
    public static Query buildLogLevelQuery(String logLevel) {
        return TermQuery.of(t -> t
                .field("log_level.keyword")
                .value(logLevel)
        )._toQuery();
    }

    /**
     * 키워드 Match 쿼리 생성 (Full-text Search)
     *
     * @param field 필드명
     * @param keyword 키워드
     * @return Query
     */
    public static Query buildMatchQuery(String field, String keyword) {
        return MatchQuery.of(m -> m
                .field(field)
                .query(keyword)
        )._toQuery();
    }

    /**
     * 날짜 범위 쿼리 생성
     *
     * @param from 시작 시간
     * @param to 종료 시간
     * @return Query
     */
    public static Query buildDateRangeQuery(LocalDateTime from, LocalDateTime to) {
        return RangeQuery.of(r -> r
                .field("@timestamp")
                .gte(JsonData.of(from.toString()))
                .lte(JsonData.of(to.toString()))
        )._toQuery();
    }

    /**
     * 최근 N시간 범위 쿼리 생성
     *
     * @param hours 시간 수
     * @return Query
     */
    public static Query buildRecentHoursQuery(int hours) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        return buildDateRangeQuery(from, to);
    }

    /**
     * 애플리케이션 Term 쿼리 생성
     *
     * @param application 애플리케이션 이름
     * @return Query
     */
    public static Query buildApplicationQuery(String application) {
        return TermQuery.of(t -> t
                .field("application.keyword")
                .value(application)
        )._toQuery();
    }

    /**
     * Bool 쿼리 빌더 (복합 조건)
     *
     * @return BoolQuery.Builder
     */
    public static BoolQuery.Builder boolQueryBuilder() {
        return new BoolQuery.Builder();
    }

    /**
     * 로그 검색용 Bool 쿼리 생성 (예시)
     *
     * @param keyword 키워드
     * @param logLevel 로그 레벨
     * @param application 애플리케이션
     * @param hours 조회 기간
     * @return Query
     */
    public static Query buildLogSearchQuery(
            String keyword,
            String logLevel,
            String application,
            int hours) {

        BoolQuery.Builder boolQuery = boolQueryBuilder();

        // 키워드 검색
        if (keyword != null && !keyword.isEmpty()) {
            boolQuery.must(buildMatchQuery("message", keyword));
        }

        // 로그 레벨 필터
        if (logLevel != null && !logLevel.isEmpty()) {
            boolQuery.must(buildLogLevelQuery(logLevel));
        }

        // 애플리케이션 필터
        if (application != null && !application.isEmpty()) {
            boolQuery.must(buildApplicationQuery(application));
        }

        // 시간 범위 필터
        if (hours > 0) {
            boolQuery.must(buildRecentHoursQuery(hours));
        }

        return boolQuery.build()._toQuery();
    }
}
