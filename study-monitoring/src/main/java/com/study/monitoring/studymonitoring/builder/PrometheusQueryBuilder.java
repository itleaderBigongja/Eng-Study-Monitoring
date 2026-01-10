package com.study.monitoring.studymonitoring.builder;

import org.springframework.stereotype.Component;

@Component
public class PrometheusQueryBuilder {

    public static String buildPrometheusQuery(String metricType, String aggregationType, String step, String application) {
        String timeAggFunc = convertToPrometheusFunction(aggregationType);
        String spaceAggFunc = convertToSpatialFunction(aggregationType);
        String resolution = "1m";

        String selector = (application != null && !application.isBlank())
                ? String.format("{application=\"%s\"}", application)
                : "";

        String upCheck = String.format("(max(up%s) or vector(0))", selector);
        String baseQuery = "";

        // Case 1: CPU Usage
        if ("CPU_USAGE".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((%s(process_cpu_usage%s))[%s:%s]) * 100",
                    timeAggFunc, spaceAggFunc, selector, step, resolution);
        }
        // Case 2: Heap Usage (핵심 수정!)
        else if ("HEAP_USAGE".equalsIgnoreCase(metricType)) {
            String heapSelector = selector.isEmpty()
                    ? "{area=\"heap\"}"
                    : selector.replace("}", ", area=\"heap\"}");

            // ✅ [핵심 수정] sum by (application) - id 라벨 제거하고 합산
            String heapUsedQuery = String.format(
                    "sum by (application) (jvm_memory_used_bytes%s)",
                    heapSelector
            );

            // max가 -1이면 0으로 변환 후 committed 사용
            String heapMaxQuery = String.format(
                    "((sum by (application) (jvm_memory_max_bytes%s) > 0) * sum by (application) (jvm_memory_max_bytes%s) or sum by (application) (jvm_memory_committed_bytes%s))",
                    heapSelector, heapSelector, heapSelector
            );

            // 비율 계산
            String heapRatio = String.format(
                    "(%s / clamp_min(%s, 1))",
                    heapUsedQuery,
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
        // Case 4: DB Connections
        else if ("DB_CONNECTIONS".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((sum(pg_stat_activity_count%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        // Case 5: DB Size
        else if ("DB_SIZE".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((sum(pg_database_size_bytes%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        // Case 6: DB Transactions
        else if ("DB_TRANSACTIONS".equalsIgnoreCase(metricType)) {
            String query = String.format("sum(rate(pg_stat_database_xact_commit%s[%s])) + sum(rate(pg_stat_database_xact_rollback%s[%s]))",
                    selector, resolution, selector, resolution);
            baseQuery = String.format("avg_over_time((%s)[%s:%s])", query, step, resolution);
        }
        // Case 7: ES JVM Heap (동일한 로직 적용)
        else if ("ES_JVM_HEAP".equalsIgnoreCase(metricType)) {
            String esSelector = selector.isEmpty()
                    ? "{area=\"heap\"}"
                    : selector.replace("}", ", area=\"heap\"}");

            // ✅ sum by (application) 적용
            String esHeapUsed = String.format(
                    "sum by (application) (elasticsearch_jvm_memory_used_bytes%s)",
                    esSelector
            );

            String esHeapMax = String.format(
                    "((sum by (application) (elasticsearch_jvm_memory_max_bytes%s) > 0) * sum by (application) (elasticsearch_jvm_memory_max_bytes%s) or sum by (application) (elasticsearch_jvm_memory_committed_bytes%s))",
                    esSelector, esSelector, esSelector
            );

            String esHeapRatio = String.format(
                    "(%s / clamp_min(%s, 1))",
                    esHeapUsed,
                    esHeapMax
            );

            baseQuery = String.format("%s((%s)[%s:%s]) * 100",
                    timeAggFunc, esHeapRatio, step, resolution);
        }
        // Case 8: ES Data Size
        else if ("ES_DATA_SIZE".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((sum(elasticsearch_indices_store_size_bytes%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        // Case 9: ES CPU
        else if ("ES_CPU".equalsIgnoreCase(metricType)) {
            baseQuery = String.format("%s((avg(elasticsearch_process_cpu_percent%s))[%s:%s])", timeAggFunc, selector, step, resolution);
        }
        // Default case
        else {
            String metricName = metricType.toLowerCase();
            baseQuery = String.format("%s((%s(%s%s))[%s:%s])", timeAggFunc, spaceAggFunc, metricName, selector, step, resolution);
        }

        // UP 상태 체크
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
                    "(sum(rate(http_server_requests_seconds_count%s[%s])) / clamp_min(sum(rate(http_server_requests_seconds_count%s[%s])), 0.001)) * 100",
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