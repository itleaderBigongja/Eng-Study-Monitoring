package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogSearchResponseDTO {

    private Long total;                 // 총 로그 수
    private List<LogEntry> logs;        // 로그 목록
    private Integer from;               // 페이지 시작
    private Integer size;               // 페이지 크기

    // ✅ [추가] 페이징 정보
    private Integer currentPage;        // 현재 페이지 (0부터 시작)
    private Integer totalPages;         // 전체 페이지 수
    private Boolean hasNext;            // 다음 페이지 존재 여부
    private Boolean hasPrevious;        // 이전 페이지 존재 여부

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

    /**
     * 페이징 정보 계산 헬퍼 메서드
     */
    public static LogSearchResponseDTO createWithPaging(
            Long total,
            List<LogEntry> logs,
            Integer from,
            Integer size) {

        LogSearchResponseDTO dto = new LogSearchResponseDTO();
        dto.setTotal(total);
        dto.setLogs(logs);
        dto.setFrom(from);
        dto.setSize(size);

        // 현재 페이지 계산 (0부터 시작)
        int currentPage = from / size;
        dto.setCurrentPage(currentPage);

        // 전체 페이지 수 계산
        int totalPages = (int) Math.ceil((double) total / size);
        dto.setTotalPages(totalPages);

        // 다음/이전 페이지 여부
        dto.setHasNext(currentPage < totalPages - 1);
        dto.setHasPrevious(currentPage > 0);

        return dto;
    }
}