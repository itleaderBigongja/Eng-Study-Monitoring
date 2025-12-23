package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.SecurityLogStatisticsResponseDTO;
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
 * Security Logs 데이터 변환 클래스
 * ElasticsearchServiceImpl에서 이미 평탄화된 데이터를 받음
 */
@Slf4j
@Component
public class SecurityLogsConverter {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✅ ElasticsearchServiceImpl.getSecurityLogDistributionByTime()에서
     * 이미 평탄화된 데이터를 받아서 DTO로 변환
     *
     * 입력 데이터 구조:
     * {
     *   "timestamp": "2025-12-19 10:00:00",
     *   "attackCount": 50,
     *   "blockedCount": 45,
     *   "threatLevelBreakdown": {
     *     "low": 20,
     *     "medium": 15,
     *     "high": 10,
     *     "critical": 5
     *   }
     * }
     */
    public List<SecurityLogStatisticsResponseDTO.SecurityDistribution> convertToSecurityDistribution(
            List<Map<String, Object>> rawData)
    {
        if (rawData == null || rawData.isEmpty()) {
            log.warn("Raw data is empty for security logs distribution");
            return List.of();
        }

        return rawData.stream()
                .map(this::convertToSecurityDistribution)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 평탄화된 데이터 → SecurityDistribution 변환
     */
    private SecurityLogStatisticsResponseDTO.SecurityDistribution convertToSecurityDistribution(
            Map<String, Object> data)
    {
        try {
            // ✅ 1. timestamp는 이미 String 형태로 제공됨
            String timestamp = getStringValue(data, "timestamp");

            // ✅ 2. attackCount
            Long attackCount = getLongValue(data, "attackCount");

            // ✅ 3. blockedCount
            Long blockedCount = getLongValue(data, "blockedCount");

            // ✅ 4. threatLevelBreakdown
            Map<String, Long> threatLevelBreakdown = getMapValue(data, "threatLevelBreakdown");

            log.debug("Converted security distribution: timestamp={}, attackCount={}, blockedCount={}, threatLevelBreakdown={}",
                    timestamp, attackCount, blockedCount, threatLevelBreakdown);

            return new SecurityLogStatisticsResponseDTO.SecurityDistribution(
                    timestamp, attackCount, blockedCount, threatLevelBreakdown
            );

        } catch (Exception e) {
            log.error("Failed to convert security log distribution data: {}", data, e);
            return new SecurityLogStatisticsResponseDTO.SecurityDistribution(
                    "Unknown", 0L, 0L, Map.of()
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