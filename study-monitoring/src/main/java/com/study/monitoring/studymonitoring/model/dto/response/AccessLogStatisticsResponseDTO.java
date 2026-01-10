package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 접근 로그 통계 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogStatisticsResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;
    private String timePeriod;

    // HTTP 메서드 별 카운트( GET: 1000, POST: 500 )
    private Map<String, Long> methodCounts;

    // 상태코드 별 카운트( 200: 8000, 404: 100, 500:50 )
    private Map<String, Long> statusCodeCounts;

    // 평균 응답시간( ms )
    private Double avgResponseTime;

    // 에러율 관련 필드
    private Long totalRequests;
    private Long errorCount;
    private Double errorRate; // 에러율 (%)

    // 시간대 별 분포
    private List<AccessDistribution> distributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessDistribution {
        private String timestamp;               // 시간일자
        private Long requestCount;              // 요청 수
        private Double avgResponseTime;         // 평균 응답시간
        private Long errorCount;                // 에러 수
    }
}
