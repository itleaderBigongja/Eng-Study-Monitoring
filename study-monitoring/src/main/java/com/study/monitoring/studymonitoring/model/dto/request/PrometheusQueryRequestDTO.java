package com.study.monitoring.studymonitoring.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * PromQL 쿼리 요청 DTO
 *
 * 사용처:
 * - POST /api/metrics/query
 * - POST /api/metrics/range
 */
@Data
public class PrometheusQueryRequestDTO {

    @NotBlank(message = "쿼리는 필수입니다")
    private String query;        // PromQL 쿼리 문자열

    private Long start;          // Unix timestamp (옵션)
    private Long end;            // Unix timestamp (옵션)

    private String step = "15s"; // 기본: 15초 간격
}
