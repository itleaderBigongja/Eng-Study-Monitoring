package com.study.monitoring.studymonitoring.builder;

import org.springframework.stereotype.Component;

@Component // Spring Bean으로 등록하여 다른 서비스에서 주입받아 사용
public class PrometheusQueryBuilder {

    /**
     * Prometheus 쿼리 생성 메인 메서드
     **/
    public static String buildPrometheusQuery(String metricType, String aggregationType, String step, String application) {
        // 1. 시간 집계 함수 (예: avg_over_time): 시간 흐름에 따른 변화를 계산
        String timeAggFunc = convertToPrometheusFunction(aggregationType);
        // 2. 공간 집계 함수 (예: avg, max): 여러 인스턴스(Pod)의 값을 하나로 병합
        String spaceAggFunc = convertToSpatialFunction(aggregationType);
        String resolution = "1m"; // 기본 해상도

        //1. Selector 생성 ({application="eng-study"} 형태)
        String selector = (application != null && !application.isBlank())
                ? String.format("{application=\"%s\"}", application)
                : "";

        // 2. UP 상태 체크 쿼리 (Pod가 하나라도 살아있으면 1, 아니면 0) [수정 포인트 1]
        // application 태그가 없는 경우(빈 셀렉터)를 대비해 vector(1)을 fallback으로 두거나,
        // 확실한 셀렉터가 있다면 max(up%s) or vector(0)을 씁니다.
        String upCheck = String.format("(max(up%s) or vector(0))", selector);

        String baseQuery = "";

        // Case 1: CPU Usage
        if ("CPU_USAGE".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((%s(process_cpu_usage%s))[%s:%s]) * 100",
                    timeAggFunc, spaceAggFunc, selector, step, resolution);
        }
        // Case 2: Heap Usage
        else if ("HEAP_USAGE".equalsIgnoreCase(metricType)) {
            // 방법 1: area="heap"으로 이미 합산된 값이 있는 경우
            String heapSelector = selector.isEmpty()
                    ? "{area=\"heap\"}"
                    : selector.replace("}", ", area=\"heap\"}");

            // ✅ 핵심: sum()을 사용하여 모든 heap 영역을 합산
            String heapUsedQuery = String.format("sum(jvm_memory_used_bytes%s)", heapSelector);
            String heapMaxQuery = String.format("sum(jvm_memory_max_bytes%s)", heapSelector);

            // ✅ 안전한 나눗셈: max가 0이면 0 반환
            String heapRatio = String.format(
                    "(%s / (%s > 0) * (%s)) or vector(0)",
                    heapUsedQuery,
                    heapMaxQuery,
                    heapMaxQuery
            );

            baseQuery = String.format("%s((%s)[%s:%s]) * 100",
                    timeAggFunc, heapRatio, step, resolution);
        }
        // Case 3: Counter Metrics
        else if (isCounterMetric(metricType)) {
            String baseRate = getRateExpression(metricType, resolution, selector);
            if ("SUM".equalsIgnoreCase(aggregationType)) {
                baseQuery = getIncreaseExpression(metricType, step, selector);
            } else {
                baseQuery = switch (aggregationType.toUpperCase()) {
                    case "AVG" -> String.format("avg_over_time((%s)[%s:%s])", baseRate, step, resolution);
                    case "MAX" -> String.format("max_over_time((%s)[%s:%s])", baseRate, step, resolution);
                    case "MIN" -> String.format("min_over_time((%s)[%s:%s])", baseRate, step, resolution);
                    default -> String.format("avg_over_time((%s)[%s:%s])", baseRate, step, resolution);
                };
            }
        }
        // PostgreSQL, Elasticsearch 등 나머지 메트릭들...
        else if ("DB_CONNECTIONS".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((sum(pg_stat_activity_count%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        else if ("DB_SIZE".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((sum(pg_database_size_bytes%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        else if ("DB_TRANSACTIONS".equalsIgnoreCase(metricType)) {
            String query = String.format("sum(rate(pg_stat_database_xact_commit%s[%s])) + sum(rate(pg_stat_database_xact_rollback%s[%s]))",
                    selector, resolution, selector, resolution);
            baseQuery = String.format("avg_over_time((%s)[%s:%s])", query, step, resolution);
        }
        else if ("ES_JVM_HEAP".equalsIgnoreCase(metricType)) {
            String esSelector = selector.isEmpty()
                    ? "{area=\"heap\"}"
                    : selector.replace("}", ", area=\"heap\"}");

            String esHeapUsed = String.format("sum(elasticsearch_jvm_memory_used_bytes%s)", esSelector);
            String esHeapMax = String.format("sum(elasticsearch_jvm_memory_max_bytes%s)", esSelector);

            String esHeapRatio = String.format(
                    "(%s / (%s > 0) * (%s)) or vector(0)",
                    esHeapUsed,
                    esHeapMax,
                    esHeapMax
            );

            baseQuery = String.format("%s((%s)[%s:%s]) * 100",
                    timeAggFunc, esHeapRatio, step, resolution);
        }
        else if ("ES_DATA_SIZE".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((sum(elasticsearch_indices_store_size_bytes%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        else if ("ES_CPU".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((avg(elasticsearch_process_cpu_percent%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        else {
            String metricName = metricType.toLowerCase();
            baseQuery = String.format("%s((%s(%s%s))[%s:%s])", timeAggFunc, spaceAggFunc, metricName, selector, step, resolution);
        }

        // 최종적으로 up 상태를 곱해서 반환 (죽었으면 * 0 이 되어 결과가 0이 됨)
        // 주의: application selector가 명확할 때만 적용하는 것이 안전합니다.
        if (application != null && !application.isBlank()) {
            return String.format("(%s) * %s", baseQuery, upCheck);
        }

        return baseQuery;
    }

    private static boolean isCounterMetric(String metricType) {
        return "TPS".equalsIgnoreCase(metricType) || "ERROR_RATE".equalsIgnoreCase(metricType);
    }

    private static String getRateExpression(String metricType, String window, String selector) {
        if ("TPS".equalsIgnoreCase(metricType)) {
            return String.format("sum(rate(http_server_requests_seconds_count%s[%s]))", selector, window);
        } else if ("ERROR_RATE".equalsIgnoreCase(metricType)) {
            String errorSelector = selector.isEmpty()
                    ? "{status=~\"5..\"}"
                    : selector.replace("}", ", status=~\"5..\"}");
            return String.format(
                    "(sum(rate(http_server_requests_seconds_count%s[%s])) / sum(rate(http_server_requests_seconds_count%s[%s]))) * 100",
                    errorSelector, window, selector, window
            );
        }
        return "";
    }

    private static String getIncreaseExpression(String metricType, String window, String selector) {
        if ("TPS".equalsIgnoreCase(metricType)) {
            return String.format("sum(increase(http_server_requests_seconds_count%s[%s]))", selector, window);
        } else if ("ERROR_RATE".equalsIgnoreCase(metricType)) {
            String errorSelector = selector.isEmpty()
                    ? "{status=~\"5..\"}"
                    : selector.replace("}", ", status=~\"5..\"}");
            return String.format("sum(increase(http_server_requests_seconds_count%s[%s]))", errorSelector, window);
        }
        return "";
    }

    private static String convertToSpatialFunction(String aggregationType) {
        return switch (aggregationType.toUpperCase()) {
            case "MAX" -> "max";
            case "MIN" -> "min";
            case "SUM" -> "sum";
            case "COUNT" -> "count";
            default -> "avg";
        };
    }

    private static String convertToPrometheusFunction(String aggregationType) {
        return switch (aggregationType.toUpperCase()) {
            case "AVG" -> "avg_over_time";
            case "MAX" -> "max_over_time";
            case "MIN" -> "min_over_time";
            case "SUM" -> "sum_over_time";
            case "COUNT" -> "count_over_time";
            default -> "avg_over_time";
        };
    }
}
