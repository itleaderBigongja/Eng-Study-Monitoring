package com.study.monitoring.studymonitoring.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 로그 검색 요청 DTO
 *
 * 사용처:
 * - GET /api/logs/search
 */
@Data
public class LogSearchRequestDTO {

    @NotBlank(message = "인덱스 패턴은 필수입니다")
    private String index = "application-logs-*";  // 기본값

    private String keyword;      // 검색 키워드 (옵션)

    private String logLevel;     // INFO, WARN, ERROR (옵션)

    @Min(value = 0, message = "페이지 시작은 0 이상입니다")
    private Integer from = 0;    // 페이지 시작

    @Min(value = 1, message = "페이지 크기는 최소 1입니다")
    @Max(value = 100, message = "페이지 크기는 최대 100입니다")
    private Integer size = 50;   // 페이지 크기
}
