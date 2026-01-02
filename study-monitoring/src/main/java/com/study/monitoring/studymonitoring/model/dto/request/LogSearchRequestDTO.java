package com.study.monitoring.studymonitoring.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 로그 검색 요청 DTO
 */
@Data
public class LogSearchRequestDTO {

    @NotBlank(message = "인덱스 패턴은 필수입니다")
    private String index = "application-logs-*";  // 기본값

    private String keyword;      // 검색 키워드 (옵션)

    private String logLevel;     // INFO, WARN, ERROR (옵션)

    // ✅ [추가] 날짜 필터링
    private String startDate;    // 시작 날짜 (옵션: yyyy-MM-ddTHH:mm:ss)

    private String endDate;      // 종료 날짜 (옵션: yyyy-MM-ddTHH:mm:ss)

    @Min(value = 0, message = "페이지 시작은 0 이상입니다")
    private Integer from = 0;    // 페이지 시작

    @Min(value = 1, message = "페이지 크기는 최소 1입니다")
    @Max(value = 100, message = "페이지 크기는 최대 100입니다")
    private Integer size = 10;   // 페이지 크기

    /**
     * 사용 가능한 인덱스 패턴 목록
     * - application-logs-*: 애플리케이션 로그
     * - access-logs-*: HTTP 접근 로그
     * - error-logs-*: 에러 로그
     * - performance-metrics-*: 성능 메트릭
     * - database-logs-*: DB 로그
     * - audit-logs-*: 감사 로그
     * - security-logs-*: 보안 로그
     */
}