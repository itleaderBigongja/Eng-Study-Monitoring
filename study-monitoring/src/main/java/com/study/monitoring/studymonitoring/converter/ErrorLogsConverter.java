package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.ErrorLogStatisticsResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Error Logs 데이터 변환 클래스
 * ElasticsearchServiceImpl에서 이미 평탄화된 데이터를 받음
 */
@Slf4j
@Component
public class ErrorLogsConverter {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✅ ElasticsearchServiceImpl.getErrorLogDistributionByTime()에서
     * 이미 평탄화된 데이터를 받아서 DTO로 변환
     *
     * 입력 데이터 구조:
     * {
     *   "timestamp": "2025-12-19 10:00:00",
     *   "errorCount": 25,
     *   "errorTypeBreakdown": {
     *     "NullPointerException": 10,
     *     "SQLException": 8,
     *     "IOException": 7
     *   }
     * }
     */
    public List<ErrorLogStatisticsResponseDTO.ErrorDistribution> toStatisticsDistribution(
            List<Map<String, Object>> rawData)
    {
        if (rawData == null || rawData.isEmpty()) {
            log.warn("Raw data is empty for error logs distribution");
            return List.of();
        }

        return rawData.stream()
                .map(this::convertToErrorDistribution)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 평탄화된 데이터 → ErrorDistribution 변환
     */
    private ErrorLogStatisticsResponseDTO.ErrorDistribution convertToErrorDistribution(
            Map<String, Object> data)
    {
        try {
            // ✅ 1. timestamp는 이미 String 형태로 제공됨
            String timestamp = getStringValue(data, "timestamp");

            // ✅ 2. errorCount
            Long errorCount = getLongValue(data, "errorCount");

            // ✅ 3. errorTypeBreakdown
            Map<String, Long> errorTypeBreakdown = getMapValue(data, "errorTypeBreakdown");

            log.debug("Converted error distribution: timestamp={}, errorCount={}, errorTypeBreakdown={}",
                    timestamp, errorCount, errorTypeBreakdown);

            return new ErrorLogStatisticsResponseDTO.ErrorDistribution(
                    timestamp, errorCount, errorTypeBreakdown
            );

        } catch (Exception e) {
            log.error("Failed to convert error log distribution data: {}", data, e);
            return new ErrorLogStatisticsResponseDTO.ErrorDistribution(
                    "Unknown", 0L, Map.of()
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
     * ✅ Map<String, Long> 값 안전하게 추출
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> getMapValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            log.warn("Key '{}' not found, returning empty map", key);
            return Map.of();
        }

        if (value instanceof Map) {
            try {
                Map<String, Object> rawMap = (Map<String, Object>) value;
                Map<String, Long> resultMap = new HashMap<>();

                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    Object entryValue = entry.getValue();
                    if (entryValue instanceof Number) {
                        resultMap.put(entry.getKey(), ((Number) entryValue).longValue());
                    }
                }

                return resultMap;
            } catch (Exception e) {
                log.warn("Failed to convert map value for key '{}': {}", key, e.getMessage());
                return Map.of();
            }
        }

        return Map.of();
    }
}