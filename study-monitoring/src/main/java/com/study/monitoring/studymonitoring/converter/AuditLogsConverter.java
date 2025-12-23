package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.AuditLogStatisticsResponseDTO;
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
 * Audit Logs 데이터 변환 클래스
 * ElasticsearchServiceImpl에서 이미 평탄화된 데이터를 받음
 */
@Slf4j
@Component
public class AuditLogsConverter {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✅ ElasticsearchServiceImpl.getAuditLogDistributionByTime()에서
     * 이미 평탄화된 데이터를 받아서 DTO로 변환
     *
     * 입력 데이터 구조:
     * {
     *   "timestamp": "2025-12-19 10:00:00",
     *   "totalEvents": 150,
     *   "successEvents": 140,
     *   "failureEvents": 10
     * }
     */
    public List<AuditLogStatisticsResponseDTO.AuditDistribution> toStatisticsDistribution(
            List<Map<String, Object>> rawData)
    {
        if (rawData == null || rawData.isEmpty()) {
            log.warn("Raw data is empty for audit logs distribution");
            return List.of();
        }

        return rawData.stream()
                .map(this::convertToAuditDistribution)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 평탄화된 데이터 → AuditDistribution 변환
     */
    private AuditLogStatisticsResponseDTO.AuditDistribution convertToAuditDistribution(
            Map<String, Object> data)
    {
        try {
            // ✅ 1. timestamp는 이미 String 형태로 제공됨
            String timestamp = getStringValue(data, "timestamp");

            // ✅ 2. totalEvents
            Long totalEvents = getLongValue(data, "totalEvents");

            // ✅ 3. successEvents
            Long successEvents = getLongValue(data, "successEvents");

            // ✅ 4. failureEvents
            Long failureEvents = getLongValue(data, "failureEvents");

            log.debug("Converted audit distribution: timestamp={}, totalEvents={}, successEvents={}, failureEvents={}",
                    timestamp, totalEvents, successEvents, failureEvents);

            return new AuditLogStatisticsResponseDTO.AuditDistribution(
                    timestamp, totalEvents, successEvents, failureEvents
            );

        } catch (Exception e) {
            log.error("Failed to convert audit log distribution data: {}", data, e);
            return new AuditLogStatisticsResponseDTO.AuditDistribution(
                    "Unknown", 0L, 0L, 0L
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
}