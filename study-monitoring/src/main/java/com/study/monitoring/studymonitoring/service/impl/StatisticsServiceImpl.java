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
import java.time.temporal.ChronoUnit;
import java.util.*;
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

    /**
     * ✅ 핵심 로직: 시계열 데이터 통계 조회
     *
     * 동작 방식:
     * 1. 조회 기간을 30일 기준으로 분리
     * 2. 30일 이내: Prometheus 조회
     * 3. 30일 이후: PostgreSQL 조회
     * 4. 두 데이터를 병합
     */
    @Override
    public StatisticsResponseDTO getTimeSeriesStatistics(StatisticsQueryRequestDTO request) {
        log.info("Fetching time series statistics: metric={}, start={}, end={}, period={}, aggregation={}",
                request.getMetricType(), request.getStartTime(), request.getEndTime(),
                request.getTimePeriod(), request.getAggregationType());

        // 날짜 형식 검증
        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException(
                    "날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime prometheusThreshold = now.minusDays(prometheusDays);  // 30일

        // String -> LocalDateTime 변환
        LocalDateTime requestStart = request.getStartTimeAsLocalDateTime();
        LocalDateTime requestEnd = request.getEndTimeAsLocalDateTime();

        List<StatisticsResponseDTO.DataPoint> allData = new ArrayList<>();

        // 1. PostgreSQL 조회(30일 이후 데이터)
        if (requestStart.isBefore(prometheusThreshold)) {
            LocalDateTime dbEnd = requestEnd.isBefore(prometheusThreshold)
                    ? requestEnd
                    : prometheusThreshold;

            log.debug("Querying PostgreSQL: {} ~ {}", requestStart, dbEnd);
            List<StatisticsVO> dbData = statisticsMapper.getStatisticsByPeriod(
                    request.getMetricType(),
                    request.getTimePeriod(),
                    request.getAggregationType(),
                    requestStart,
                    dbEnd
            );

            // VO -> DataPoint 변환(Long timestamp 전달)
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

        // 2. Prometheus 조회( 30일 이내 데이터 )
        if (requestEnd.isAfter(prometheusThreshold)) {
            LocalDateTime promStart = requestStart.isAfter(prometheusThreshold)
                    ? requestStart
                    : prometheusThreshold;

            log.debug("Querying Prometheus: {} ~ {}", promStart, requestEnd);

            long start = promStart.atZone(ZoneId.systemDefault()).toEpochSecond();
            long end = requestEnd.atZone(ZoneId.systemDefault()).toEpochSecond();
            String step = calculateStep(request.getTimePeriod());

            String promQuery = buildPrometheusQuery(
                    request.getMetricType(),
                    request.getAggregationType(),
                    step
            );

            List<Map<String, Object>> promData = prometheusService.queryRange(
                    promQuery, start, end, step
            );

            List<StatisticsResponseDTO.DataPoint> promPoints =
                    prometheusStatisticsConverter.convertData(
                            promData,
                            request.getTimePeriod(),
                            request.getAggregationType()
                    );

            allData.addAll(promPoints);
        }

        // ✅ 3️⃣ 데이터 병합 및 정렬 (timestamp는 이미 String이므로 비교 불가)
        // DataPoint의 timestamp를 LocalDateTime으로 파싱해서 정렬
        allData.sort((a, b) -> {
            LocalDateTime timeA = LocalDateTime.parse(a.getTimestamp(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime timeB = LocalDateTime.parse(b.getTimestamp(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return timeA.compareTo(timeB);
        });

        // ✅ 4️⃣ 응답 생성
        StatisticsResponseDTO response = new StatisticsResponseDTO();
        response.setMetricType(request.getMetricType());
        response.setTimePeriod(request.getTimePeriod());
        response.setAggregationType(request.getAggregationType());

        // ✅ Long timestamp를 전달하면 setStartTime/setEndTime에서 String으로 변환됨
        response.setStartTime(requestStart.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(requestEnd.atZone(ZoneId.systemDefault()).toEpochSecond());

        response.setDataSource(determineDataSource(requestStart, requestEnd, prometheusThreshold));
        response.setData(allData);

        log.info("Statistics query completed: {} data points", allData.size());

        return response;
    }

    /**
     * ✅ 핵심 로직: 로그 Application 통계 조회 (Elasticsearch)
     */
    @Override
    public LogStatisticsResponseDTO getLogStatistics(LogStatisticsQueryRequestDTO request) {
        log.info("Fetching log statistics: start={}, end={}, period={}, logLevel={}",
                request.getStartTime(), request.getEndTime(),
                request.getTimePeriod(), request.getLogLevel());

        // 날짜 형식 검증
        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException(
                    "날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss"
            );
        }

        // String -> LocalDateTime 변환
        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        // 로그 레벨별 카운트 조회
        Map<String, Long> logCounts = elasticsearchService.countByLogLevel("application-logs-*");

        // 시간대 별 로그 분포
        List<Map<String, Object>> distribution = elasticsearchService.getLogDistributionByTime(
                "application-logs-*",
                startTime,
                endTime,
                request.getTimePeriod(),
                request.getLogLevel()
        );

        // 응답 생성
        LogStatisticsResponseDTO response = new LogStatisticsResponseDTO();

        // LocalDateTime -> Unix timestamp -> String 변환
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setLogCounts(logCounts);
        response.setDistributions(logsConverter.toStatisticsDistribution(distribution));

        return response;
    }

    /** access-logs 통계 조회 */
    @Override
    public AccessLogStatisticsResponseDTO getAccessLogStatistics(AccessLogStatisticsQueryRequestDTO request) {
        log.info("Fetching access log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());

        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss");
        }

        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        /** HTTP 메서드 별 카운트 */
        Map<String, Long> methodCounts = elasticsearchService.countByHttpMethod("access-logs-*", startTime, endTime);

        /** 상태 코드 별 카운트 */
        Map<String, Long> statusCodeCounts = elasticsearchService.countByStatusCode("access-logs-*", startTime, endTime);

        /** 평균 응답시간 */
        Double avgResponseTIme = elasticsearchService.getAverageResponseTime("access-logs-*", startTime, endTime);

        /** 시간대 별 분포 */
        List<Map<String, Object>> distribution = elasticsearchService.getAccessLogDistributionByTime(
                "access-logs-*", startTime, endTime, request.getTimePeriod()
        );

        // 응답이 생성
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

        // 날짜 형식 검증
        if (!request.isVaildDateFormat()) {
            throw new IllegalArgumentException(
                    "날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss"
            );
        }

        // String -> LocalDateTime 변환
        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        // 에러 타입별 카운트
        Map<String, Long> errorTypeCounts = elasticsearchService.countByErrorType(
                "error-logs-*",
                startTime,
                endTime
        );

        // 심각도별 카운트
        Map<String, Long> severityCounts = elasticsearchService.countBySeverity(
                "error-logs-*",
                startTime,
                endTime
        );

        // 시간대별 분포
        List<Map<String, Object>> distribution = elasticsearchService.getErrorLogDistributionByTime(
                "error-logs-*",
                startTime,
                endTime,
                request.getTimePeriod()
        );

        // 응답 생성
        ErrorLogStatisticsResponseDTO response = new ErrorLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setErrorTypeCounts(errorTypeCounts);
        response.setSeverityCounts(severityCounts);

        // Converter를 통한 분포 변환
        ErrorLogsConverter errorLogsConverter = new ErrorLogsConverter();
        response.setDistributions(errorLogsConverter.toStatisticsDistribution(distribution));

        return response;
    }

    @Override
    public PerformanceMetricsStatisticsResponseDTO getPerformanceMetricsStatistics(
            PerformanceMetricsStatisticsQueryRequestDTO request) {

        log.info("Fetching performance metrics statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());

        // 날짜 형식 검증
        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException(
                    "날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss"
            );
        }

        // String -> LocalDateTime 변환
        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        // 시스템 메트릭 집계
        Map<String, Double> systemMetrics = elasticsearchService.getSystemMetricsAggregation(
                "performance-metrics-*",
                startTime,
                endTime
        );

        // JVM 메트릭 집계
        Map<String, Double> jvmMetrics = elasticsearchService.getJvmMetricsAggregation(
                "performance-metrics-*",
                startTime,
                endTime
        );

        // 시간대별 분포
        List<Map<String, Object>> distribution = elasticsearchService.getPerformanceMetricsDistributionByTime(
                "performance-metrics-*",
                startTime,
                endTime,
                request.getTimePeriod()
        );

        // 응답 생성
        PerformanceMetricsStatisticsResponseDTO response = new PerformanceMetricsStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());

        // [수정 1] SystemMetrics 안전하게 생성 (getOrDefault 사용)
        PerformanceMetricsStatisticsResponseDTO.SystemMetrics systemMetricsObj =
                new PerformanceMetricsStatisticsResponseDTO.SystemMetrics(
                        systemMetrics.getOrDefault("avg_cpu", 0.0),
                        systemMetrics.getOrDefault("avg_memory", 0.0),
                        systemMetrics.getOrDefault("avg_disk", 0.0),
                        systemMetrics.getOrDefault("max_cpu", 0.0),
                        systemMetrics.getOrDefault("max_memory", 0.0)
                );
        response.setSystemMetrics(systemMetricsObj);

        // [수정 2] JvmMetrics 안전하게 생성 (NullPointerException 방지)
        // .longValue() 호출 전 null 체크가 필수이므로 getOrDefault를 꼭 써야 합니다.
        PerformanceMetricsStatisticsResponseDTO.JvmMetrics jvmMetricsObj =
                new PerformanceMetricsStatisticsResponseDTO.JvmMetrics(
                        jvmMetrics.getOrDefault("avg_heap", 0.0),
                        jvmMetrics.getOrDefault("max_heap", 0.0),
                        jvmMetrics.getOrDefault("total_gc_count", 0.0).longValue(), // null이면 0.0 -> 0L
                        jvmMetrics.getOrDefault("total_gc_time", 0.0).longValue(),  // null이면 0.0 -> 0L
                        jvmMetrics.getOrDefault("avg_thread_count", 0.0)
                );
        response.setJvmMetrics(jvmMetricsObj);

        // Converter를 통한 분포 변환
        PerformanceMetricsConverter performanceMetricsConverter = new PerformanceMetricsConverter();
        response.setDistributions(performanceMetricsConverter.toStatisticsDistribution(distribution));

        return response;
    }

    @Override
    public DatabaseLogStatisticsResponseDTO getDatabaseLogStatistics(
            DatabaseLogStatisticsQueryRequestDTO request) {

        log.info("Fetching database log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());

        // 날짜 형식 검증
        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException(
                    "날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss"
            );
        }

        // String -> LocalDateTime 변환
        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        // Operation별 카운트
        Map<String, Long> operationCounts = elasticsearchService.countByOperation(
                "database-logs-*",
                startTime,
                endTime
        );

        // 테이블별 카운트
        Map<String, Long> tableCounts = elasticsearchService.countByTable(
                "database-logs-*",
                startTime,
                endTime
        );

        // 쿼리 성능 지표
        Map<String, Object> queryPerformanceStats = elasticsearchService.getQueryPerformanceStats(
                "database-logs-*",
                startTime,
                endTime
        );

        // 시간대별 분포
        List<Map<String, Object>> distribution = elasticsearchService.getDatabaseLogDistributionByTime(
                "database-logs-*",
                startTime,
                endTime,
                request.getTimePeriod()
        );

        // 응답 생성
        DatabaseLogStatisticsResponseDTO response = new DatabaseLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setOperationCounts(operationCounts);
        response.setTableCounts(tableCounts);

        // QueryPerformance 객체 생성
        DatabaseLogStatisticsResponseDTO.QueryPerformance queryPerformance =
                new DatabaseLogStatisticsResponseDTO.QueryPerformance(
                        (Double) queryPerformanceStats.get("avgDuration"),
                        (Double) queryPerformanceStats.get("maxDuration"),
                        (Long) queryPerformanceStats.get("slowQueryCount"),
                        (Long) queryPerformanceStats.get("totalQueryCount")
                );
        response.setQueryPerformance(queryPerformance);

        // Converter를 통한 분포 변환
        DatabaseLogsConverter databaseLogsConverter = new DatabaseLogsConverter();
        response.setDistributions(databaseLogsConverter.toStatisticsDistribution(distribution));

        return response;
    }

    @Override
    public AuditLogStatisticsResponseDTO getAuditLogStatistics(
            AuditLogStatisticsQueryRequestDTO request) {

        log.info("Fetching audit log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());

        // 날짜 형식 검증
        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException(
                    "날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss"
            );
        }

        // String -> LocalDateTime 변환
        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        // 이벤트 액션별 카운트
        Map<String, Long> eventActionCounts = elasticsearchService.countByEventAction(
                "audit-logs-*",
                startTime,
                endTime
        );

        // 카테고리별 카운트
        Map<String, Long> categoryCounts = elasticsearchService.countByCategory(
                "audit-logs-*",
                startTime,
                endTime
        );

        // 성공/실패 비율
        Map<String, Long> eventResultCounts = elasticsearchService.countByEventResult(
                "audit-logs-*",
                startTime,
                endTime
        );

        // 시간대별 분포
        List<Map<String, Object>> distribution = elasticsearchService.getAuditLogDistributionByTime(
                "audit-logs-*",
                startTime,
                endTime,
                request.getTimePeriod()
        );

        // 응답 생성
        AuditLogStatisticsResponseDTO response = new AuditLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setEventActionCounts(eventActionCounts);
        response.setCategoryCounts(categoryCounts);

        // ResultStats 계산
        Long successCount = eventResultCounts.getOrDefault("success", 0L);
        Long failureCount = eventResultCounts.getOrDefault("failure", 0L);
        Long totalCount = successCount + failureCount;
        Double successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0.0;

        AuditLogStatisticsResponseDTO.ResultStats resultStats =
                new AuditLogStatisticsResponseDTO.ResultStats(
                        successCount,
                        failureCount,
                        successRate
                );
        response.setResultStats(resultStats);

        // Converter를 통한 분포 변환
        AuditLogsConverter auditLogsConverter = new AuditLogsConverter();
        response.setDistributions(auditLogsConverter.toStatisticsDistribution(distribution));

        return response;
    }

    @Override
    public SecurityLogStatisticsResponseDTO getSecurityLogStatistics(
            SecurityLogStatisticsQueryRequestDTO request) {

        log.info("Fetching security log statistics: start={}, end={}, period={}",
                request.getStartTime(), request.getEndTime(), request.getTimePeriod());

        // 날짜 형식 검증
        if (!request.isVaildDateFormat()) {
            throw new IllegalArgumentException(
                    "날짜 형식이 올바르지 않습니다. 형식: yyyy-MM-dd HH:mm:ss"
            );
        }

        // String -> LocalDateTime 변환
        LocalDateTime startTime = request.getStartTimeAsLocalDateTime();
        LocalDateTime endTime = request.getEndTimeAsLocalDateTime();

        // 위협 레벨별 카운트
        Map<String, Long> threatLevelCounts = elasticsearchService.countByThreatLevel(
                "security-logs-*",
                startTime,
                endTime
        );

        // 공격 타입별 카운트
        Map<String, Long> attackTypeCounts = elasticsearchService.countByAttackType(
                "security-logs-*",
                startTime,
                endTime
        );

        // 차단 통계
        Map<String, Long> blockStatistics = elasticsearchService.getBlockStatistics(
                "security-logs-*",
                startTime,
                endTime
        );

        // 시간대별 분포
        List<Map<String, Object>> distribution = elasticsearchService.getSecurityLogDistributionByTime(
                "security-logs-*",
                startTime,
                endTime,
                request.getTimePeriod()
        );

        // 응답 생성
        SecurityLogStatisticsResponseDTO response = new SecurityLogStatisticsResponseDTO();
        response.setStartTime(startTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setEndTime(endTime.atZone(ZoneId.systemDefault()).toEpochSecond());
        response.setTimePeriod(request.getTimePeriod());
        response.setThreatLevelCounts(threatLevelCounts);
        response.setAttackTypeCounts(attackTypeCounts);

        // BlockStats 계산
        Long totalAttacks = blockStatistics.getOrDefault("totalAttacks", 0L);
        Long blockedAttacks = blockStatistics.getOrDefault("blockedAttacks", 0L);
        Long allowedAttacks = blockStatistics.getOrDefault("allowedAttacks", 0L);
        Double blockRate = totalAttacks > 0 ? (blockedAttacks * 100.0 / totalAttacks) : 0.0;

        SecurityLogStatisticsResponseDTO.BlockStats blockStats =
                new SecurityLogStatisticsResponseDTO.BlockStats(
                        totalAttacks,
                        blockedAttacks,
                        allowedAttacks,
                        blockRate
                );
        response.setBlockStats(blockStats);

        // Converter를 통한 분포 변환
        SecurityLogsConverter securityLogsConverter = new SecurityLogsConverter();
        response.setDistributions(securityLogsConverter.convertToSecurityDistribution(distribution));

        return response;
    }

    /**
     * Prometheus 쿼리 빌드 (수정됨)
     *
     * ⚠️ 중요:
     * - range query에서는 step 파라미터로 시간 간격 지정
     * - 쿼리 자체에는 range를 중첩하지 않음
     */
    private String buildPrometheusQuery(String metricType, String aggregationType, String step) {
        // 1. 기본 메트릭 쿼리 (instant vector 형태)
        String baseQuery = switch (metricType.toLowerCase()) {
            case "tps" -> "rate(http_server_requests_seconds_count[5m])";
            case "heap_usage" -> "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"} * 100";
            case "error_rate" -> "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m]) / rate(http_server_requests_seconds_count[5m]) * 100";
            case "cpu_usage" -> "process_cpu_usage * 100";
            default -> throw new IllegalArgumentException("Unknown metric: " + metricType);
        };

        // 2. 집계 함수 적용
        // ⚠️ range query에서는 _over_time 함수를 사용하지 않음
        // step 파라미터가 이미 시간 간격을 지정하기 때문
        return switch (aggregationType.toLowerCase()) {
            case "avg" -> String.format("avg(%s)", baseQuery);
            case "sum" -> String.format("sum(%s)", baseQuery);
            case "min" -> String.format("min(%s)", baseQuery);
            case "max" -> String.format("max(%s)", baseQuery);
            case "count" -> String.format("count(%s)", baseQuery);
            default -> baseQuery;
        };
    }

    /**
     * 시간 주기에 따른 Step 계산
     */
    private String calculateStep(String timePeriod) {
        return switch (timePeriod.toUpperCase()) {
            case "MINUTE" -> "1m";
            case "HOUR" -> "1h";
            case "DAY" -> "1d";
            case "WEEK" -> "7d";
            case "MONTH" -> "30d";
            default -> "15s";
        };
    }

    /**
     * 데이터 소스 판별
     * {
     *      혼합 = PROMETHEUS 30일 전 + POSTGRESQL 30일 후 ),
     *      PROMETHEUS = 30일 이전 데이터,
     *      POSTGRESQL = 30일 이후 데이터
     * }
     **/
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