package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.builder.PrometheusQueryBuilder;
import com.study.monitoring.studymonitoring.converter.*;
import com.study.monitoring.studymonitoring.mapper.StatisticsMapper;
import com.study.monitoring.studymonitoring.model.dto.request.*;
import com.study.monitoring.studymonitoring.model.dto.response.*;
import com.study.monitoring.studymonitoring.model.vo.StatisticsVO;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import com.study.monitoring.studymonitoring.service.StatisticsService;
import com.study.monitoring.studymonitoring.util.MetricUtil;
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
    private int prometheusDays;  // default: 30

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 시계열 데이터 통계 조회 (PostgreSQL + Prometheus 하이브리드 조회)
     * - 오래된 데이터(30일 이전)는 DB에서, 최신 데이터는 Prometheus에서 조회하여 병합합니다.
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

        // 1. PostgreSQL 조회 (Retention 기간 이전 데이터)
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

        // 2. Prometheus 조회 (Retention 기간 이내 데이터)
        if (requestEnd.isAfter(prometheusThreshold)) {
            LocalDateTime promStart = requestStart.isAfter(prometheusThreshold) ? requestStart : prometheusThreshold;
            long start = promStart.atZone(ZoneId.systemDefault()).toEpochSecond();
            long end = requestEnd.atZone(ZoneId.systemDefault()).toEpochSecond();

            // 기간에 따른 적절한 Step(간격) 계산
            String step = calculateStep(request.getTimePeriod(), promStart, requestEnd);

            log.info("Fetching Prometheus Data. Step: {}", step);

            List<StatisticsResponseDTO.DataPoint> promPoints = fetchRichPrometheusData(
                    request.getMetricType(),
                    request.getAggregationType(),
                    start,
                    end,
                    step,
                    request.getApplication()
            );

            allData.addAll(promPoints);
        }

        // 3. 데이터 병합 및 시간순 정렬
        allData.sort(Comparator.comparing(a -> LocalDateTime.parse(a.getTimestamp(), DATE_FORMATTER)));

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
     * Prometheus 데이터 조회 (병렬 처리)
     * - Main(선택한 집계), Min, Max 쿼리를 동시에 실행하여 Rich Data를 구성합니다.
     */
    private List<StatisticsResponseDTO.DataPoint> fetchRichPrometheusData(
            String metricType, String mainAggregationType, long start, long end, String step, String application) {

        // 1. 쿼리 생성 (메인 차트용, 최소값 밴드용, 최대값 밴드용)
        String mainQuery = PrometheusQueryBuilder.buildPrometheusQuery(metricType, mainAggregationType, step, application);
        String minQuery = PrometheusQueryBuilder.buildPrometheusQuery(metricType, "MIN", step, application);
        String maxQuery = PrometheusQueryBuilder.buildPrometheusQuery(metricType, "MAX", step, application);

        // 2. 비동기 병렬 실행 (Network I/O 대기 시간 최소화)
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

            // 3. 메인 데이터 변환
            List<StatisticsResponseDTO.DataPoint> basePoints = prometheusStatisticsConverter.convertData(
                    mainData, step, mainAggregationType
            );

            // 4. Min, Max 데이터를 Map으로 변환하여 O(1)로 조회 가능하게 처리
            Map<String, Double> minMap = extractValueMapAsString(minData);
            Map<String, Double> maxMap = extractValueMapAsString(maxData);

            // 5. DataPoint에 Min/Max 값 병합
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
            Thread.currentThread().interrupt(); // 인터럽트 상태 복구
            return Collections.emptyList();
        }
    }

    /**
     * Prometheus 응답 데이터를 { "yyyy-MM-dd HH:mm:ss": Value } 형태의 맵으로 변환
     */
    private Map<String, Double> extractValueMapAsString(List<Map<String, Object>> dataList) {
        Map<String, Double> map = new HashMap<>();
        if (dataList == null) return map;

        for (Map<String, Object> series : dataList) {
            List<List<Object>> values = (List<List<Object>>) series.get("values");
            if (values != null) {
                for (List<Object> valuePair : values) {
                    long timestampSeconds = ((Number) valuePair.get(0)).longValue();

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

    // --- Elasticsearch Log Statistics Methods ---

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
        Double successRate = MetricUtil.calculatePercentage(successCount, totalCount, 1);
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
        Double blockRate = MetricUtil.calculatePercentage(blockedAttacks, totalAttacks, 1);
        response.setBlockStats(new SecurityLogStatisticsResponseDTO.BlockStats(totalAttacks, blockedAttacks, allowedAttacks, blockRate));

        response.setDistributions(new SecurityLogsConverter().convertToSecurityDistribution(distribution));
        return response;
    }

    /**
     * 조회 기간(Duration)에 따른 적절한 Prometheus Step(간격) 계산
     * - 짧은 기간은 촘촘하게(15m), 긴 기간은 널널하게(1d) 조회하여 성능 최적화
     */
    private String calculateStep(String requestTimePeriod, LocalDateTime start, LocalDateTime end) {
        long durationMinutes = java.time.Duration.between(start, end).toMinutes();
        String unit = requestTimePeriod == null ? "AUTO" : requestTimePeriod.toUpperCase();

        switch (unit) {
            case "MINUTE":
                return durationMinutes <= 60 ? "15m" : "30m";
            case "HOUR":
                return "1h";
            case "DAY":
                return "1d";
            case "WEEK":
                return "1w";
            case "MONTH":
                return "30d";
            default:
                if (durationMinutes <= 60) return "15m";
                if (durationMinutes <= 360) return "30m";      // 6시간
                if (durationMinutes <= 1440) return "1h";      // 24시간
                if (durationMinutes <= 10080) return "6h";     // 7일
                return "1d";
        }
    }

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