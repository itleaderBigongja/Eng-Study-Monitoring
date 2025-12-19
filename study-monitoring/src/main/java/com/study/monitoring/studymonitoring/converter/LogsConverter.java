package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.LogSearchResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.LogStatisticsResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch 로그 응답 변환기
 *
 * 책임:
 *  1. 로그 검색 결과 변환 (LogSearchResponseDTO)
 *  2. 로그 통계 결과 변환 (LogStatisticsResponseDTO)
 *
 * 원칙:
 *  - Elasticsearch 조회 로직은 포함하지 않는다.
 *  - Elasticsearch 응답 구조 → DTO 변환만 담당한다.
 */
@Component
public class LogsConverter {

    /* ==========================================================
     * 1️⃣ 로그 검색(Search) 변환
     * ========================================================== */

    /**
     * Elasticsearch 로그 검색 응답 → LogSearchResponseDTO 변환
     *
     * @param elasticsearchData Elasticsearch 응답 데이터
     * @param from 페이지 시작
     * @param size 페이지 크기
     */
    public LogSearchResponseDTO toSearchDTO(
            Map<String, Object> elasticsearchData,
            int from,
            int size) {

        LogSearchResponseDTO dto = new LogSearchResponseDTO();

        // 총 로그 수
        Long total = elasticsearchData.get("total") != null
                ? ((Number) elasticsearchData.get("total")).longValue()
                : 0L;
        dto.setTotal(total);

        // 로그 목록 변환
        List<Map<String, Object>> logs =
                (List<Map<String, Object>>) elasticsearchData.get("logs");

        if (logs != null && !logs.isEmpty()) {
            dto.setLogs(
                    logs.stream()
                            .map(this::toLogEntry)
                            .collect(Collectors.toList())
            );
        } else {
            dto.setLogs(List.of());
        }

        dto.setFrom(from);
        dto.setSize(size);

        return dto;
    }

    /**
     * Elasticsearch 로그 문서 → LogEntry 변환
     */
    private LogSearchResponseDTO.LogEntry toLogEntry(Map<String, Object> logDoc) {
        LogSearchResponseDTO.LogEntry entry = new LogSearchResponseDTO.LogEntry();

        entry.setId((String) logDoc.get("_id"));
        entry.setIndex((String) logDoc.get("_index"));
        entry.setTimestamp((String) logDoc.get("@timestamp"));
        entry.setLogLevel((String) logDoc.get("log_level"));
        entry.setLoggerName((String) logDoc.get("logger_name"));
        entry.setMessage((String) logDoc.get("message"));
        entry.setApplication((String) logDoc.get("application"));
        entry.setStackTrace((String) logDoc.get("stack_trace"));

        return entry;
    }

    /* ==========================================================
     * 2️⃣ 로그 통계(Statistics) 변환
     * ========================================================== */

    /**
     * Elasticsearch 로그 집계 결과 → LogStatisticsResponseDTO.LogDistribution 변환
     *
     * @param esData Elasticsearch aggregation 결과
     */
    public List<LogStatisticsResponseDTO.LogDistribution> toStatisticsDistribution(
            List<Map<String, Object>> esData) {

        if (esData == null || esData.isEmpty()) {
            return List.of();
        }

        return esData.stream()
                .map(data -> new LogStatisticsResponseDTO.LogDistribution(
                        (String) data.get("timestamp"),
                        ((Number) data.get("count")).longValue()
                ))
                .collect(Collectors.toList());
    }
}
