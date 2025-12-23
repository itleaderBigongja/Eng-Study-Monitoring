package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.AccessLogStatisticsResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Access Logs 데이터 변환 클래스
 * ElasticsearchServiceImpl에서 이미 평탄화된 데이터를 받음
 */
@Slf4j
@Component
public class AccessLogsConverter {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✅ ElasticsearchServiceImpl.getAccessLogDistributionByTime()에서
     * 이미 평탄화된 데이터를 받아서 DTO로 변환
     *
     * 입력 데이터 구조:
     * {
     *   "timestamp": "2025-12-19 10:00:00",
     *   "requestCount": 3,
     *   "avgResponseTime": 1721.666,
     *   "errorCount": 1
     * }
     */
    public List<AccessLogStatisticsResponseDTO.AccessDistribution> toStatisticsDistribution(
            List<Map<String, Object>> rawData) {

        if (rawData == null || rawData.isEmpty()) {
            log.warn("Raw data is empty for access logs distribution");
            return List.of();
        }

        return rawData.stream()
                .map(this::convertToAccessDistribution)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 평탄화된 데이터 → AccessDistribution 변환
     */
    private AccessLogStatisticsResponseDTO.AccessDistribution convertToAccessDistribution(
            Map<String, Object> data) {

        try {
            // ✅ 1. timestamp는 이미 String 형태로 제공됨
            String timestamp = getStringValue(data, "timestamp");

            // ✅ 2. requestCount
            Long requestCount = getLongValue(data, "requestCount");

            // ✅ 3. avgResponseTime
            Double avgResponseTime = getDoubleValue(data, "avgResponseTime");

            // ✅ 4. errorCount
            Long errorCount = getLongValue(data, "errorCount");

            log.debug("Converted access distribution: timestamp={}, requestCount={}, avgResponseTime={}, errorCount={}",
                    timestamp, requestCount, avgResponseTime, errorCount);

            return new AccessLogStatisticsResponseDTO.AccessDistribution(
                    timestamp, requestCount, avgResponseTime, errorCount
            );

        } catch (Exception e) {
            log.error("Failed to convert access log distribution data: {}", data, e);
            return new AccessLogStatisticsResponseDTO.AccessDistribution(
                    "Unknown", 0L, 0.0, 0L
            );
        }
    }

    /**
     * ✅ String 값 안전하게 추출
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            log.warn("Key '{}' not found in data: {}", key, data.keySet());
            return "Unknown";
        }
        return value.toString();
    }

    /**
     * ✅ Long 값 안전하게 추출
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            log.warn("Key '{}' not found, returning 0", key);
            return 0L;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse '{}' as Long: {}", key, value);
            return 0L;
        }
    }

    /**
     * ✅ Double 값 안전하게 추출
     */
    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            log.warn("Key '{}' not found, returning 0.0", key);
            return 0.0;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse '{}' as Double: {}", key, value);
            return 0.0;
        }
    }
}