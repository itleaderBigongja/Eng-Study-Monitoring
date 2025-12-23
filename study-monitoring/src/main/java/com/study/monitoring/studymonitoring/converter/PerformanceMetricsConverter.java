package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.PerformanceMetricsStatisticsResponseDTO;
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
 * Performance Metrics 데이터 변환 클래스
 * ElasticsearchServiceImpl에서 이미 평탄화된 데이터를 받음
 */
@Slf4j
@Component
public class PerformanceMetricsConverter {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✅ ElasticsearchServiceImpl.getPerformanceMetricsDistributionByTime()에서
     * 이미 평탄화된 데이터를 받아서 DTO로 변환
     *
     * 입력 데이터 구조:
     * {
     *   "timestamp": "2025-12-19 10:00:00",
     *   "cpuUsage": 45.5,
     *   "memoryUsage": 68.2,
     *   "heapUsage": 1024.0
     * }
     */
    public List<PerformanceMetricsStatisticsResponseDTO.MetricDistribution> toStatisticsDistribution(
            List<Map<String, Object>> rawData)
    {
        if (rawData == null || rawData.isEmpty()) {
            log.warn("Raw data is empty for performance metrics distribution");
            return List.of();
        }

        return rawData.stream()
                .map(this::convertToMetricsDistribution)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 평탄화된 데이터 → MetricDistribution 변환
     */
    private PerformanceMetricsStatisticsResponseDTO.MetricDistribution convertToMetricsDistribution(
            Map<String, Object> data)
    {
        try {
            // ✅ 1. timestamp는 이미 String 형태로 제공됨
            String timestamp = getStringValue(data, "timestamp");

            // ✅ 2. cpuUsage
            Double cpuUsage = getDoubleValue(data, "cpuUsage");

            // ✅ 3. memoryUsage
            Double memoryUsage = getDoubleValue(data, "memoryUsage");

            // ✅ 4. heapUsage
            Double heapUsage = getDoubleValue(data, "heapUsage");

            log.debug("Converted performance metrics: timestamp={}, cpuUsage={}, memoryUsage={}, heapUsage={}",
                    timestamp, cpuUsage, memoryUsage, heapUsage);

            return new PerformanceMetricsStatisticsResponseDTO.MetricDistribution(
                    timestamp, cpuUsage, memoryUsage, heapUsage
            );

        } catch (Exception e) {
            log.error("Failed to convert performance metrics data: {}", data, e);
            return new PerformanceMetricsStatisticsResponseDTO.MetricDistribution(
                    "Unknown", 0.0, 0.0, 0.0
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