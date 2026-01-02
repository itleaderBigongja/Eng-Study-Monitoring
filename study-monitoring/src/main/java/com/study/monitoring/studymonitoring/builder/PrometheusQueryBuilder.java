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

        // Case 1: CPU Usage
        if ("CPU_USAGE".equalsIgnoreCase(metricType)) {
            // 예: process_cpu_usage{application="eng-study"}
            return String.format("%s((%s(process_cpu_usage%s))[%s:%s]) * 100",
                    timeAggFunc, spaceAggFunc, selector, step, resolution);
        }

        // Case 2: Heap Usage
        if ("HEAP_USAGE".equalsIgnoreCase(metricType)) {
            // Heap은 area="heap" 조건이 필수이므로, selector와 합쳐야 함
            // 예: jvm_memory_used_bytes{application="eng-study", area="heap"}
            String innerSelector = selector.isEmpty() ? "{area=\"heap\"}" : selector.replace("}", ", area=\"heap\"}");
            String heapExpr = String.format("(sum(jvm_memory_used_bytes%s) / sum(jvm_memory_max_bytes%s))", innerSelector, innerSelector);
            return String.format("%s((%s)[%s:%s]) * 100", timeAggFunc, heapExpr, step, resolution);
        }

        // Case 3: Counter Metrics (TPS, Error Rate)
        if (isCounterMetric(metricType)) {
            String baseRate = getRateExpression(metricType, resolution, selector);
            if ("SUM".equalsIgnoreCase(aggregationType)) {
                return getIncreaseExpression(metricType, step, selector);
            }

            return switch (aggregationType.toUpperCase()) {
                case "AVG" -> String.format("avg_over_time((%s)[%s:%s])", baseRate, step, resolution);
                case "MAX" -> String.format("max_over_time((%s)[%s:%s])", baseRate, step, resolution);
                case "MIN" -> String.format("min_over_time((%s)[%s:%s])", baseRate, step, resolution);
                default -> String.format("avg_over_time((%s)[%s:%s])", baseRate, step, resolution);
            };
        }

        // --- 🐘 PostgreSQL 메트릭 ---
        // 1. 활성 연결 수 (Connections)
        if ("DB_CONNECTIONS".equalsIgnoreCase(metricType)) {
            return String.format("%s((sum(pg_stat_activity_count%s))[%s:%s])",
                    timeAggFunc, selector, step, resolution);
        }

        // 2. DB 사이즈 (Bytes -> MB 변환 등은 프론트에서 하거나 여기서 /1024/1024)
        if ("DB_SIZE".equalsIgnoreCase(metricType)) {
            return String.format("%s((sum(pg_database_size_bytes%s))[%s:%s])",
                    timeAggFunc, selector, step, resolution);
        }

        // 3. 트랜잭션 수 (Commit + Rollback) - Counter 타입이라 rate 적용
        if ("DB_TRANSACTIONS".equalsIgnoreCase(metricType)) {
            String query = String.format("sum(rate(pg_stat_database_xact_commit%s[%s])) + sum(rate(pg_stat_database_xact_rollback%s[%s]))",
                    selector, resolution, selector, resolution);
            return String.format("avg_over_time((%s)[%s:%s])", query, step, resolution);
        }

        // --- 🔍 Elasticsearch 메트릭 ---
        // 1. ES JVM Heap 사용률 (ES도 Java 기반)
        if ("ES_JVM_HEAP".equalsIgnoreCase(metricType)) {
            String esSelector = selector.isEmpty() ? "{area=\"heap\"}" : selector.replace("}", ", area=\"heap\"}");
            String heapExpr = String.format("(sum(elasticsearch_jvm_memory_used_bytes%s) / sum(elasticsearch_jvm_memory_max_bytes%s))", esSelector, esSelector);
            return String.format("%s((%s)[%s:%s]) * 100", timeAggFunc, heapExpr, step, resolution);
        }

        // 2. 데이터 크기 (Index Size) : indices_store_size_bytes -> 실제 인덱스 데이터 용량 (KB ~ MB 단위 예상)
        // 'sum'을 해야 모든 인덱스(primary + replica)의 합계를 보여줍니다.
        if ("ES_DATA_SIZE".equalsIgnoreCase(metricType)) {
            return String.format("%s((sum(elasticsearch_indices_store_size_bytes%s))[%s:%s])",
                    timeAggFunc, selector, step, resolution);
        }

        if ("ES_CPU".equalsIgnoreCase(metricType)) {
            return String.format("%s((avg(elasticsearch_process_cpu_percent%s))[%s:%s])",
                    timeAggFunc, selector, step, resolution);
        }

        // Default
        String metricName = metricType.toLowerCase();
        return String.format("%s((%s(%s%s))[%s:%s])", timeAggFunc, spaceAggFunc, metricName, selector, step, resolution);
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
