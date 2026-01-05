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

        entry.setLogLevel(determineLogLevel(logDoc));

        String loggerName = (String) logDoc.get("logger_name");
        if (loggerName == null || loggerName.isEmpty()) {
            loggerName = (String) logDoc.get("logger");
        }
        entry.setLoggerName(loggerName != null ? loggerName : "root");

        entry.setMessage(determineMessage(logDoc));
        entry.setApplication((String) logDoc.get("application"));

        // StackTrace 처리
        if (logDoc.containsKey("error") && logDoc.get("error") instanceof Map) {
            Map<String, Object> errorMap = (Map<String, Object>) logDoc.get("error");
            entry.setStackTrace((String) errorMap.get("stack_trace"));
        } else {
            entry.setStackTrace((String) logDoc.get("stack_trace"));
        }

        return entry;
    }

    /**
     * 로그 레벨 결정 (인덱스별 로직 분기)
     */
    private String determineLogLevel(Map<String, Object> doc) {
        String index = (String) doc.get("_index");

        if (index != null) {
            if (index.startsWith("access-logs")) {
                int status = 0;
                if (doc.containsKey("http")) {
                    Map<String, Object> http = (Map<String, Object>) doc.get("http");
                    if (http != null && http.containsKey("status_code")) {
                        status = ((Number) http.get("status_code")).intValue();
                    }
                } else if (doc.containsKey("status_code")) {
                    status = ((Number) doc.get("status_code")).intValue();
                }

                if (status > 0) {
                    if (status >= 500) return "ERROR";
                    if (status >= 400) return "WARN";
                    return "INFO";
                }
                return "INFO";
            }

            if (index.startsWith("security-logs")) {
                if (doc.containsKey("security")) {
                    Map<String, Object> sec = (Map<String, Object>) doc.get("security");
                    if (sec != null && sec.containsKey("threat_level")) {
                        String threat = String.valueOf(sec.get("threat_level")).toUpperCase();
                        if ("HIGH".equals(threat) || "CRITICAL".equals(threat)) return "ERROR";
                        if ("MEDIUM".equals(threat)) return "WARN";
                        return "INFO";
                    }
                }
                return "INFO";
            }

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

            if (index.startsWith("error-logs")) {
                if (doc.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) doc.get("error");
                    if (error != null && error.containsKey("severity")) {
                        return String.valueOf(error.get("severity"));
                    }
                }
                return "ERROR";
            }

            // ✅ database-logs 로그 레벨 결정 (rawMessage 선언 후 사용)
            if (index.startsWith("database-logs")) {
                // 1. 에러 필드가 있거나 스택트레이스가 있으면 ERROR
                if ((doc.containsKey("error") && doc.get("error") instanceof Map) ||
                        (doc.containsKey("stack_trace") && doc.get("stack_trace") != null)) {
                    return "ERROR";
                }

                String rawMessage = (String) doc.get("message");

                // 2. Interceptor가 남긴 정상 SQL 로그("SQL: [")는 INFO로 표시
                if (rawMessage != null && rawMessage.startsWith("SQL: [")) {
                    return "INFO";
                }

                // 3. 그 외 원본 로그 레벨 확인
                if (doc.containsKey("log_level")) {
                    String level = String.valueOf(doc.get("log_level"));
                    if (level != null && !level.isEmpty() && !"null".equals(level)) {
                        return level.toUpperCase();
                    }
                }

                return "DEBUG";
            }

            if (index.startsWith("performance-metrics")) {
                if (doc.containsKey("log_level")) {
                    return String.valueOf(doc.get("log_level"));
                }
                return "INFO";
            }
        }

        if (doc.containsKey("log_level")) return String.valueOf(doc.get("log_level"));
        if (doc.containsKey("level")) return String.valueOf(doc.get("level"));

        return "INFO";
    }

    /**
     * 로그 메시지 파싱 및 포맷팅
     */
    private String determineMessage(Map<String, Object> doc) {
        String index = (String) doc.getOrDefault("_index", "");
        String rawMessage = (String) doc.get("message");

        // 1. Performance Metrics
        if (index.startsWith("performance-metrics")) {
            if (doc.containsKey("method") && doc.containsKey("execution_time_ms")) {
                String className = (String) doc.getOrDefault("class", "");
                String simpleClassName = className.contains(".")
                        ? className.substring(className.lastIndexOf(".") + 1)
                        : className;
                return String.format("[Perf] %s.%s() took %sms",
                        simpleClassName, doc.get("method"), doc.get("execution_time_ms"));
            }
            if (rawMessage != null && !rawMessage.isEmpty()) return "[Metrics] " + rawMessage;
            return "[Metrics] Performance Data";
        }

        // 2. Access Logs
        if (index.startsWith("access-logs")) {
            if (doc.containsKey("http")) {
                Map<String, Object> http = (Map<String, Object>) doc.get("http");
                String method = String.valueOf(http.getOrDefault("method", "REQ"));
                String url = String.valueOf(http.getOrDefault("url", "-"));
                String status = String.valueOf(http.getOrDefault("status_code", "0"));
                String duration = String.valueOf(http.getOrDefault("response_time_ms", "0"));
                return String.format("[%s] %s -> %s (%sms)", method, url, status, duration);
            }
        }

        // 3. Security Logs
        if (index.startsWith("security-logs")) {
            if (doc.containsKey("security")) {
                Map<String, Object> sec = (Map<String, Object>) doc.get("security");
                return String.format("[Security] %s (%s)",
                        sec.getOrDefault("event_type", "Event"), sec.getOrDefault("threat_level", "Info"));
            }
            if (rawMessage != null && !rawMessage.isEmpty()) {
                return "[Security] " + (rawMessage.length() > 100 ? rawMessage.substring(0, 100) + "..." : rawMessage);
            }
            return "[Security] No message";
        }

        // ✅ 4. Database Logs (예외 처리 개선)
        if (index.startsWith("database-logs")) {

            // [Case 1] 예외 발생한 경우 - SQL 정보 포함해서 표시
            if (doc.containsKey("stack_trace") && doc.get("stack_trace") != null) {
                String stackTrace = String.valueOf(doc.get("stack_trace"));

                // rawMessage에 SQL 정보가 포함되어 있으면 함께 표시
                if (rawMessage != null && rawMessage.contains("SQL Execution Failed")) {
                    try {
                        int sqlIndex = rawMessage.indexOf("SQL: [");
                        if (sqlIndex > 0) {
                            String sqlPart = rawMessage.substring(sqlIndex);
                            // 너무 길면 일부만 표시
                            if (sqlPart.length() > 200) {
                                sqlPart = sqlPart.substring(0, 200) + "...";
                            }
                            return "[DB Error] " + sqlPart;
                        }
                    } catch (Exception e) {
                        // 파싱 실패 시 원본 표시
                    }
                    return "[DB Error] " + rawMessage;
                }

                // 스택트레이스에서 SQL 추출 시도
                if (stackTrace.contains("### SQL:")) {
                    try {
                        int sqlStart = stackTrace.indexOf("### SQL:");
                        int sqlEnd = stackTrace.indexOf("###", sqlStart + 8);
                        if (sqlEnd > sqlStart) {
                            String sql = stackTrace.substring(sqlStart + 8, sqlEnd).trim();
                            return "[DB Error] " + sql.replaceAll("\\s+", " ");
                        }
                    } catch (Exception e) {
                        // 파싱 실패
                    }
                }

                // SQL 정보 없으면 에러 메시지만 표시
                if (rawMessage != null && !rawMessage.isEmpty()) {
                    return "[DB Error] " + rawMessage;
                }
                return "[DB Error] SQL execution failed";
            }

            // [Case 2] Interceptor가 남긴 정상 SQL 로그 파싱
            if (rawMessage != null && rawMessage.startsWith("SQL: [")) {
                try {
                    int paramsIndex = rawMessage.indexOf("| Params:");
                    if (paramsIndex > 0) {
                        // "SQL: [" (길이 6) 부터 Params 전까지 자르기
                        String sqlPart = rawMessage.substring(0, paramsIndex).trim();

                        // 끝에 있는 "]" 제거 (길이가 충분한지 확인)
                        if (sqlPart.length() > 7 && sqlPart.endsWith("]")) {
                            String cleanSql = sqlPart.substring(6, sqlPart.length() - 1);
                            return "[DB] " + cleanSql;
                        }
                    }
                } catch (Exception e) {
                    // 파싱 실패 시 원본 리턴하도록 무시
                }
                return "[DB] " + rawMessage;
            }

            // [Case 3] 기타 MyBatis 로그
            if (rawMessage != null) {
                String cleanMsg = rawMessage.trim();
                if (cleanMsg.contains("Preparing:")) {
                    return "[DB] " + cleanMsg.replace("==>  Preparing:", "").trim();
                }
                if (cleanMsg.contains("Parameters:")) {
                    return "[DB Params] " + cleanMsg.replace("==> Parameters:", "").trim();
                }
                if (cleanMsg.contains("Executing SQL")) {
                    return "[DB] " + cleanMsg.replace("Executing SQL query", "Query").trim();
                }
                return "[DB] " + cleanMsg;
            }

            // 🔴 [디버깅용 수정] 마지막 return 문을 이렇게 바꿔보세요.
            // 화면에 "[DB] NULL MESSAGE" 라고 뜨면 데이터가 안 넘어오는 것이고,
            // "[DB] RAW: ..." 라고 뜨면 형식이 안 맞는 것입니다.
            if (rawMessage == null) {
                return "[DB] NULL MESSAGE (Check Service Layer)";
            }
            return "[DB] RAW: " + rawMessage;
        }

        // 5. Audit Logs
        if (index.startsWith("audit-logs") && doc.containsKey("event")) {
            Map<String, Object> event = (Map<String, Object>) doc.get("event");
            String action = String.valueOf(event.getOrDefault("action", "Action"));
            String result = String.valueOf(event.getOrDefault("result", "Result"));
            if (rawMessage != null) return String.format("[Audit] %s (%s) - %s", action, result, rawMessage);
            return String.format("[Audit] %s - %s", action, result);
        }

        // 6. Generic Error handling
        if (doc.containsKey("error")) {
            Object errorObj = doc.get("error");
            if (errorObj instanceof Map) {
                Map<String, Object> error = (Map<String, Object>) errorObj;
                String errorMsg = (String) error.get("message");
                if (errorMsg != null) return errorMsg;
            }
        }

        if (rawMessage != null) return rawMessage;
        return "내용 없음";
    }

    public List<LogStatisticsResponseDTO.LogDistribution> toStatisticsDistribution(List<Map<String, Object>> esData) {
        if (esData == null || esData.isEmpty()) return List.of();
        return esData.stream()
                .map(data -> new LogStatisticsResponseDTO.LogDistribution(
                        (String) data.get("timestamp"), ((Number) data.get("count")).longValue()))
                .collect(Collectors.toList());
    }
}