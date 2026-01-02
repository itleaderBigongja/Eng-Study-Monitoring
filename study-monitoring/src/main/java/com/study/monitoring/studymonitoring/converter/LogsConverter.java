package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.LogSearchResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.LogStatisticsResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LogsConverter {

    /**
     * Elasticsearch 응답을 LogSearchResponseDTO로 변환 (페이징 정보 포함)
     */
    public LogSearchResponseDTO toSearchDTO(Map<String, Object> elasticsearchData, int from, int size) {
        Long total = elasticsearchData.get("total") != null ? ((Number) elasticsearchData.get("total")).longValue() : 0L;
        List<Map<String, Object>> logs = (List<Map<String, Object>>) elasticsearchData.get("logs");

        List<LogSearchResponseDTO.LogEntry> logEntries;
        if (logs != null && !logs.isEmpty()) {
            logEntries = logs.stream().map(this::toLogEntry).collect(Collectors.toList());
        } else {
            logEntries = List.of();
        }

        // 페이징 정보 포함 DTO 생성
        return LogSearchResponseDTO.createWithPaging(total, logEntries, from, size);
    }

    /**
     * Elasticsearch Hit를 LogEntry로 변환
     */
    private LogSearchResponseDTO.LogEntry toLogEntry(Map<String, Object> logDoc) {
        LogSearchResponseDTO.LogEntry entry = new LogSearchResponseDTO.LogEntry();
        entry.setId((String) logDoc.get("_id"));
        entry.setIndex((String) logDoc.get("_index"));
        entry.setTimestamp((String) logDoc.get("@timestamp"));

        // ----------------------------------------------------
        // ✅ [핵심] 인덱스별 필드를 분석하여 통합 LogLevel 도출
        // ----------------------------------------------------
        entry.setLogLevel(determineLogLevel(logDoc));

        entry.setLoggerName(getOrDefault(logDoc, "logger_name", "root"));
        entry.setMessage(determineMessage(logDoc)); // 메시지도 필드명이 다를 수 있음
        entry.setApplication((String) logDoc.get("application"));

        // StackTrace 처리 (error-logs에만 있음)
        if (logDoc.containsKey("error") && logDoc.get("error") instanceof Map) {
            Map<String, Object> errorMap = (Map<String, Object>) logDoc.get("error");
            entry.setStackTrace((String) errorMap.get("stack_trace"));
        } else {
            entry.setStackTrace((String) logDoc.get("stack_trace"));
        }

        return entry;
    }

    /**
     * 다양한 인덱스 필드에서 '로그 레벨'을 추출하는 로직
     */
    private String determineLogLevel(Map<String, Object> doc) {
        String index = (String) doc.get("_index");

        // 1. 인덱스 종류별 우선 처리 (정확도 향상)
        if (index != null) {
            // ✅ Access Logs: 무조건 status_code로 판단
            if (index.startsWith("access-logs")) {
                if (doc.containsKey("http")) {
                    Map<String, Object> http = (Map<String, Object>) doc.get("http");
                    if (http != null && http.containsKey("status_code")) {
                        // Integer, Long 모두 안전하게 Number로 처리
                        int status = ((Number) http.get("status_code")).intValue();

                        if (status >= 500) return "ERROR"; // 500번대는 빨간색
                        if (status >= 400) return "WARN";  // 400번대는 노란색
                        return "INFO";                     // 나머지는 파란색
                    }
                }
                return "INFO"; // status_code가 없으면 기본값
            }

            // ✅ Security Logs: threat_level로 판단
            if (index.startsWith("security-logs")) {
                if (doc.containsKey("security")) {
                    Map<String, Object> sec = (Map<String, Object>) doc.get("security");
                    if (sec != null && sec.containsKey("threat_level")) {
                        String threat = String.valueOf(sec.get("threat_level")).toUpperCase();

                        if ("HIGH".equals(threat) || "CRITICAL".equals(threat)) return "ERROR";
                        if ("MEDIUM".equals(threat)) return "WARN";
                        return "INFO"; // low 등
                    }
                }
                return "INFO";
            }

            // ✅ Audit Logs: result로 판단
            if (index.startsWith("audit-logs")) {
                if (doc.containsKey("event")) {
                    Map<String, Object> event = (Map<String, Object>) doc.get("event");
                    if (event != null && event.containsKey("result")) {
                        String result = String.valueOf(event.get("result"));
                        return "failure".equalsIgnoreCase(result) ? "ERROR" : "INFO";
                    }
                }
                return "INFO";
            }

            // ✅ Error Logs: severity로 판단
            if (index.startsWith("error-logs")) {
                if (doc.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) doc.get("error");
                    if (error != null && error.containsKey("severity")) {
                        return String.valueOf(error.get("severity")); // ERROR, FATAL 등 그대로 반환
                    }
                }
                return "ERROR"; // error-logs인데 severity가 없으면 기본 ERROR로 취급
            }
        }

        // 2. Application Logs 및 기타 (기존 log_level 필드 사용)
        if (doc.containsKey("log_level")) {
            return String.valueOf(doc.get("log_level"));
        }

        // 3. 최후의 보루
        return "INFO";
    }

    /**
     * 메시지 필드도 인덱스마다 다르므로 통합
     */
    private String determineMessage(Map<String, Object> doc) {
        // application-logs
        if (doc.containsKey("message")) return (String) doc.get("message");

        // error-logs
        if (doc.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) doc.get("error");
            return (String) error.get("message");
        }

        // database-logs
        if (doc.containsKey("query")) {
            Map<String, Object> query = (Map<String, Object>) doc.get("query");
            return (String) query.get("sql");
        }

        // access-logs (메시지가 없으므로 URL Method 등으로 조합)
        if (doc.containsKey("http")) {
            Map<String, Object> http = (Map<String, Object>) doc.get("http");
            return http.get("method") + " " + http.get("url") + " (" + http.get("status_code") + ")";
        }

        // audit-logs
        if (doc.containsKey("event")) {
            Map<String, Object> event = (Map<String, Object>) doc.get("event");
            return event.get("action") + " - " + event.get("result");
        }

        return "";
    }

    private String getOrDefault(Map<String, Object> map, String key, String defaultValue) {
        return map.containsKey(key) && map.get(key) != null ? (String) map.get(key) : defaultValue;
    }

    /**
     * 통계 분포 변환
     */
    public List<LogStatisticsResponseDTO.LogDistribution> toStatisticsDistribution(
            List<Map<String, Object>> esData) {

        if (esData == null || esData.isEmpty()) {
            return List.of();
        }

        return esData.stream()
                .map(data -> new LogStatisticsResponseDTO.LogDistribution(
                        (String) data.get("timestamp"),
                        ((Number) data.get("count")).longValue()))
                .collect(Collectors.toList());
    }
}