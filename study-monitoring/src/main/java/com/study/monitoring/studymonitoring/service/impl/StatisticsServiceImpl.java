package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.converter.*;
import com.study.monitoring.studymonitoring.mapper.StatisticsMapper;
import com.study.monitoring.studymonitoring.model.dto.request.*;
import com.study.monitoring.studymonitoring.model.dto.response.*;
import com.study.monitoring.studymonitoring.model.vo.StatisticsVO;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import com.study.monitoring.studymonitoring.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PrometheusService prometheusService;
    private final ElasticsearchService elasticsearchService;
    private final StatisticsMapper statisticsMapper;
    private final PrometheusStatisticsConverter prometheusStatisticsConverter;
    private final LogsConverter logsConverter;
    private final AccessLogsConverter accessLogsConverter;

    @Value("${monitoring.retention.prometheus-days}")
    private int prometheusDays;  // 30

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✅ 핵심 로직: 시계열 데이터 통계 조회
     */
    @Override
    public StatisticsResponseDTO getTimeSeriesStatistics(StatisticsQueryRequestDTO request) {
        log.info("Fetching time series statistics: metric={}, start={}, end={}, period={}, aggregation={}",
                request.getMetricType(), request.getStartTime(), request.getEndTime(),
                request.getTimePeriod(), request.getAggregationType());

        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestStart = request.getStartTimeAsLocalDateTime();
        LocalDateTime requestEnd = request.getEndTimeAsLocalDateTime();
        LocalDateTime prometheusThreshold = now.minusDays(prometheusDays);

        List<StatisticsResponseDTO.DataPoint> allData = new ArrayList<>();

        // 1. PostgreSQL 조회 (30일 이전 데이터)
        if (requestStart.isBefore(prometheusThreshold)) {
            LocalDateTime dbEnd = requestEnd.isBefore(prometheusThreshold) ? requestEnd : prometheusThreshold;

            List<StatisticsVO> dbData = statisticsMapper.getStatisticsByPeriod(
                    request.getMetricType(), request.getTimePeriod(), request.getAggregationType(), requestStart, dbEnd
            );

            List<StatisticsResponseDTO.DataPoint> dbPoints = dbData.stream()
                    .map(vo -> new StatisticsResponseDTO.DataPoint(
                            vo.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond(),
                            vo.getMetricValue().doubleValue(),
                            vo.getMinValue() != null ? vo.getMinValue().doubleValue() : null,
                            vo.getMaxValue() != null ? vo.getMaxValue().doubleValue() : null,
                            vo.getSampleCount()
                    ))
                    .collect(Collectors.toList());
            allData.addAll(dbPoints);
        }

        // 2. Prometheus 조회 (30일 이내 데이터)
        if (requestEnd.isAfter(prometheusThreshold)) {
            LocalDateTime promStart = requestStart.isAfter(prometheusThreshold) ? requestStart : prometheusThreshold;
            long start = promStart.atZone(ZoneId.systemDefault()).toEpochSecond();
            long end = requestEnd.atZone(ZoneId.systemDefault()).toEpochSecond();

            // ✅ [최적화 2] Step 계산 및 불필요한 queryRange 변수 제거
            String step = calculateStep(request.getTimePeriod(), promStart, requestEnd);

            log.info("📊 Fetching Prometheus Data. Step: {}", step);

            List<StatisticsResponseDTO.DataPoint> promPoints = fetchRichPrometheusData(
                    request.getMetricType(),
                    request.getAggregationType(),
                    start,
                    end,
                    step
            );

            allData.addAll(promPoints);
        }

        // 3. 데이터 병합 및 정렬
        allData.sort((a, b) -> {
            // ✅ [최적화 1 적용] 상수 Formatter 사용
            LocalDateTime timeA = LocalDateTime.parse(a.getTimestamp(), DATE_FORMATTER);
            LocalDateTime timeB = LocalDateTime.parse(b.getTimestamp(), DATE_FORMATTER);
            return timeA.compareTo(timeB);
        });

        // 4. 응답 생성
        StatisticsResponseDTO response = new StatisticsResponseDTO();
        response.setMetricType(request.getMetricType());
        response.setTimePeriod(request.getTimePeriod());
        response.setAggregationType(request.getAggregationType());
        response.setStartTime(requestStart.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(requestEnd.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setDataSource(determineDataSource(requestStart, requestEnd, prometheusThreshold));
        response.setData(allData);

        return response;
    }

    /**
     * ✅ [신규] Prometheus에서 AVG, MIN, MAX를 병렬로 조회하여 하나의 DataPoint로 병합
     */
    private List<StatisticsResponseDTO.DataPoint> fetchRichPrometheusData(
            String metricType, String mainAggregationType, long start, long end, String step) {

        // 1. 쿼리 생성 (step을 집계 범위로 사용)
        String mainQuery = buildPrometheusQuery(metricType, mainAggregationType, step);
        String minQuery = buildPrometheusQuery(metricType, "MIN", step);
        String maxQuery = buildPrometheusQuery(metricType, "MAX", step);

        // 2. 비동기 병렬 실행
        CompletableFuture<List<Map<String, Object>>> mainFuture = CompletableFuture.supplyAsync(() ->
                prometheusService.queryRange(mainQuery, start, end, step));
        CompletableFuture<List<Map<String, Object>>> minFuture = CompletableFuture.supplyAsync(() ->
                prometheusService.queryRange(minQuery, start, end, step));
        CompletableFuture<List<Map<String, Object>>> maxFuture = CompletableFuture.supplyAsync(() ->
                prometheusService.queryRange(maxQuery, start, end, step));

        try {
            List<Map<String, Object>> mainData = mainFuture.get();
            List<Map<String, Object>> minData = minFuture.get();
            List<Map<String, Object>> maxData = maxFuture.get();

            if (mainData == null || mainData.isEmpty()) return Collections.emptyList();

            // 3. 메인 데이터 변환 (Converter에는 원본 step을 period로 전달)
            List<StatisticsResponseDTO.DataPoint> basePoints = prometheusStatisticsConverter.convertData(
                    mainData, step, mainAggregationType
            );

            // 4. Min, Max 매핑
            Map<String, Double> minMap = extractValueMapAsString(minData);
            Map<String, Double> maxMap = extractValueMapAsString(maxData);

            // 5. 값 주입
            for (StatisticsResponseDTO.DataPoint point : basePoints) {
                String key = point.getTimestamp();
                double currentVal = point.getValue();

                point.setMinValue(minMap.getOrDefault(key, currentVal));
                point.setMaxValue(maxMap.getOrDefault(key, currentVal));

                if (point.getSampleCount() == null) point.setSampleCount(1);
            }

            return basePoints;

        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to fetch rich prometheus data", e);
            Thread.currentThread().interrupt(); // 인터럽트 상태 복구 권장
            return Collections.emptyList();
        }
    }

    /**
     * ✅ [신규] Prometheus 응답 리스트를 {시간: 값} 형태의 맵으로 변환
     */
    private Map<String, Double> extractValueMapAsString(List<Map<String, Object>> dataList) {
        Map<String, Double> map = new HashMap<>();
        if (dataList == null) return map;

        for (Map<String, Object> series : dataList) {
            List<List<Object>> values = (List<List<Object>>) series.get("values");
            if (values != null) {
                for (List<Object> valuePair : values) {
                    long timestampSeconds = ((Number) valuePair.get(0)).longValue();

                    // ✅ [최적화 1 적용] 상수 Formatter 사용
                    String key = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(timestampSeconds),
                            ZoneId.systemDefault()
                    ).format(DATE_FORMATTER);

                    double value = Double.parseDouble(valuePair.get(1).toString());
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    @Override
    public LogStatisticsResponseDTO getLogStatistics(LogStatisticsQueryRequestDTO request) {
        if (!request.isValidDateFormat()) throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");
        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        Map<String, Long> logCounts = elasticsearchService.countByLogLevel("application-logs-*");
        List<Map<String, Object>> distribution = elasticsearchService.getLogDistributionByTime(
                "application-logs-*", startTime, endTime, request.getTimePeriod(), request.getLogLevel()
        );

        LogStatisticsResponseDTO response = new LogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setLogCounts(logCounts);
        response.setDistributions(logsConverter.toStatisticsDistribution(distribution));
        return response;
    }

    @Override
    public AccessLogStatisticsResponseDTO getAccessLogStatistics(AccessLogStatisticsQueryRequestDTO request) {
        log.info("Fetching access log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());
        if (!request.isValidDateFormat()) throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");

        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        Map<String, Long> methodCounts = elasticsearchService.countByHttpMethod("access-logs-*", startTime, endTime);
        Map<String, Long> statusCodeCounts = elasticsearchService.countByStatusCode("access-logs-*", startTime, endTime);
        Double avgResponseTIme = elasticsearchService.getAverageResponseTime("access-logs-*", startTime, endTime);
        List<Map<String, Object>> distribution = elasticsearchService.getAccessLogDistributionByTime(
                "access-logs-*", startTime, endTime, request.getTimePeriod()
        );

        AccessLogStatisticsResponseDTO response = new AccessLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setMethodCounts(methodCounts);
        response.setStatusCodeCounts(statusCodeCounts);
        response.setAvgResponseTime(avgResponseTIme);
        response.setDistributions(accessLogsConverter.toStatisticsDistribution(distribution));
        return response;
    }

    @Override
    public ErrorLogStatisticsResponseDTO getErrorLogStatistics(ErrorLogStatisticsQueryRequestDTO request) {
        log.info("Fetching error log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());
        if (!request.isVaildDateFormat()) throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");

        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        Map<String, Long> errorTypeCounts = elasticsearchService.countByErrorType("error-logs-*", startTime, endTime);
        Map<String, Long> severityCounts = elasticsearchService.countBySeverity("error-logs-*", startTime, endTime);
        List<Map<String, Object>> distribution = elasticsearchService.getErrorLogDistributionByTime(
                "error-logs-*", startTime, endTime, request.getTimePeriod()
        );

        ErrorLogStatisticsResponseDTO response = new ErrorLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setErrorTypeCounts(errorTypeCounts);
        response.setSeverityCounts(severityCounts);
        response.setDistributions(new ErrorLogsConverter().toStatisticsDistribution(distribution));
        return response;
    }

    @Override
    public PerformanceMetricsStatisticsResponseDTO getPerformanceMetricsStatistics(PerformanceMetricsStatisticsQueryRequestDTO request) {
        log.info("Fetching performance metrics statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());
        if (!request.isValidDateFormat()) throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");

        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        Map<String, Double> systemMetrics = elasticsearchService.getSystemMetricsAggregation("performance-metrics-*", startTime, endTime);
        Map<String, Double> jvmMetrics = elasticsearchService.getJvmMetricsAggregation("performance-metrics-*", startTime, endTime);
        List<Map<String, Object>> distribution = elasticsearchService.getPerformanceMetricsDistributionByTime(
                "performance-metrics-*", startTime, endTime, request.getTimePeriod()
        );

        PerformanceMetricsStatisticsResponseDTO response = new PerformanceMetricsStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());

        response.setSystemMetrics(new PerformanceMetricsStatisticsResponseDTO.SystemMetrics(
                systemMetrics.getOrDefault("avg_cpu", 0.0), systemMetrics.getOrDefault("avg_memory", 0.0),
                systemMetrics.getOrDefault("avg_disk", 0.0), systemMetrics.getOrDefault("max_cpu", 0.0),
                systemMetrics.getOrDefault("max_memory", 0.0)
        ));

        response.setJvmMetrics(new PerformanceMetricsStatisticsResponseDTO.JvmMetrics(
                jvmMetrics.getOrDefault("avg_heap", 0.0), jvmMetrics.getOrDefault("max_heap", 0.0),
                jvmMetrics.getOrDefault("total_gc_count", 0.0).longValue(),
                jvmMetrics.getOrDefault("total_gc_time", 0.0).longValue(),
                jvmMetrics.getOrDefault("avg_thread_count", 0.0)
        ));

        response.setDistributions(new PerformanceMetricsConverter().toStatisticsDistribution(distribution));
        return response;
    }

    @Override
    public DatabaseLogStatisticsResponseDTO getDatabaseLogStatistics(DatabaseLogStatisticsQueryRequestDTO request) {
        log.info("Fetching database log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());
        if (!request.isValidDateFormat()) throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");

        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        Map<String, Long> operationCounts = elasticsearchService.countByOperation("database-logs-*", startTime, endTime);
        Map<String, Long> tableCounts = elasticsearchService.countByTable("database-logs-*", startTime, endTime);
        Map<String, Object> queryPerformanceStats = elasticsearchService.getQueryPerformanceStats("database-logs-*", startTime, endTime);
        List<Map<String, Object>> distribution = elasticsearchService.getDatabaseLogDistributionByTime(
                "database-logs-*", startTime, endTime, request.getTimePeriod()
        );

        DatabaseLogStatisticsResponseDTO response = new DatabaseLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setOperationCounts(operationCounts);
        response.setTableCounts(tableCounts);

        response.setQueryPerformance(new DatabaseLogStatisticsResponseDTO.QueryPerformance(
                (Double) queryPerformanceStats.get("avgDuration"), (Double) queryPerformanceStats.get("maxDuration"),
                (Long) queryPerformanceStats.get("slowQueryCount"), (Long) queryPerformanceStats.get("totalQueryCount")
        ));

        response.setDistributions(new DatabaseLogsConverter().toStatisticsDistribution(distribution));
        return response;
    }

    @Override
    public AuditLogStatisticsResponseDTO getAuditLogStatistics(AuditLogStatisticsQueryRequestDTO request) {
        log.info("Fetching audit log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());
        if (!request.isValidDateFormat()) throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");

        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        Map<String, Long> eventActionCounts = elasticsearchService.countByEventAction("audit-logs-*", startTime, endTime);
        Map<String, Long> categoryCounts = elasticsearchService.countByCategory("audit-logs-*", startTime, endTime);
        Map<String, Long> eventResultCounts = elasticsearchService.countByEventResult("audit-logs-*", startTime, endTime);
        List<Map<String, Object>> distribution = elasticsearchService.getAuditLogDistributionByTime(
                "audit-logs-*", startTime, endTime, request.getTimePeriod()
        );

        AuditLogStatisticsResponseDTO response = new AuditLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setEventActionCounts(eventActionCounts);
        response.setCategoryCounts(categoryCounts);

        Long successCount = eventResultCounts.getOrDefault("success", 0L);
        Long failureCount = eventResultCounts.getOrDefault("failure", 0L);
        Long totalCount = successCount + failureCount;
        Double successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0.0;
        response.setResultStats(new AuditLogStatisticsResponseDTO.ResultStats(successCount, failureCount, successRate));

        response.setDistributions(new AuditLogsConverter().toStatisticsDistribution(distribution));
        return response;
    }

    @Override
    public SecurityLogStatisticsResponseDTO getSecurityLogStatistics(SecurityLogStatisticsQueryRequestDTO request) {
        log.info("Fetching security log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());
        if (!request.isVaildDateFormat()) throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");

        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        Map<String, Long> threatLevelCounts = elasticsearchService.countByThreatLevel("security-logs-*", startTime, endTime);
        Map<String, Long> attackTypeCounts = elasticsearchService.countByAttackType("security-logs-*", startTime, endTime);
        Map<String, Long> blockStatistics = elasticsearchService.getBlockStatistics("security-logs-*", startTime, endTime);
        List<Map<String, Object>> distribution = elasticsearchService.getSecurityLogDistributionByTime(
                "security-logs-*", startTime, endTime, request.getTimePeriod()
        );

        SecurityLogStatisticsResponseDTO response = new SecurityLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setThreatLevelCounts(threatLevelCounts);
        response.setAttackTypeCounts(attackTypeCounts);

        Long totalAttacks = blockStatistics.getOrDefault("totalAttacks", 0L);
        Long blockedAttacks = blockStatistics.getOrDefault("blockedAttacks", 0L);
        Long allowedAttacks = blockStatistics.getOrDefault("allowedAttacks", 0L);
        Double blockRate = totalAttacks > 0 ? (blockedAttacks * 100.0 / totalAttacks) : 0.0;
        response.setBlockStats(new SecurityLogStatisticsResponseDTO.BlockStats(totalAttacks, blockedAttacks, allowedAttacks, blockRate));

        response.setDistributions(new SecurityLogsConverter().convertToSecurityDistribution(distribution));
        return response;
    }

    /**
     * ✅ Prometheus Query Builder
     * - 수정: 조회하려는 통계 타입(AVG, MAX, MIN)에 맞춰 내부 인스턴스 합산 방식도 동적으로 변경
     */
    private String buildPrometheusQuery(String metricType, String aggregationType, String step) {
        // 1. 시간 집계 함수 (예: avg_over_time)
        String timeAggFunc = convertToPrometheusFunction(aggregationType);

        // 2. [NEW] 공간 집계 함수 (예: avg, max, min) - 인스턴스 간 병합용
        String spaceAggFunc = convertToSpatialFunction(aggregationType);

        String resolution = "1m";

        // 1. CPU_USAGE
        if ("CPU_USAGE".equalsIgnoreCase(metricType)) {
            // ✅ 핵심 변경:
            // 사용자가 MAX를 원하면 max(process_cpu_usage)를 하여 가장 바쁜 서버를 찾고,
            // 사용자가 AVG를 원하면 avg(process_cpu_usage)를 하여 전체 평균을 찾음.
            return String.format("%s((%s(process_cpu_usage))[%s:%s]) * 100",
                    timeAggFunc, spaceAggFunc, step, resolution);
        }

        // 2. HEAP_USAGE (여기는 sum으로 전체 용량을 합치는 게 맞으므로 그대로 둠)
        if ("HEAP_USAGE".equalsIgnoreCase(metricType)) {
            String heapExpr = "(sum(jvm_memory_used_bytes{area=\"heap\"}) / sum(jvm_memory_max_bytes{area=\"heap\"}))";
            return String.format("%s((%s)[%s:%s]) * 100", timeAggFunc, heapExpr, step, resolution);
        }

        // 3. Counter 형 메트릭
        if (isCounterMetric(metricType)) {
            String baseRate = getRateExpression(metricType, resolution);
            return switch (aggregationType.toUpperCase()) {
                case "AVG" -> String.format("avg_over_time((%s)[%s:%s])", baseRate, step, resolution);
                case "SUM" -> getIncreaseExpression(metricType, step);
                case "MAX" -> String.format("max_over_time((%s)[%s:%s])", baseRate, step, resolution);
                case "MIN" -> String.format("min_over_time((%s)[%s:%s])", baseRate, step, resolution);
                default -> String.format("avg_over_time((%s)[%s:%s])", baseRate, step, resolution);
            };
        }

        // 4. 기타 Gauge
        String metricName = metricType.toLowerCase();
        // 기타 메트릭도 통계 타입에 맞춰 합산
        return String.format("%s((%s(%s))[%s:%s])", timeAggFunc, spaceAggFunc, metricName, step, resolution);
    }

    /**
     * [신규] 통계 타입에 따른 Prometheus 공간 집계 함수 매핑
     * 여러 인스턴스의 값을 하나로 합칠 때 사용 (AVG -> avg, MAX -> max, MIN -> min)
     */
    private String convertToSpatialFunction(String aggregationType) {
        return switch (aggregationType.toUpperCase()) {
            case "MAX" -> "max";
            case "MIN" -> "min";
            case "SUM" -> "sum"; // CPU 등에서는 잘 안 쓰지만 논리상 sum
            case "COUNT" -> "count";
            default -> "avg"; // 기본은 평균
        };
    }

    private String convertToPrometheusFunction(String aggregationType) {
        return switch (aggregationType.toUpperCase()) {
            case "AVG" -> "avg_over_time";
            case "MAX" -> "max_over_time";
            case "MIN" -> "min_over_time";
            case "SUM" -> "sum_over_time";
            case "COUNT" -> "count_over_time";
            default -> "avg_over_time";
        };
    }

    // 메트릭 타입이 Counter(증가형)인지 판별
    private boolean isCounterMetric(String metricType) {
        return "TPS".equalsIgnoreCase(metricType) || "ERROR_RATE".equalsIgnoreCase(metricType);
    }

    // Counter형 메트릭의 Rate 표현식 생성
    private String getRateExpression(String metricType, String window) {
        if ("TPS".equalsIgnoreCase(metricType)) {
            return String.format("sum(rate(http_server_requests_seconds_count[%s]))", window);
        } else if ("ERROR_RATE".equalsIgnoreCase(metricType)) {
            // 에러율 계산
            return String.format(
                    "(sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[%s])) / sum(rate(http_server_requests_seconds_count[%s]))) * 100",
                    window, window
            );
        }
        return "";
    }

    // Counter형 메트릭의 Increase(총 증가량) 표현식 생성
    private String getIncreaseExpression(String metricType, String window) {
        if ("TPS".equalsIgnoreCase(metricType)) {
            // TPS의 합계 = 총 요청 수
            return String.format("sum(increase(http_server_requests_seconds_count[%s]))", window);
        } else if ("ERROR_RATE".equalsIgnoreCase(metricType)) {
            // 에러율의 합계? (의미가 모호하지만 에러 건수로 처리 가능, 여기선 단순 rate sum으로 처리하거나 increase 사용)
            return String.format("sum(increase(http_server_requests_seconds_count{status=~\"5..\"}[%s]))", window);
        }
        return "";
    }

    // Gauge형 메트릭 이름 반환
    private String getGaugeMetricName(String metricType) {
        // CPU, HEAP 로직은 buildPrometheusQuery로 이동했으므로 단순 소문자 변환
        return metricType.toLowerCase();
    }

    /**
     * 시간 범위 및 선택된 주기에 따른 Step 계산
     * * 요청 사항:
     * 1. '분(MINUTE)' 선택 시: 1시간 이내는 15분, 그 이상(6시간~30일)은 30분 간격
     * 2. '시간(HOUR)' 선택 시: 기간 상관없이 무조건 1시간 간격
     * 3. '일(DAY)' 선택 시: 기간 상관없이 무조건 1일 간격
     */
    private String calculateStep(String requestTimePeriod, LocalDateTime start, LocalDateTime end) {
        long durationMinutes = java.time.Duration.between(start, end).toMinutes();

        // 대소문자 무시 처리를 위해 대문자로 변환 (null 방지)
        String unit = requestTimePeriod == null ? "AUTO" : requestTimePeriod.toUpperCase();

        switch (unit) {
            case "MINUTE": // 🔹 사용자가 '분' 단위를 선택한 경우
                if (durationMinutes <= 60) {
                    return "15m"; // 최근 1시간 이내 -> 15분 간격
                }
                // 최근 6시간, 24시간, 7일, 30일 등 1시간을 넘어가면 -> 30분 간격
                return "30m";

            case "HOUR":   // 🔹 사용자가 '시간' 단위를 선택한 경우
                return "1h";  // 기간 상관없이 무조건 1시간 간격 (1시간 조회시 점 1개, 24시간 조회시 점 24개)

            case "DAY":    // 🔹 사용자가 '일' 단위를 선택한 경우
                return "1d";  // 기간 상관없이 무조건 1일 간격 (1시간 조회시 데이터 없음/1개, 7일 조회시 점 7개)

            case "WEEK":
                return "1w";

            case "MONTH":
                return "30d";

            default:
                // 🔹 단위(Unit)를 선택하지 않고 기간만 보냈을 때의 자동 로직 (Fallback)
                if (durationMinutes <= 60) return "15m";
                if (durationMinutes <= 60 * 6) return "30m";
                if (durationMinutes <= 60 * 24) return "1h";
                if (durationMinutes <= 60 * 24 * 7) return "6h";
                return "1d";
        }
    }

    /**
     * 데이터 소스 판별
     */
    private String determineDataSource(LocalDateTime start, LocalDateTime end, LocalDateTime threshold) {
        boolean hasPrometheus = end.isAfter(threshold);
        boolean hasPostgres = start.isBefore(threshold);

        if (hasPrometheus && hasPostgres) {
            return "MIXED";
        } else if (hasPrometheus) {
            return "PROMETHEUS";
        } else {
            return "POSTGRESQL";
        }
    }
}