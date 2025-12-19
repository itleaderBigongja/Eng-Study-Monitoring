package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 로그 검색 응답 DTO
 *
 * 사용처:
 * - GET /api/logs/search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogSearchResponseDTO {

    private Long total;                 // 총 로그 수
    private List<LogEntry> logs;        // 로그 목록
    private Integer from;               // 페이지 시작
    private Integer size;               // 페이지 크기

    /**
     * 로그 엔트리 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private String id;              // 로그 ID
        private String index;           // 인덱스명
        private String timestamp;       // 타임스탬프
        private String logLevel;        // 로그 레벨
        private String loggerName;      // 로거명
        private String message;         // 메시지
        private String application;     // 애플리케이션
        private String stackTrace;      // 스택 트레이스 (옵션)
    }
}
